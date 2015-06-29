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
package io.gravitee.gateway.core.repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.gravitee.gateway.api.Repository;
import io.gravitee.model.Api;

/**
 * Defines the default behaviour of registries.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractRepository implements Repository {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Set<Api> apis = new HashSet<>();

    protected boolean register(final Api api) {
        LOGGER.info("Registering a new API : {}", api);

        if (validate(api)) {
            apis.add(api);
            return true;
        }
        return false;
    }

    protected void deregisterAll() {
        LOGGER.info("Deregistering all APIs");
        apis.clear();
    }

    protected boolean validate(final Api api) {
        if (api == null) {
            return false;
        }

        if (api.getName() == null || api.getName().isEmpty()) {
            LOGGER.error("Unable to register API {} : name is missing", api);
            return false;
        }

        if (api.getPublicURI() == null) {
            LOGGER.error("Unable to register API {} : public URI is missing", api);
            return false;
        }

        if (api.getTargetURI() == null) {
            LOGGER.error("Unable to register API {} : target URI is missing", api);
            return false;
        }

        if (apis.stream().anyMatch(input -> api.getPublicURI().getPath().equals(input.getPublicURI().getPath()))) {
            LOGGER.error("Unable to register API {} : context path already exists", api);
            return false;
        }

        if (apis.stream().anyMatch(input -> api.getName().equals(input.getName()))) {
            LOGGER.error("Unable to register API {} : name already exists", api);
            return false;
        }

        return true;
    }

    @Override
    public Set<Api> listAll() {
        return Collections.unmodifiableSet(apis);
    }

    @Override
    public Api get(final String name) {
        return apis.stream().filter(input -> input.getName().equals(name)).findFirst().get();
    }
}
