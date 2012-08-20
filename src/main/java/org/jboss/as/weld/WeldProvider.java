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

import java.util.Map;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.CDIProvider;

import org.jboss.as.weld.deployment.WeldDeployment;
import org.jboss.weld.Container;
import org.jboss.weld.Weld;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.reflection.Reflections;

import com.sun.faces.mgbean.BeanManager;

/**
 * Service provider for {@link CDI}.
 *
 * @author Jozef Hartinger
 *
 */
public class WeldProvider implements CDIProvider {

    private final Weld<?> weld = new EnhancedWeld();

    @Override
    public <T> CDI<T> getCDI() {
        return Reflections.cast(weld);
    }

    private static class EnhancedWeld extends Weld<Object> {

        /**
         * If the called of a {@link CDI} method is placed outside of BDA we return the {@link BeanManager} of the additional
         * bean archive which sees every {@link Bean} deployed in any of the bean archives. No alternatives / interceptors /
         * decorators are enabled for this {@link BeanManager}.
         */
        @Override
        protected BeanManagerImpl unsatisfiedBeanManager(String callerClassName) {
            for (Map.Entry<BeanDeploymentArchive, BeanManagerImpl> entry : Container.instance().beanDeploymentArchives().entrySet()) {
                if (entry.getKey().getId().endsWith(WeldDeployment.ADDITIONAL_CLASSES_BDA_SUFFIX)) {
                    return entry.getValue();
                }
            }
            return super.unsatisfiedBeanManager(callerClassName);
        }
    }
}
