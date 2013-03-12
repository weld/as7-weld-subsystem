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

import static org.jboss.as.weld.util.Indices.getAnnotationTargets;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.NormalScope;
import javax.enterprise.inject.spi.Extension;
import javax.inject.Scope;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.DotName;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.TypeDiscoveryConfiguration;
import org.jboss.weld.bootstrap.spi.Metadata;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Processor which scans for bean-defining annotations (CDI scopes) and annotations annotated with required annotations.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldTypeDiscoveryProcessor implements DeploymentUnitProcessor {

    private final DotName NORMAL_SCOPE_DOTNAME = DotName.createSimple(NormalScope.class.getName());
    private final DotName SCOPE_DOTNAME = DotName.createSimple(Scope.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        final List<Metadata<Extension>> extensions = deploymentUnit.getAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS);

        final WeldBootstrap bootstrap = new WeldBootstrap();
        final TypeDiscoveryConfiguration configuration = bootstrap.startExtensions(extensions);

        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        Set<DotName> beanDefiningAnnotations = getBeanDefiningAnnotations(configuration, index);
        Set<DotName> requiredAnnotations = flattenRequiredAnnotations(configuration, index);

        WeldTypeDiscoveryConfiguration discoveryConfiguration = new WeldTypeDiscoveryConfiguration(beanDefiningAnnotations, requiredAnnotations);

        deploymentUnit.putAttachment(WeldAttachments.WELD_BOOTSTRAP, bootstrap);
        deploymentUnit.putAttachment(WeldAttachments.WELD_TYPE_DISCOVERY_CONFIGURATION, discoveryConfiguration);
    }

    private Set<DotName> getBeanDefiningAnnotations(TypeDiscoveryConfiguration configuration, CompositeIndex index) {
        ImmutableSet.Builder<DotName> builder = ImmutableSet.builder();
        // scopes known to weld
        builder.addAll(Lists.transform(new ArrayList<Class<?>>(configuration.getKnownBeanDefiningAnnotations()), new DotNameFunction()));
        // found normal scopes (may overlap with scopes known to weld)
        builder.addAll(getAnnotationTargets(index.getAnnotations(NORMAL_SCOPE_DOTNAME)));
        // found pseudo scopes (may overlap with scopes known to weld)
        builder.addAll(getAnnotationTargets(index.getAnnotations(SCOPE_DOTNAME)));
        return builder.build();
    }

    private Set<DotName> flattenRequiredAnnotations(TypeDiscoveryConfiguration configuration, CompositeIndex index) {
        ImmutableSet.Builder<DotName> builder = ImmutableSet.builder();
        for (Class<? extends Annotation> requiredAnnotation : configuration.getAdditionalTypeMarkerAnnotations()) {
            DotName annotationName = DotName.createSimple(requiredAnnotation.getName());
            builder.add(annotationName);
            builder.addAll(getAnnotationTargets(index.getAnnotations(annotationName), Indices.ANNOTATION_FILTER));
        }
        return builder.build();
    }

    private static class DotNameFunction implements Function<Class<?>, DotName> {
        @Override
        public DotName apply(Class<?> input) {
            return DotName.createSimple(input.getName());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // noop
    }
}
