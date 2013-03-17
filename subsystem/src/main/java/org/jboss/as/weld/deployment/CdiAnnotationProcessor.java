/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.weld.deployment;

import static org.jboss.as.weld.util.Indices.getAnnotationTargets;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.CdiAnnotations;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.DotName;

import com.google.common.collect.Lists;

/**
 * CdiAnnotationProcessor class. Used to verify the presence of CDI annotations.
 */
public class CdiAnnotationProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // TODO: it should be enough to do this for toplevel deployment only

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit rootDeploymentUnit = (deploymentUnit.getParent() == null) ? deploymentUnit : deploymentUnit.getParent();

        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        processCdiAnnotations(index, deploymentUnit);

        List<AnnotationType> additionalScopes = new ArrayList<AnnotationType>();
        // found normal scopes (may overlap with scopes known to weld)
        additionalScopes.addAll(Lists.transform(getAnnotationTargets(index.getAnnotations(CdiAnnotations.NORMAL_SCOPE), Indices.ANNOTATION_FILTER), AnnotationType.FOR_CLASSINFO));
        // found pseudo scopes (may overlap with scopes known to weld)
        additionalScopes.addAll(Lists.transform(getAnnotationTargets(index.getAnnotations(CdiAnnotations.SCOPE), Indices.ANNOTATION_FILTER), AnnotationType.FOR_CLASSINFO));

        processBeanArchives(index, deploymentUnit, additionalScopes);

        for (AnnotationType annotationType : additionalScopes) {
            rootDeploymentUnit.addToAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEFINING_ANNOTATIONS, annotationType);
        }
    }

    private void processCdiAnnotations(CompositeIndex index, DeploymentUnit deploymentUnit) {
        for (final CdiAnnotations annotation : CdiAnnotations.values()) {
            if (!index.getAnnotations(annotation.getDotName()).isEmpty()) {
                CdiAnnotationMarker.mark(deploymentUnit);
                return;
            }
        }
    }

    private void processBeanArchives(CompositeIndex index, DeploymentUnit deploymentUnit, List<AnnotationType> additionalScopes) {
        for (DotName scope : CdiAnnotations.BUILT_IN_SCOPES) {
            if (!index.getAnnotations(scope).isEmpty()) {
                WeldDeploymentMarker.mark(deploymentUnit);
                return;
            }
        }
        for (AnnotationType scope : additionalScopes) {
            if (!index.getAnnotations(scope.getName()).isEmpty()) {
                WeldDeploymentMarker.mark(deploymentUnit);
                return;
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
