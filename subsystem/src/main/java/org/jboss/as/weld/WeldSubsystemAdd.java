/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.weld;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.compatibility.ApplicationServerVersion;
import org.jboss.as.weld.deployment.CdiAnnotationProcessor;
import org.jboss.as.weld.deployment.processors.BeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.BeansXmlProcessor;
import org.jboss.as.weld.deployment.processors.ExternalBeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.LegacyBeansXmlProcessor;
import org.jboss.as.weld.deployment.processors.WebIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldBeanManagerServiceProcessor;
import org.jboss.as.weld.deployment.processors.WeldComponentIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldDependencyProcessor;
import org.jboss.as.weld.deployment.processors.WeldDeploymentProcessor;
import org.jboss.as.weld.deployment.processors.WeldPortableExtensionProcessor;
import org.jboss.as.weld.deployment.processors.WeldTypeDiscoveryProcessor;
import org.jboss.as.weld.services.TCCLSingletonService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * The weld subsystem add update handler.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 */
class WeldSubsystemAdd extends AbstractBoottimeAddStepHandler {
    public static final int PARSE_CDI_ANNOTATIONS                       = 0x2A10;
    public static final int POST_MODULE_WELD_WEB_INTEGRATION            = 0x0700;

    static final WeldSubsystemAdd INSTANCE = new WeldSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        OperationStepHandler handler = null;
        if (ApplicationServerVersion.isEqualOrNewer(ApplicationServerVersion.AS712Final)) {
            handler = new DeploymentProcessorRegistrar();
        } else {
            handler = new AS711DeploymentRegistrar();
        }
        context.addStep(handler, OperationContext.Stage.RUNTIME);

        TCCLSingletonService singleton = new TCCLSingletonService();
        newControllers.add(context.getServiceTarget().addService(TCCLSingletonService.SERVICE_NAME, singleton).setInitialMode(
                Mode.ON_DEMAND).install());
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }

    /**
     * The default registrar for Weld deployment processors.
     *
     * @author Jozef Hartinger
     *
     */
    private static class DeploymentProcessorRegistrar extends AbstractDeploymentChainStep {
        @Override
        protected void execute(DeploymentProcessorTarget processorTarget) {
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_CDI_ANNOTATIONS, new CdiAnnotationProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WELD_DEPLOYMENT, new BeansXmlProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new WeldDependencyProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, POST_MODULE_WELD_WEB_INTEGRATION, new WebIntegrationProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS, new WeldPortableExtensionProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS + 10, new WeldTypeDiscoveryProcessor()); // TODO: remove this relative priority and register in Phase
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS + 20, new BeanArchiveProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS + 30, new ExternalBeanArchiveProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_COMPONENT_INTEGRATION, new WeldComponentIntegrationProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new WeldDeploymentProcessor());
            processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WELD_BEAN_MANAGER, new WeldBeanManagerServiceProcessor());
        }

    }

    /**
     * Deployment registrar that is compatible with AS 7.1.1.Final.
     *
     * It uses the deprecated {@link DeploymentProcessorTarget#addDeploymentProcessor(Phase, int, DeploymentUnitProcessor)} method
     * and also registers {@link LegacyBeansXmlProcessor}.
     *
     * @author Jozef Hartinger
     *
     */
    private static class AS711DeploymentRegistrar extends AbstractDeploymentChainStep {
        @SuppressWarnings("deprecation")
        @Override
        protected void execute(DeploymentProcessorTarget processorTarget) {
            processorTarget.addDeploymentProcessor(Phase.PARSE, PARSE_CDI_ANNOTATIONS, new CdiAnnotationProcessor());
            processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WELD_DEPLOYMENT, new LegacyBeansXmlProcessor());
            processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new WeldDependencyProcessor());
            processorTarget.addDeploymentProcessor(Phase.POST_MODULE, POST_MODULE_WELD_WEB_INTEGRATION, new WebIntegrationProcessor());
            processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_BEAN_ARCHIVE, new BeanArchiveProcessor());
            processorTarget.addDeploymentProcessor(Phase.POST_MODULE, org.jboss.as.weld.compatibility.Phase.POST_MODULE_WELD_EXTERNAL_BEAN_ARCHIVE, new ExternalBeanArchiveProcessor());
            processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS, new WeldPortableExtensionProcessor());
            processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_WELD_COMPONENT_INTEGRATION, new WeldComponentIntegrationProcessor());
            processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new WeldDeploymentProcessor());
            processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WELD_BEAN_MANAGER, new WeldBeanManagerServiceProcessor());
        }
    }
}
