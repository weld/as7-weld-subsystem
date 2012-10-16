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
package org.jboss.as.weld.ejb;

import java.util.Map;

import org.jboss.as.weld.util.ApplicationServerVersion;
import org.jboss.ejb.client.SessionID;
import org.jboss.msc.service.ServiceName;
import org.jboss.weld.ejb.api.SessionObjectReference;

/**
 * For 7.1.1.Final we need to use a {@link SessionObjectReference} implementation that is compatible with what's in 7.1.1.Final.
 *
 * @author Jozef Hartinger
 *
 */
public class StatefulSessionObjectReferenceFactory {

    private static boolean legacy = !ApplicationServerVersion.isEqualOrNewer(ApplicationServerVersion.AS712Final);

    public static SessionObjectReference create(final SessionID id, final ServiceName createServiceName, final Map<String, ServiceName> viewServices) {
        if (legacy) {
            return new LegacyStatefulSessionObjectReferenceImpl(id, createServiceName, viewServices);
        } else {
            return new StatefulSessionObjectReferenceImpl(id, createServiceName, viewServices);
        }
    }

    public static SessionObjectReference create(EjbDescriptorImpl<?> descriptor) {
        if (legacy) {
            return new LegacyStatefulSessionObjectReferenceImpl(descriptor);
        } else {
            return new StatefulSessionObjectReferenceImpl(descriptor);
        }
    }
}
