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
package org.jboss.as.weld.deployment.processors;

import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.weld.bootstrap.api.CDI11Bootstrap;

/**
 * Represents the result of the initial phase of Weld bootstrap ({@link CDI11Bootstrap#startExtensions(Iterable)}).
 *
 * @author Jozef Hartinger
 *
 */
public class WeldTypeDiscoveryConfiguration {

    private final Set<DotName> beanDefiningAnnotations;
    private final Set<DotName> requiredAnnotations;

    protected WeldTypeDiscoveryConfiguration(Set<DotName> beanDefiningAnnotations, Set<DotName> requiredAnnotations) {
        this.beanDefiningAnnotations = beanDefiningAnnotations;
        this.requiredAnnotations = requiredAnnotations;
    }

    /**
     * @return bean defining annotations recognized by Weld combined with bean defining annotations recognized by scanning
     */
    public Set<DotName> getBeanDefiningAnnotations() {
        return beanDefiningAnnotations;
    }

    /**
     * @return a set containing required annotations and application annotations annotated with required annotations
     */
    public Set<DotName> getRequiredAnnotations() {
        return requiredAnnotations;
    }

    @Override
    public String toString() {
        return "WeldTypeDiscoveryConfiguration [beanDefiningAnnotations=" + beanDefiningAnnotations + ", requiredAnnotations=" + requiredAnnotations + "]";
    }
}
