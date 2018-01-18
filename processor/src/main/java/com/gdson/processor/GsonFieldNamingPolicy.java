package com.gdson.processor;

import com.google.gson.FieldNamingPolicy;

import java.util.Locale;

/**
 * 复制的{@link com.google.gson.FieldNamingPolicy}中的几个方法
 */
public class GsonFieldNamingPolicy {
    public static String translateName(FieldNamingPolicy fieldNamingPolicy, String fieldName) {
        switch (fieldNamingPolicy) {
            case UPPER_CAMEL_CASE:
                return upperCaseFirstLetter(fieldName);
            case UPPER_CAMEL_CASE_WITH_SPACES:
                return upperCaseFirstLetter(separateCamelCase(fieldName, " "));
            case LOWER_CASE_WITH_UNDERSCORES:
                return separateCamelCase(fieldName, "_").toLowerCase(Locale.ENGLISH);
            case LOWER_CASE_WITH_DASHES:
                return separateCamelCase(fieldName, "-").toLowerCase(Locale.ENGLISH);
            default: // IDENTITY
                return fieldName;
        }
    }

    /**
     * Converts the field name that uses camel-case define word separation into
     * separate words that are separated by the provided {@code separatorString}.
     */
    private static String separateCamelCase(String name, String separator) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(separator);
            }
            translation.append(character);
        }
        return translation.toString();
    }

    /**
     * Ensures the JSON field names begins with an upper case letter.
     */
    private static String upperCaseFirstLetter(String name) {
        StringBuilder fieldNameBuilder = new StringBuilder();
        int index = 0;
        char firstCharacter = name.charAt(index);

        while (index < name.length() - 1) {
            if (Character.isLetter(firstCharacter)) {
                break;
            }

            fieldNameBuilder.append(firstCharacter);
            firstCharacter = name.charAt(++index);
        }

        if (index == name.length()) {
            return fieldNameBuilder.toString();
        }

        if (!Character.isUpperCase(firstCharacter)) {
            String modifiedTarget = modifyString(Character.toUpperCase(firstCharacter), name, ++index);
            return fieldNameBuilder.append(modifiedTarget).toString();
        } else {
            return name;
        }
    }

    private static String modifyString(char firstCharacter, String srcString, int indexOfSubstring) {
        return (indexOfSubstring < srcString.length())
                ? firstCharacter + srcString.substring(indexOfSubstring)
                : String.valueOf(firstCharacter);
    }
}
