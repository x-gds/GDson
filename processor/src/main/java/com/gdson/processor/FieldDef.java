package com.gdson.processor;

import com.gdson.GDson;
import com.google.gson.annotations.SerializedName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.VariableElement;

public class FieldDef {
    private static final ClassName CLASS_NAME_STRING = ClassName.get(String.class);
    private final GDson fieldConfig;

    private final TypeName fieldType;

    private final String fieldName;

    private final String serializedName;

    private final List<String> serializedNameCandidates;

    public FieldDef(GDson fieldConfig, VariableElement element) {
        this.fieldConfig = fieldConfig;
        fieldType = TypeName.get(element.asType());
        fieldName = element.getSimpleName().toString();
        serializedNameCandidates = new ArrayList<>();

        SerializedName annotation = element.getAnnotation(SerializedName.class);
        if (annotation != null) {
            serializedName = annotation.value();
            serializedNameCandidates.add(serializedName);
            Collections.addAll(serializedNameCandidates, annotation.alternate());
        } else {
            serializedName = GsonFieldNamingPolicy.translateName(fieldConfig.fieldNamingPolicy(), fieldName);
            serializedNameCandidates.add(serializedName);
        }
    }

    public TypeName getFieldType() {
        return fieldType;
    }

    public List<String> getSerializedNameCandidates() {
        return serializedNameCandidates;
    }

    public boolean isSimpleType() {
        return fieldType.equals(TypeName.BOOLEAN)
                || fieldType.equals(TypeName.INT)
                || fieldType.equals(TypeName.BYTE)
                || fieldType.equals(TypeName.SHORT)
                || fieldType.equals(TypeName.FLOAT)
                || fieldType.equals(TypeName.DOUBLE)
                || fieldType.equals(TypeName.BOOLEAN.box())
                || fieldType.equals(TypeName.INT.box())
                || fieldType.equals(TypeName.BYTE.box())
                || fieldType.equals(TypeName.SHORT.box())
                || fieldType.equals(TypeName.FLOAT.box())
                || fieldType.equals(TypeName.DOUBLE.box())
                || fieldType.equals(CLASS_NAME_STRING);
    }

    public CodeBlock buildWriteBlock(String object, String writer) {
        CodeBlock.Builder block = CodeBlock.builder();
        if (!fieldType.isPrimitive() && !fieldConfig.serializeNulls()) {
            block.beginControlFlow("if ($L.$L != null)", object, fieldName);
        }

        block.addStatement("$L.name($S)", writer, serializedName);

        if (!fieldType.isPrimitive() && fieldConfig.serializeNulls()) {
            block.beginControlFlow("if ($L.$L != null)", object, fieldName);
        }

        if (isSimpleType()) {
            block.addStatement("$L.value($L.$L)", writer, object, fieldName);
        } else {
            block.addStatement("$N.write($L, $L.$L)",
                    Utils.getField(fieldType), writer, object, fieldName);
        }

        if (!fieldType.isPrimitive()) {
            block.endControlFlow();
            if (fieldConfig.serializeNulls()) {
                block.beginControlFlow("else");
                block.addStatement("$L.nullValue()", writer);
                block.endControlFlow();
            }
        }
        return block.build();
    }

    public CodeBlock buildReadCodeBlock(String object, String reader) {
        CodeBlock.Builder block = CodeBlock.builder();
        TypeName unboxType;
        try {
            unboxType = fieldType.unbox();
        } catch (UnsupportedOperationException e) {
            unboxType = fieldType;
        }
        if (unboxType.equals(TypeName.BOOLEAN)) {
            block.addStatement("$L.$L = $L.nextBoolean()", object, fieldName, reader);
        } else if (unboxType.equals(TypeName.LONG)) {
            block.addStatement("$L.$L = $L.nextLong()", object, fieldName, reader);

        } else if (unboxType.equals(TypeName.INT)
                || unboxType.equals(TypeName.BYTE) || unboxType.equals(TypeName.SHORT)) {
            block.addStatement("$L.$L = ($T) $L.nextLong()", object, fieldName, unboxType, reader);
        } else if (unboxType.equals(TypeName.DOUBLE)) {
            block.addStatement("$L.$L = $L.nextDouble()", object, fieldName, reader);
        } else if (unboxType.equals(TypeName.FLOAT)) {
            block.addStatement("$L.$L = ($T) $L.nextDouble()", object, fieldName, unboxType, reader);
        } else if (unboxType.equals(CLASS_NAME_STRING)) {
            block.addStatement("$L.$L = $L.nextString()", object, fieldName, reader);
        } else {
            block.addStatement("$L.$L = $N.read($L)",
                    object, fieldName, Utils.getField(fieldType), reader);
        }

        return block.build();
    }

}
