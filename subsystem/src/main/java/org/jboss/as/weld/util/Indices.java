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

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import com.google.common.base.Function;

/**
 * Utilities for working with Jandex indices.
 *
 * @author Jozef Hartinger
 *
 */
public class Indices {

    public static final Function<ClassInfo, String> CLASS_INFO_TO_FQCN = new Function<ClassInfo, String>() {
        @Override
        public String apply(ClassInfo input) {
            return input.name().toString();
        }
    };

    private static final int ANNOTATION = 0x00002000;

    public static final Filter ANNOTATION_FILTER = new AnnotationFilter();

    public static final Function<Class<?>, DotName> CLASS_TO_DOTNAME_FUNCTION = new Function<Class<?>, DotName>() {
        @Override
        public DotName apply(Class<?> input) {
            return DotName.createSimple(input.getName());
        }
    };

    private Indices() {
    }

    public static boolean isAnnotation(ClassInfo clazz) {
        return (clazz.flags() & ANNOTATION) != 0;
    }

    public static List<DotName> getAnnotationTargets(List<AnnotationInstance> instances) {
        return getAnnotationTargets(instances, null);
    }

    public static List<DotName> getAnnotationTargets(List<AnnotationInstance> instances, Filter filter) {
        List<DotName> result = new ArrayList<DotName>();
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                ClassInfo clazz = (ClassInfo) target;
                if (filter == null || filter.accepts(clazz)) {
                    result.add(clazz.name());
                }
            }
        }
        return result;
    }

    public interface Filter {
        boolean accepts(ClassInfo target);
    }

    private static class AnnotationFilter implements Filter {
        @Override
        public boolean accepts(ClassInfo target) {
            return isAnnotation(target);
        }
    }
}
