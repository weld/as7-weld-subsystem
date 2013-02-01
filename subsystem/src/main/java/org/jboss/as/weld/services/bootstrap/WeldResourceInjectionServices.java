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
package org.jboss.as.weld.services.bootstrap;

import javax.annotation.Resource;
import javax.ejb.TimerService;
import javax.ejb.spi.HandleDelegate;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.weld.WeldLogger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.injection.spi.helpers.AbstractResourceServices;
import org.jboss.weld.injection.spi.helpers.SimpleResourceReference;

public class WeldResourceInjectionServices extends AbstractResourceServices implements ResourceInjectionServices {

    private static final String USER_TRANSACTION_LOCATION = "java:comp/UserTransaction";
    private static final String USER_TRANSACTION_CLASS_NAME = "javax.transaction.UserTransaction";
    private static final String HANDLE_DELEGATE_CLASS_NAME = "javax.ejb.spi.HandleDelegate";
    private static final String TIMER_SERVICE_CLASS_NAME = "javax.ejb.TimerService";
    private static final String ORB_CLASS_NAME = "org.omg.CORBA.ORB";

    private final Context context;

    private final ServiceRegistry serviceRegistry;

    private final EEModuleDescription moduleDescription;

    protected static String getEJBResourceName(InjectionPoint injectionPoint, String proposedName) {
        if (injectionPoint.getType() instanceof Class<?>) {
            Class<?> type = (Class<?>) injectionPoint.getType();
            if (USER_TRANSACTION_CLASS_NAME.equals(type.getName())) {
                return USER_TRANSACTION_LOCATION;
            } else if (HANDLE_DELEGATE_CLASS_NAME.equals(type.getName())) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(HandleDelegate.class, injectionPoint.getMember());
                return proposedName;
            } else if (ORB_CLASS_NAME.equals(type.getName())) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(org.omg.CORBA.ORB.class, injectionPoint.getMember());
                return proposedName;
            } else if (TIMER_SERVICE_CLASS_NAME.equals(type.getName())) {
                WeldLogger.ROOT_LOGGER.injectionTypeNotValue(TimerService.class, injectionPoint.getMember());
                return proposedName;
            }
        }
        return proposedName;
    }


    public WeldResourceInjectionServices(final ServiceRegistry serviceRegistry, final EEModuleDescription moduleDescription) {
        this.serviceRegistry = serviceRegistry;
        this.moduleDescription = moduleDescription;
        try {
            this.context = new InitialContext();
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Context getContext() {
        return context;
    }


    @Override
    protected String getResourceName(InjectionPoint injectionPoint) {
        Resource resource = injectionPoint.getAnnotated().getAnnotation(Resource.class);
        String mappedName = resource.mappedName();
        String lookup = resource.lookup();
        if (!lookup.isEmpty()) {
            return lookup;
        }
        if (!mappedName.isEmpty()) {
            return mappedName;
        }
        String proposedName = super.getResourceName(injectionPoint);
        return getEJBResourceName(injectionPoint, proposedName);
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final InjectionPoint injectionPoint) {
        final String result = getResourceName(injectionPoint);
        if (result.startsWith("java:global")
                || result.startsWith("java:app")
                || result.startsWith("java:module")
                || result.startsWith("java:comp")
                || result.startsWith("java:jboss")) {
            return handleServiceLookup(result);
        } else {

            return new ResourceReferenceFactory<Object>() {
                @Override
                public ResourceReference<Object> createResource() {
                    return new SimpleResourceReference<Object>(resolveResource(injectionPoint));
                }
            };
        }
    }

    @Override
    public ResourceReferenceFactory<Object> registerResourceInjectionPoint(final String jndiName, final String mappedName) {
        final String result = getResourceName(jndiName, mappedName);
        if (result.startsWith("java:global")
                || result.startsWith("java:app")
                || result.startsWith("java:module")
                || result.startsWith("java:comp")
                || result.startsWith("java:jboss")) {
            return handleServiceLookup(result);
        } else {

            return new ResourceReferenceFactory<Object>() {
                @Override
                public ResourceReference<Object> createResource() {
                    return new SimpleResourceReference<Object>(resolveResource(jndiName, mappedName));
                }
            };
        }
    }

    private ResourceReferenceFactory<Object> handleServiceLookup(final String result) {
        final ContextNames.BindInfo ejbBindInfo = ContextNames.bindInfoForEnvEntry(moduleDescription.getApplicationName(), moduleDescription.getModuleName(), moduleDescription.getModuleName(), false,  result);
        return new ResourceReferenceFactory<Object>() {
            @Override
            public ResourceReference<Object> createResource() {
                ServiceController<?> controller = serviceRegistry.getRequiredService(ejbBindInfo.getBinderServiceName());
                final ManagedReferenceFactory factory = (ManagedReferenceFactory) controller.getValue();
                final ManagedReference instance = factory.getReference();
                return new ResourceReference<Object>() {
                    @Override
                    public Object getInstance() {
                        return instance.getInstance();
                    }

                    @Override
                    public void release() {
                        instance.release();
                    }
                };
            }
        };
    }

    @Override
    public void cleanup() {
    }
}
