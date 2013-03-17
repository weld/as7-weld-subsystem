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

import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.weld.deployment.processors.WeldDependencyProcessor;
import org.jboss.as.weld.deployment.processors.WeldTypeDiscoveryProcessor;
import org.jboss.as.weld.discovery.AnnotationType;
import org.jboss.as.weld.discovery.WeldTypeDiscoveryConfiguration;
import org.jboss.weld.bootstrap.WeldBootstrap;
import org.jboss.weld.bootstrap.api.CDI11Bootstrap;
import org.jboss.weld.bootstrap.spi.Metadata;

/**
 * {@link AttachmentKey}s for weld attachments
 *
 * @author Stuart Douglas
 *
 */
public class WeldAttachments {

    /**
     * The {@link BeanDeploymentModule} for a deployment
     */
    public static final AttachmentKey<BeanDeploymentModule> BEAN_DEPLOYMENT_MODULE = AttachmentKey.create(BeanDeploymentModule.class);

    /**
     * top level list of all additional bean deployment modules
     */
    public static final AttachmentKey<AttachmentList<BeanDeploymentModule>> ADDITIONAL_BEAN_DEPLOYMENT_MODULES = AttachmentKey.createList(BeanDeploymentModule.class);

    /**
     * per DU list of all visible additional BDM's
     */
    public static final AttachmentKey<AttachmentList<BeanDeploymentModule>> VISIBLE_ADDITIONAL_BEAN_DEPLOYMENT_MODULE = AttachmentKey.createList(BeanDeploymentModule.class);

    /**
     * The {@link BeanDeploymentArchiveImpl} that corresponds to the main resource root of a deployment or sub deployment. For
     * consistency, the bean manager that corresponds to this bda is always bound to the java:comp namespace for web modules.
     */
    public static final AttachmentKey<BeanDeploymentArchiveImpl> DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE = AttachmentKey.create(BeanDeploymentArchiveImpl.class);

    /**
     * Portable extensions discovered in sub deployments. All sub deployments may contain portable extensions, even ones without
     * beans.xml files
     */
    public static final AttachmentKey<AttachmentList<Metadata<Extension>>> PORTABLE_EXTENSIONS = AttachmentKey.createList(Metadata.class);

    /**
     * Represents the result of the initial phase of Weld bootstrap ({@link CDI11Bootstrap#startExtensions(Iterable)})
     */
    public static final AttachmentKey<WeldTypeDiscoveryConfiguration> WELD_TYPE_DISCOVERY_CONFIGURATION = AttachmentKey.create(WeldTypeDiscoveryConfiguration.class);

    /**
     * {@link WeldBootstrap} needs to be instantiated by {@link WeldTypeDiscoveryProcessor} in order to create a
     * {@link WeldTypeDiscoveryConfiguration} instance. Afterwards, the {@link WeldBootstrap} instance is propagated to
     * {@link WeldDependencyProcessor} (where the rest of initialization is performed) as an attachment.
     */
    public static final AttachmentKey<WeldBootstrap> WELD_BOOTSTRAP = AttachmentKey.create(WeldBootstrap.class);

    /**
     * The {@link ResourceRoot} for WEB-INF/classes of a web archive.
     */
    public static final AttachmentKey<ResourceRoot> CLASSES_RESOURCE_ROOT = AttachmentKey.create(ResourceRoot.class);

    /**
     * Scopes for built-in contexts combined with additional normal and pseudo scopes discovered in the deployment.
     * @see CdiAnnotationProcessor
     * @see WeldTypeDiscoveryProcessor
     */
    public static final AttachmentKey<AttachmentList<AnnotationType>> ADDITIONAL_BEAN_DEFINING_ANNOTATIONS = AttachmentKey.createList(AnnotationType.class);
}
