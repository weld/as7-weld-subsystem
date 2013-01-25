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
package org.jboss.as.weld.injection;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.Container;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.SessionBean;
import org.jboss.weld.injection.CurrentInjectionPoint;
import org.jboss.weld.injection.ForwardingInjectionPoint;
import org.jboss.weld.injection.InjectionContextImpl;
import org.jboss.weld.injection.SLSBInvocationInjectionPoint;
import org.jboss.weld.injection.producer.AbstractInjectionTarget;
import org.jboss.weld.injection.producer.DefaultInstantiator;
import org.jboss.weld.injection.producer.Instantiator;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.Beans;

/**
 * {@link InjectionTarget} implementation used for non-contextual EE components
 * such as message-driven beans, Servlets, tag library handlers, ...
 *
 * This {@link InjectionTarget} implementation does not provider resource injection
 * as it would otherwise be performed twice.
 *
 * @author Jozef Hartinger
 * @author Marko Luksa
 *
 * @param <T>
 */
public class NonContextualComponentInjectionTarget<T> extends AbstractInjectionTarget<T> {

    @SuppressWarnings("unchecked")
    public NonContextualComponentInjectionTarget(Class<?> componentClass, Bean<T> bean, BeanManagerImpl beanManager) {
        this(beanManager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType((Class<T>) componentClass, beanManager.getId()), bean, beanManager);
    }

    public NonContextualComponentInjectionTarget(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager) {
        super(type, bean, beanManager);
    }

    @Override
    public void inject(final T instance, final CreationalContext<T> ctx) {
        new InjectionContextImpl<T>(getBeanManager(), this, getType(), instance) {

            public void proceed() {
                if (isStatelessSessionBean()) {
                    CurrentInjectionPoint currentInjectionPoint = Container.instance().services().get(CurrentInjectionPoint.class);
                    currentInjectionPoint.push(new SLSBDynamicInjectionPoint());
                    try {
                        injectFieldsAndInitializers();
                    } finally {
                        currentInjectionPoint.pop();
                    }
                } else {
                    injectFieldsAndInitializers();
                }
            }

            private boolean isStatelessSessionBean() {
                if (getBean() instanceof SessionBean) {
                    SessionBean<T> sessionBean = (SessionBean<T>) getBean();
                    return sessionBean.getEjbDescriptor().isStateless();
                } else {
                    return false;
                }
            }

            private void injectFieldsAndInitializers() {
                Beans.injectFieldsAndInitializers(instance, ctx, getBeanManager(), getInjectableFields(), getInitializerMethods());
            }

        }.run();
    }

    @Override
    protected Instantiator<T> initInstantiator(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager,
            Set<InjectionPoint> injectionPoints) {
        return new DefaultInstantiator<T>(type, bean, beanManager);
    }

    @Override
    protected List<AnnotatedMethod<? super T>> initPostConstructMethods(EnhancedAnnotatedType<T> type) {
        // suppress lifecycle callback invocation - this is handled by AS
        return Collections.emptyList();
    }

    @Override
    protected List<AnnotatedMethod<? super T>> initPreDestroyMethods(EnhancedAnnotatedType<T> type) {
        // suppress lifecycle callback invocation - this is handled by AS
        return Collections.emptyList();
    }

    private static class SLSBDynamicInjectionPoint extends ForwardingInjectionPoint implements Serializable {
        private static final long serialVersionUID = 0L;

        protected InjectionPoint delegate() {
            return Container.instance().services().get(SLSBInvocationInjectionPoint.class).peek();
        }
    }
}
