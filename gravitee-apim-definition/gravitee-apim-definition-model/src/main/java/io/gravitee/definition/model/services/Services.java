/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model.services;

import io.gravitee.definition.model.Service;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public final class Services implements Serializable {

    private Map<Class<? extends Service>, Service> services = new HashMap<>();

    public Collection<Service> getAll() {
        if (services == null) {
            return null;
        }

        return services.values();
    }

    public <T extends Service> T get(Class<T> serviceType) {
        if (services == null) {
            return null;
        }

        return (T) services.get(serviceType);
    }

    public void set(Collection<? extends Service> services) {
        services.forEach((Consumer<Service>) service -> Services.this.services.put(service.getClass(), service));
    }

    public void put(Class<? extends Service> clazz, Service service) {
        this.services.put(clazz, service);
    }

    public void remove(Class<? extends Service> clazz) {
        this.services.remove(clazz);
    }

    public boolean isEmpty() {
        return (services == null) || services.isEmpty();
    }
}
