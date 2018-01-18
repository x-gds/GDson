package com.gdson.processor;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.lang.model.element.Modifier;

public class Utils {
    public static final ClassName CLASS_NAME_TYPE_ADAPTER = ClassName.get(com.google.gson.TypeAdapter.class);
    private static final ClassName CLASS_NAME_GSON_TYPES = ClassName.get(com.google.gson.internal.$Gson$Types.class);

    public static ParameterizedTypeName getTypeAdapterName(TypeName type) {
        return ParameterizedTypeName.get(CLASS_NAME_TYPE_ADAPTER, type.box());
    }

    public static FieldSpec getField(TypeName type) {
        return FieldSpec.builder(Utils.getTypeAdapterName(type), getFieldName(type))
                .addModifiers(new Modifier[]{Modifier.PRIVATE, Modifier.FINAL})
                .build();
    }

    private static String getFieldName(TypeName type) {
        return "$" + type.toString().replaceAll("[^a-zA-Z0-9]+", "\\$");
    }

    public static CodeBlock getTypeCodeBlock(TypeName type) {
        if (type instanceof ParameterizedTypeName) {
            ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
            CodeBlock.Builder typeArgsExpr = CodeBlock.builder();
            for (int i = 0; i < parameterizedType.typeArguments.size(); i++) {
                TypeName t = parameterizedType.typeArguments.get(i);
                if (i != 0) {
                    typeArgsExpr.add(", $L", getTypeCodeBlock(t));
                } else {
                    typeArgsExpr.add("$L", getTypeCodeBlock(t));
                }
            }
            return CodeBlock.of("$T.newParameterizedTypeWithOwner(null, $T.class, $L)",
                    CLASS_NAME_GSON_TYPES, parameterizedType.rawType, typeArgsExpr.build());
        } else {
            return CodeBlock.of("$T.class", type);
        }
    }

    /**
     * 给生成的TypeAdapter添加{@link SuppressWarnings}注解
     *
     * @param warnings SuppressWarnings注解的值
     * @return 返回生成的SuppressWarnings注解
     */
    public static AnnotationSpec suppressWarnings(String... warnings) {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(SuppressWarnings.class);
        CodeBlock.Builder names = CodeBlock.builder();
        boolean first = true;
        for (String warning : warnings) {
            if (first) {
                names.add("$S", warning);
                first = false;
            } else {
                names.add(", $S", warning);
            }
        }
        if (warnings.length == 1) {
            builder.addMember("value", names.build());
        } else {
            builder.addMember("value", "{$L}", names.build());
        }
        return builder.build();
    }

    /**
     * 根据Model的类名得到TypeAdapter的类名
     *
     * @param modelType Model类
     * @return 返回TypeAdapter的类名, 如果Model类是内部类, 会在类名加上外部类类名加$
     */
    public static String createTypeAdapterClassName(ClassName modelType) {
        List<String> names = modelType.simpleNames();
        String modelClassName;
        if (names.size() > 1) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String name : names) {
                stringBuilder.append(name).append('$');
            }
            if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) == '$') {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            modelClassName = stringBuilder.toString();
        } else {
            modelClassName = names.get(0);
        }
        return getTypeAdapterName(modelClassName);
    }

    public static String getQualifiedTypeAdapterClassName(ClassName modelType) {
        return modelType.packageName() + "." + createTypeAdapterClassName(modelType);
    }

    private static String getTypeAdapterName(String modelClassName) {
        return modelClassName + "TypeAdapter";
    }
}
