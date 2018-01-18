package com.gdson.processor;

import com.google.gson.Gson;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class TypeAdapterFactoryWriter {
    private Set<TypeElement> typeElementSet = new HashSet<>();
    private String packageName;
    private String typeAdapterFactoryName;

    public TypeAdapterFactoryWriter(ProcessingEnvironment processingEnv, Set<? extends Element> elements, Set<? extends Element> moduleElements) {
        for (Object element : elements) {
            if (element instanceof TypeElement) {
                typeElementSet.add((TypeElement) element);
            }
        }
        if (moduleElements.isEmpty()) {
            packageName = "com.gdson";
            typeAdapterFactoryName = "GDsonTypeAdapterFactory";
        } else {
            for (Element moduleElement : moduleElements) {
                if (moduleElement instanceof TypeElement) {
                    ClassName className = ClassName.get((TypeElement) moduleElement);
                    packageName = className.packageName();
                    typeAdapterFactoryName = Utils.createTypeAdapterClassName(className) + "Factory";
                }
            }
        }
    }

    public void write(Filer synchronizedFiler) {
        try {
            buildJavaFile().writeTo(synchronizedFiler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JavaFile buildJavaFile() {
        return JavaFile.builder(packageName, buildTypeSpec())
//                .skipJavaLangImports(true)
                .build();
    }

    private TypeSpec buildTypeSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(typeAdapterFactoryName)
                .addAnnotation(Utils.suppressWarnings("unused"))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(TypeAdapterFactory.class);

        builder.addMethod(buildTypeNameMethod());
        builder.addMethod(buildCreateMethod());

        return builder.build();
    }

    private MethodSpec buildTypeNameMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getTypeAdapterName");
        methodBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(String.class, "className");
        methodBuilder.addStatement("return className + $S", "TypeAdapter");
        return methodBuilder.build();
    }

    private MethodSpec buildCreateMethod() {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("create")
                .addAnnotation(Utils.suppressWarnings("unchecked"))
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Utils.CLASS_NAME_TYPE_ADAPTER, TypeVariableName.get("T")))
                .addTypeVariable(TypeVariableName.get("T"))
                .addParameter(Gson.class, "gson")
                .addParameter(ParameterizedTypeName.get(ClassName.get(TypeToken.class), TypeVariableName.get("T")), "typeToken");
        methodBuilder.addStatement("$T typeAdapter", Utils.CLASS_NAME_TYPE_ADAPTER);
        if (typeElementSet.isEmpty()) {
            methodBuilder.addStatement("typeAdapter = null");
        } else {
            methodBuilder.beginControlFlow("switch (getTypeAdapterName(typeToken.getRawType().getName()))");
            for (TypeElement element : typeElementSet) {
                methodBuilder.addCode("case $S:\n", Utils.getQualifiedTypeAdapterClassName(ClassName.get(element)));
                methodBuilder.addStatement("typeAdapter = new $N(gson)", Utils.getQualifiedTypeAdapterClassName(ClassName.get(element)));
                methodBuilder.addStatement("break");
            }
            methodBuilder.addCode("default:\n");
            methodBuilder.addStatement("typeAdapter = null");
            methodBuilder.addStatement("break");
            methodBuilder.endControlFlow();
        }
        methodBuilder.addStatement("return typeAdapter");
        return methodBuilder.build();
    }
}
