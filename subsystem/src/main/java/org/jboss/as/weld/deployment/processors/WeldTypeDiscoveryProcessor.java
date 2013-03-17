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

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.discovery.WeldTypeDiscoveryConfiguration;
import org.jboss.as.weld.util.Indices;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.TypeDiscoveryConfiguration;
import org.jboss.weld.bootstrap.spi.Metadata;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Processor which scans for bean-defining annotations (CDI scopes) and annotations annotated with required annotations.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldTypeDiscoveryProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (phaseContext.getDeploymentUnit().getParent() != null) {
            return; // only start WeldBootstrap for the root deployment
        }
        final DeploymentUnit rootDeploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(rootDeploymentUnit)) {
            return;
        }

        final List<Metadata<Extension>> extensions = rootDeploymentUnit.getAttachmentList(WeldAttachments.PORTABLE_EXTENSIONS);

        final WeldBootstrap bootstrap = new WeldBootstrap();
        final TypeDiscoveryConfiguration configuration = bootstrap.startExtensions(extensions);

        final CompositeIndex index = rootDeploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        Set<AnnotationType> beanDefiningAnnotations = getBeanDefiningAnnotations(configuration, index, rootDeploymentUnit);
        Set<AnnotationType> requiredAnnotations = flattenRequiredAnnotations(configuration, index);

        WeldTypeDiscoveryConfiguration discoveryConfiguration = new WeldTypeDiscoveryConfiguration(beanDefiningAnnotations, requiredAnnotations);

        rootDeploymentUnit.putAttachment(WeldAttachments.WELD_BOOTSTRAP, bootstrap);
        rootDeploymentUnit.putAttachment(WeldAttachments.WELD_TYPE_DISCOVERY_CONFIGURATION, discoveryConfiguration);
    }

    private Set<AnnotationType> getBeanDefiningAnnotations(TypeDiscoveryConfiguration configuration, CompositeIndex index, DeploymentUnit rootDeploymentUnit) {
        ImmutableSet.Builder<AnnotationType> builder = ImmutableSet.builder();
        // scopes known to weld
        builder.addAll(Lists.transform(new ArrayList<Class<? extends Annotation>>(configuration.getKnownBeanDefiningAnnotations()), AnnotationType.FOR_CLASS));
        // add additional scopes discovered in the deployment
        builder.addAll(rootDeploymentUnit.getAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEFINING_ANNOTATIONS));
        return builder.build();
    }

    private Set<AnnotationType> flattenRequiredAnnotations(TypeDiscoveryConfiguration configuration, CompositeIndex index) {
        ImmutableSet.Builder<AnnotationType> builder = ImmutableSet.builder();
        for (Class<? extends Annotation> requiredAnnotation : configuration.getAdditionalTypeMarkerAnnotations()) {
            AnnotationType annotation = AnnotationType.FOR_CLASS.apply(requiredAnnotation);
            builder.add(annotation);
            builder.addAll(Lists.transform(getAnnotationTargets(index.getAnnotations(annotation.getName()), Indices.ANNOTATION_FILTER), AnnotationType.FOR_CLASSINFO));
        }
        return builder.build();
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // noop
    }
}
