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
package org.jboss.as.weld.injection;

import static org.jboss.weld.util.reflection.Reflections.cast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.web.common.WebComponentDescription;
import org.jboss.as.webservices.injection.WSComponentDescription;
import org.jboss.as.weld.WeldBootstrapService;
import org.jboss.as.weld.WeldLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.producer.InjectionTargetService;
import org.jboss.weld.literal.AnyLiteral;
import org.jboss.weld.manager.BeanManagerImpl;

/**
 * Managed reference factory that can be used to create and inject components.
 *
 * @author Stuart Douglas
 */
public class WeldManagedReferenceFactory implements ManagedReferenceFactory, Service<WeldManagedReferenceFactory> {

    private final Class<?> componentClass;
    private final InjectedValue<WeldBootstrapService> weldContainer;
    private final String ejbName;
    private final Set<Class<?>> interceptorClasses;
    private final Map<Class<?>, InjectionTarget> interceptorInjections = new HashMap<Class<?>, InjectionTarget>();
    private final ClassLoader classLoader;
    private final String beanDeploymentArchiveId;
    private final ComponentDescription componentDescription;

    private InjectionTarget injectionTarget;
    private Bean<?> bean;
    private BeanManagerImpl beanManager;

    public WeldManagedReferenceFactory(Class<?> componentClass, String ejbName, final Set<Class<?>> interceptorClasses, final ClassLoader classLoader, final String beanDeploymentArchiveId, ComponentDescription componentDescription) {
        this.componentClass = componentClass;
        this.ejbName = ejbName;
        this.beanDeploymentArchiveId = beanDeploymentArchiveId;
        this.weldContainer = new InjectedValue<WeldBootstrapService>();
        this.interceptorClasses = interceptorClasses;
        this.classLoader = classLoader;
        this.componentDescription = componentDescription;
    }

    @Override
    public ManagedReference getReference() {
        final CreationalContext<?> ctx;
        if (bean == null) {
            ctx = beanManager.createCreationalContext(null);
        } else {
            ctx = beanManager.createCreationalContext(bean);
        }
        final Object instance = injectionTarget.produce(ctx);
        return new WeldManagedReference(ctx, instance, injectionTarget, interceptorInjections);
    }

    public ManagedReference injectExistingReference(final ManagedReference existing) {
        final CreationalContext<?> ctx;
        if (bean == null) {
            ctx = beanManager.createCreationalContext(null);
        } else {
            ctx = beanManager.createCreationalContext(bean);
        }
        final Object instance = existing.getInstance();

        injectionTarget.inject(instance, ctx);

        return new ManagedReference() {
            @Override
            public void release() {
                try {
                    existing.release();
                } finally {
                    ctx.release();
                }
            }

            @Override
            public Object getInstance() {
                return instance;
            }
        };
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final ClassLoader cl = SecurityActions.getContextClassLoader();
        try {
            SecurityActions.setContextClassLoader(classLoader);
            beanManager = weldContainer.getValue().getBeanManager(beanDeploymentArchiveId);

            for (final Class<?> interceptor : interceptorClasses) {
                interceptorInjections.put(interceptor, new NonContextualComponentInjectionTarget(interceptor, null, beanManager));
            }

            if (ejbName != null) {
                EjbDescriptor<Object> descriptor = beanManager.getEjbDescriptor(ejbName);
                //may happen if the EJB was vetoed
                if (descriptor != null) {
                    bean = beanManager.getBean(descriptor);
                }
            }

            if (bean instanceof SessionBean<?>) {
                SessionBean<?> sessionBean = (SessionBean<?>) bean;
                this.injectionTarget = sessionBean.getInjectionTarget();
                return;
            }

            if (componentDescription instanceof WSComponentDescription) {
                ManagedBean<?> bean = findManagedBeanForWSComponent(componentClass);
                if (bean != null) {
                    injectionTarget = bean.getInjectionTarget();
                    return;
                }
            }

            NonContextualComponentInjectionTarget injectionTarget = new NonContextualComponentInjectionTarget(componentClass, bean, beanManager);
            if (componentDescription instanceof MessageDrivenComponentDescription || componentDescription instanceof WebComponentDescription) {
                // fire ProcessInjectionTarget for non-contextual components
                this.injectionTarget = beanManager.fireProcessInjectionTarget(injectionTarget.getAnnotated(), injectionTarget);
            } else {
                this.injectionTarget = injectionTarget;
            }
            beanManager.getServices().get(InjectionTargetService.class).validateProducer(injectionTarget);

        } finally {
            SecurityActions.setContextClassLoader(cl);
        }

    }

    private <T> ManagedBean<T> findManagedBeanForWSComponent(Class<T> definingClass) {
        Set<Bean<?>> beans = beanManager.getBeans(definingClass, AnyLiteral.INSTANCE);
        for (Iterator<Bean<?>> i = beans.iterator(); i.hasNext();) {
            Bean<?> bean = i.next();
            if (bean instanceof ManagedBean<?> && bean.getBeanClass().equals(definingClass)) {
                continue;
            }
            i.remove();
        }
        if (beans.isEmpty()) {
            WeldLogger.DEPLOYMENT_LOGGER.debugf("Could not find bean for %s, interception and decoration will be unavailable", componentClass);
            return null;
        }
        if (beans.size() > 1) {
            WeldLogger.DEPLOYMENT_LOGGER.debugf("Multiple beans for %s : %s ", componentClass, beans);
        }
        return cast(beans.iterator().next());
    }

    @Override
    public synchronized void stop(final StopContext context) {
        injectionTarget = null;
        interceptorInjections.clear();
        bean = null;
    }

    @Override
    public synchronized WeldManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<WeldBootstrapService> getWeldContainer() {
        return weldContainer;
    }
}
