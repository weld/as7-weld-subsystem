/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld.util;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Utilities for working with Jandex indices.
 *
 * @author Jozef Hartinger
 *
 */
public class Indices {

    public static final DotName INHERITED_NAME = DotName.createSimple(Inherited.class.getName());

    public static final Function<ClassInfo, String> CLASS_INFO_TO_FQCN = new Function<ClassInfo, String>() {
        @Override
        public String apply(ClassInfo input) {
            return input.name().toString();
        }
    };

    private static final int ANNOTATION = 0x00002000;

    public static final Predicate<ClassInfo> ANNOTATION_FILTER = new Predicate<ClassInfo>() {
        @Override
        public boolean apply(ClassInfo input) {
            return isAnnotation(input);
        }
    };

    public static final Function<ClassInfo, String> CLASSINFO_TO_STRING_FUNCTION = new Function<ClassInfo, String>() {
        @Override
        public String apply(ClassInfo input) {
            return input.name().toString();
        }
    };

    private Indices() {
    }

    public static boolean isAnnotation(ClassInfo clazz) {
        return (clazz.flags() & ANNOTATION) != 0;
    }

    public static List<ClassInfo> getAnnotationTargets(List<AnnotationInstance> instances) {
        return getAnnotationTargets(instances, null);
    }

    public static List<ClassInfo> getAnnotationTargets(List<AnnotationInstance> instances, Predicate<ClassInfo> predicate) {
        List<ClassInfo> result = new ArrayList<ClassInfo>();
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                processClass(result, (ClassInfo) target, predicate);
            }
        }
        return result;
    }

    private static void processClass(List<ClassInfo> result, ClassInfo clazz, Predicate<ClassInfo> predicate) {
        if (predicate != null) {
            if (!predicate.apply(clazz)) {
                return;
            }
        }
        result.add(clazz);
    }

    public interface Filter {
        boolean accepts(ClassInfo target);
    }
}
