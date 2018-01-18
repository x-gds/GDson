package com.gdson.processor;

import com.gdson.GDson;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class TypeAdapterWriter {
    private static final ClassName CLASS_NAME_TYPE_TOKEN = ClassName.get(com.google.gson.reflect.TypeToken.class);
    private final Map<TypeName, FieldSpec> registry = new HashMap<>();

    private final ParameterizedTypeName typeAdapter;

    private ClassName modelType;

    private final List<FieldDef> fields;

    public TypeAdapterWriter(ProcessingEnvironment environment, TypeElement element) {
        modelType = ClassName.get(element);
        typeAdapter = Utils.getTypeAdapterName(modelType);

        GDson annotation = element.getAnnotation(GDson.class);

        fields = new ArrayList<>();
        if (!element.getSuperclass().toString().equals(Object.class.toString())) {
            fields.addAll(extractFields(annotation,
                    environment.getElementUtils().getTypeElement(element.getSuperclass().toString())));
        }

        fields.addAll(extractFields(annotation, element));
    }

    private static List<FieldDef> extractFields(
            GDson config,
            TypeElement typeElement) {
        ArrayList<FieldDef> result = new ArrayList<>();
        List<? extends Element> elements = typeElement.getEnclosedElements();
        elements.stream().filter(obj -> obj instanceof VariableElement).forEach(obj -> {
            VariableElement element = (VariableElement) obj;
            if (!element.getModifiers().contains(Modifier.TRANSIENT)
                    && !element.getModifiers().contains(Modifier.FINAL)
                    && !element.getModifiers().contains(Modifier.PRIVATE)
                    && !element.getModifiers().contains(Modifier.STATIC)) {
                result.add(new FieldDef(config, element));
            }
        });
        return result;
    }

    private Set<TypeName> getComplexTypes() {
        return fields.stream().filter(fieldDef -> !fieldDef.isSimpleType()).map(FieldDef::getFieldType).collect(Collectors.toSet());
    }

    private TypeSpec buildTypeSpec() {
        TypeSpec.Builder typeAdapterClass = TypeSpec.classBuilder(Utils.createTypeAdapterClassName(modelType));
        typeAdapterClass.addAnnotation(Utils.suppressWarnings("unused"));
        typeAdapterClass.addModifiers(Modifier.PUBLIC);
        typeAdapterClass.superclass(typeAdapter);

        for (TypeName type : getComplexTypes()) {
            FieldSpec fieldSpec = Utils.getField(type);
            registry.put(type, fieldSpec);
            typeAdapterClass.addField(fieldSpec);
        }

        typeAdapterClass.addMethod(MethodSpec.constructorBuilder()
                .addAnnotation(Utils.suppressWarnings("unchecked"))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Gson.class, "gson")
                .addCode(getFieldInitialization())
                .build());

        typeAdapterClass.addMethod(buildWriteMethod());
        typeAdapterClass.addMethod(buildReadMethod());

        return typeAdapterClass.build();
    }

    private CodeBlock getInitTypeAdapterCode(TypeName type) {
        return CodeBlock.of("gson.getAdapter($T.get($L))", CLASS_NAME_TYPE_TOKEN, Utils.getTypeCodeBlock(type));
    }

    private CodeBlock getFieldInitialization() {
        CodeBlock.Builder block = CodeBlock.builder();
        Set<TypeName> keys = registry.keySet();
        for (TypeName typeName : keys) {
            FieldSpec field = registry.get(typeName);
            if (typeName instanceof ParameterizedTypeName) {
                block.addStatement("$N = ($T) $L",
                        field, Utils.getTypeAdapterName(typeName), getInitTypeAdapterCode(typeName));
            } else {
                block.addStatement("$N = $L",
                        field, getInitTypeAdapterCode(typeName));
            }
        }
        return block.build();
    }

    private MethodSpec buildWriteMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addParameter(JsonWriter.class, "writer")
                .addParameter(modelType, "value");

        method.beginControlFlow("if (value == null)");
        method.addStatement("writer.nullValue()");
        method.addStatement("return");
        method.endControlFlow();
        method.addStatement("writer.beginObject()");
        for (FieldDef field : fields) {
            method.addCode(field.buildWriteBlock("value", "writer"));
        }
        method.addStatement("writer.endObject()");

        return method.build();
    }

    private MethodSpec buildReadMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("read")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(modelType)
                .addException(IOException.class)
                .addParameter(JsonReader.class, "reader");
        method.beginControlFlow("if (reader.peek() == $T.NULL)", ClassName.get(JsonToken.class));
        method.addStatement("reader.nextNull()");
        method.addStatement("return null");
        method.endControlFlow();
        method.addStatement("$T object = new $T()", modelType, modelType);

        method.addStatement("reader.beginObject()");
        method.beginControlFlow("while (reader.hasNext())");
        method.beginControlFlow("switch (reader.nextName())");
        for (FieldDef field : fields) {
            for (String name : field.getSerializedNameCandidates()) {
                method.addCode("case $S:\n", name);
            }
            method.addCode(field.buildReadCodeBlock("object", "reader"));
            method.addStatement("break");
        }
        method.addCode("default:\n");
        method.addStatement("reader.skipValue()");
        method.addStatement("break");
        method.endControlFlow();
        method.endControlFlow();
        method.addStatement("reader.endObject()");

        method.addStatement("return object");

        return method.build();
    }

    private JavaFile buildJavaFile() {
        return JavaFile.builder(modelType.packageName(), buildTypeSpec())
                .skipJavaLangImports(true)
                .build();
    }

    public void write(Filer filer) {
        try {
            buildJavaFile().writeTo(filer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
