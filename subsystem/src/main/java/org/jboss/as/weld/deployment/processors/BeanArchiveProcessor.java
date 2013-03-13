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
package org.jboss.as.weld.deployment.processors;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.annotation.AnnotationIndexUtils;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.weld.WeldDeploymentMarker;
import org.jboss.as.weld.WeldLogger;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.BeanDeploymentModule;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadata;
import org.jboss.as.weld.deployment.ExplicitBeanArchiveMetadataContainer;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.as.weld.discovery.BeanDefiningAnnotationTargetDiscovery;
import org.jboss.as.weld.discovery.RequiredAnnotationTargetDiscovery;
import org.jboss.as.weld.discovery.WeldTypeDiscoveryConfiguration;
import org.jboss.as.weld.ejb.EjbDescriptorImpl;
import org.jboss.as.weld.services.bootstrap.WeldJpaInjectionServices;
import org.jboss.as.weld.util.Indices;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.modules.Module;
import org.jboss.weld.bootstrap.spi.BeanDiscoveryMode;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.injection.spi.JpaInjectionServices;

import com.google.common.collect.Lists;

/**
 * Deployment processor that builds bean archives and attaches them to the deployment
 * <p/>
 * Currently this is done by pulling the information out of the jandex {@link Index}.
 * <p/>
 *
 * @author Stuart Douglas
 */
public class BeanArchiveProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!WeldDeploymentMarker.isPartOfWeldDeployment(deploymentUnit)) {
            return;
        }

        final ResourceRootProcessor resourceRootProcessor = new ResourceRootProcessor(deploymentUnit);
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(Attachments.REFLECTION_INDEX);

        final String beanArchiveIdPrefix;
        if (deploymentUnit.getParent() == null) {
            beanArchiveIdPrefix = deploymentUnit.getName();
        } else {
            beanArchiveIdPrefix = deploymentUnit.getParent().getName() + "." + deploymentUnit.getName();
        }

        final Set<BeanDeploymentArchiveImpl> beanDeploymentArchives = new HashSet<BeanDeploymentArchiveImpl>();
        WeldLogger.DEPLOYMENT_LOGGER.processingWeldDeployment(phaseContext.getDeploymentUnit().getName());

        final Map<ResourceRoot, Index> indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
        final Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap = new HashMap<ResourceRoot, BeanDeploymentArchiveImpl>();

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        BeanDeploymentArchiveImpl rootBda = null;

        List<ResourceRoot> structure = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : structure) {
            if (ModuleRootMarker.isModuleRoot(resourceRoot) && !SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                if ("classes".equals(resourceRoot.getRootName())) {
                    continue; // this is handled below
                }
                BeanDeploymentArchiveImpl bda = resourceRootProcessor.processResourceRoot(resourceRoot);
                if (bda != null) {
                    beanDeploymentArchives.add(bda);
                    bdaMap.put(resourceRoot, bda);
                    if (bda.isRoot()) {
                        rootBda = bda;
                        deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bda);
                    }
                }
            }
        }

        // handle jar deployments
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            BeanDeploymentArchiveImpl bda = resourceRootProcessor.processResourceRoot(resourceRoot);
            if (bda != null) {
                beanDeploymentArchives.add(bda);
                bdaMap.put(resourceRoot, bda);
                if (bda.isRoot()) {
                    rootBda = bda;
                    deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bda);
                }
            }
        }

        if (rootBda == null) {
            BeanDeploymentArchiveImpl bda = new BeanDeploymentArchiveImpl(Collections.<String>emptySet(), Collections.<String>emptySet(),
                    BeansXml.EMPTY_BEANS_XML, module, beanArchiveIdPrefix, true);
            beanDeploymentArchives.add(bda);
            deploymentUnit.putAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE, bda);
            rootBda = bda;
        }
        processEEComponents(deploymentUnit, bdaMap, rootBda, indexes, reflectionIndex);

        final JpaInjectionServices jpaInjectionServices = new WeldJpaInjectionServices(deploymentUnit, deploymentUnit.getServiceRegistry());

        final BeanDeploymentModule bdm = new BeanDeploymentModule(beanDeploymentArchives);
        bdm.addService(JpaInjectionServices.class, jpaInjectionServices);
        deploymentUnit.putAttachment(WeldAttachments.BEAN_DEPLOYMENT_MODULE, bdm);
    }


    private void processEEComponents(DeploymentUnit deploymentUnit, Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap, BeanDeploymentArchiveImpl rootBda, Map<ResourceRoot, Index> indexes, DeploymentReflectionIndex reflectionIndex) {
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        for (ComponentDescription component : moduleDescription.getComponentDescriptions()) {
            BeanDeploymentArchiveImpl bda = resolveComponentBda(component.getComponentClassName(), bdaMap, rootBda, indexes);
            component.setBeanDeploymentArchiveId(bda.getId());
            if (component instanceof EJBComponentDescription) {
                final EJBComponentDescription componentDescription = (EJBComponentDescription) component;
                //first we need to resolve the correct BDA for the bean
                bda.addEjbDescriptor(new EjbDescriptorImpl<Object>(componentDescription, bda, reflectionIndex));
            }
        }
    }

    /**
     * Resolves the bean deployment archive for a session bean
     *
     * @param ejbClassName the session bean's class
     * @param bdaMap       The BDA's keyed by resource root
     * @param rootBda      The root bda, this is used as the BDA of last resort if the correct BDA cannot be found
     * @param indexes      The jandex indexes
     * @return The correct BDA for the EJB
     */
    private BeanDeploymentArchiveImpl resolveComponentBda(String ejbClassName, Map<ResourceRoot, BeanDeploymentArchiveImpl> bdaMap, BeanDeploymentArchiveImpl rootBda, Map<ResourceRoot, Index> indexes) {
        final DotName className = DotName.createSimple(ejbClassName);
        for (Map.Entry<ResourceRoot, BeanDeploymentArchiveImpl> entry : bdaMap.entrySet()) {
            final Index index = indexes.get(entry.getKey());
            if (index != null) {
                if (index.getClassByName(className) != null) {
                    return entry.getValue();
                }
            }
        }
        return rootBda;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private static class ResourceRootProcessor {
        private final ExplicitBeanArchiveMetadataContainer explicitBeanArchives;
        private final Module module;
        private final String prefix;
        private final Map<ResourceRoot, Index> indexes;
        private final WeldTypeDiscoveryConfiguration discoveryConfiguration;

        private ResourceRootProcessor(DeploymentUnit deploymentUnit) {
            this.explicitBeanArchives = deploymentUnit.getAttachment(ExplicitBeanArchiveMetadataContainer.ATTACHMENT_KEY);
            this.module = deploymentUnit.getAttachment(Attachments.MODULE);
            String prefix = deploymentUnit.getName();
            if (deploymentUnit.getParent() != null) {
                prefix = deploymentUnit.getParent().getName() + "." + prefix;
            }
            this.prefix = prefix;
            this.indexes = AnnotationIndexUtils.getAnnotationIndexes(deploymentUnit);
            this.discoveryConfiguration = deploymentUnit.getAttachment(WeldAttachments.WELD_TYPE_DISCOVERY_CONFIGURATION);
        }

        private BeanDeploymentArchiveImpl processResourceRoot(ResourceRoot resourceRoot) throws DeploymentUnitProcessingException {
            ExplicitBeanArchiveMetadata metadata = null;
            if (explicitBeanArchives != null) {
                metadata = explicitBeanArchives.getBeanArchiveMetadata().get(resourceRoot);
            }
            if (metadata == null || metadata.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.ANNOTATED)) {
                // this is either an implicit bean archive or not a bean archive at all!
                Index index = indexes.get(resourceRoot);
                if (index == null) {
                    return null; // index may be null for some resource roots
                }

                Set<String> beans = getImplicitBeanClasses(index);
                Set<String> additionalTypes = getAdditionalTypes(index, beans);

                if (beans.isEmpty() && additionalTypes.isEmpty()) {
                    return null;
                }
                // TODO: EJBs in an archive should cause implicit BDA!
                // TODO: provide beans.xml if found  ------------------------>
                return new BeanDeploymentArchiveImpl(beans, additionalTypes, null, module, prefix + resourceRoot.getRoot().getPathName());
            } else if (metadata.getBeansXml().getBeanDiscoveryMode().equals(BeanDiscoveryMode.NONE)) {
                // scanning suppressed per spec in this archive
                return null;
            } else {
                boolean isRootBda = metadata.isDeploymentRoot();
                return createBeanDeploymentArchive(indexes.get(metadata.getResourceRoot()), metadata, isRootBda);
            }
        }

        private Set<String> getImplicitBeanClasses(Index index) {
            RequiredAnnotationTargetDiscovery discovery = new BeanDefiningAnnotationTargetDiscovery(index);
            List<ClassInfo> classes = discovery.getAffectedClasses(discoveryConfiguration.getBeanDefiningAnnotations());
            return new HashSet<String>(Lists.transform(classes, Indices.CLASSINFO_TO_STRING_FUNCTION));
        }

        private Set<String> getAdditionalTypes(Index index, Set<String> beanClasses) {
            RequiredAnnotationTargetDiscovery discovery = new RequiredAnnotationTargetDiscovery(index);
            List<ClassInfo> classes = discovery.getAffectedClasses(discoveryConfiguration.getRequiredAnnotations());
            Set<String> types = new HashSet<String>(Lists.transform(classes, Indices.CLASSINFO_TO_STRING_FUNCTION));
            types.removeAll(beanClasses);
            return types;
        }

        private BeanDeploymentArchiveImpl createBeanDeploymentArchive(final Index index, ExplicitBeanArchiveMetadata beanArchiveMetadata, boolean root) throws DeploymentUnitProcessingException {

            Set<String> classNames = new HashSet<String>();
            // index may be null if a war has a beans.xml but no WEB-INF/classes
            if (index != null) {
                for (ClassInfo classInfo : index.getKnownClasses()) {
                    classNames.add(classInfo.name().toString());
                }
            }

            String beanArchiveId = prefix;
            if (beanArchiveMetadata.getResourceRoot() != null) {
                beanArchiveId += beanArchiveMetadata.getResourceRoot().getRoot().getPathName();
            }
            return new BeanDeploymentArchiveImpl(classNames, Collections.<String> emptySet(), beanArchiveMetadata.getBeansXml(), module, beanArchiveId, root);
        }
    }
}
