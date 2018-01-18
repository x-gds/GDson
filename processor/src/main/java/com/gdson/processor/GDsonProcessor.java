package com.gdson.processor;

import com.gdson.GDson;
import com.gdson.GDsonModule;
import com.google.auto.service.AutoService;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class GDsonProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> set = new HashSet<>(2);
        set.add(GDson.class.getCanonicalName());
        set.add(GDsonModule.class.getCanonicalName());
        return set;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return true;
        }

        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(GDson.class);
        for (Object element : elements) {
            TypeAdapterWriter typeAdapterWriter = new TypeAdapterWriter(processingEnv, (TypeElement) element);
            typeAdapterWriter.write(new SynchronizedFiler(processingEnv.getFiler()));
        }

        Set<? extends Element> moduleElements = roundEnvironment.getElementsAnnotatedWith(GDsonModule.class);
        TypeAdapterFactoryWriter typeAdapterFactoryWriter = new TypeAdapterFactoryWriter(processingEnv, elements, moduleElements);
        typeAdapterFactoryWriter.write(new SynchronizedFiler(processingEnv.getFiler()));
        return true;
    }
}
