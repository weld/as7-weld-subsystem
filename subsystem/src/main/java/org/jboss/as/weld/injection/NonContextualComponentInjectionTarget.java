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

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.injection.producer.BasicInjectionTarget;
import org.jboss.weld.injection.producer.LifecycleCallbackInvoker;
import org.jboss.weld.injection.producer.NoopLifecycleCallbackInvoker;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.resources.ClassTransformer;

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
public class NonContextualComponentInjectionTarget<T> extends BasicInjectionTarget<T> {

    @SuppressWarnings("unchecked")
    public NonContextualComponentInjectionTarget(Class<?> componentClass, Bean<T> bean, BeanManagerImpl beanManager) {
        this(beanManager.getServices().get(ClassTransformer.class).getEnhancedAnnotatedType((Class<T>) componentClass, beanManager.getId()), bean, beanManager);
    }

    public NonContextualComponentInjectionTarget(EnhancedAnnotatedType<T> type, Bean<T> bean, BeanManagerImpl beanManager) {
        super(type, bean, beanManager);
    }

    @Override
    protected LifecycleCallbackInvoker<T> initInvoker(EnhancedAnnotatedType<T> type) {
        return NoopLifecycleCallbackInvoker.getInstance();
    }
}
