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
package org.jboss.as.weld.discovery;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

public class BeanDefiningAnnotationTargetDiscovery extends RequiredAnnotationTargetDiscovery {

    public BeanDefiningAnnotationTargetDiscovery(Index index) {
        super(index);
    }

    @Override
    protected void processParameter(List<ClassInfo> classes, MethodParameterInfo parameter) {
        return; // bean defining annotations cannot be applied on parameters
    }

    @Override
    protected void processField(List<ClassInfo> classes, FieldInfo field) {
        classes.add(field.declaringClass()); // do not process subclasses as producer fields are not inherited
    }

    @Override
    protected void processMethod(List<ClassInfo> classes, MethodInfo method) {
        classes.add(method.declaringClass()); // do not process subclasses as producer methods are not inherited
    }

    @Override
    protected void processConstructor(List<ClassInfo> classes, MethodInfo method) {
        // noop - bean defining annotations cannot be applied on constructors
    }
}
