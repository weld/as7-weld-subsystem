/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.as.weld.WeldExtension;

public class Compatibility {

    private Compatibility() {
    }

    public static final Class<?> SERIALIZABLE_CDI_INTERCEPTORS_KEY_CLASS = classForName("org.jboss.as.ejb3.component.stateful.SerializedCdiInterceptorsKey");

    private static Class<?> classForName(String name) {
        try {
            return WeldExtension.class.getClassLoader().loadClass(name);
        } catch (Exception ingored) {
            return Object.class;
        }
    }
}
