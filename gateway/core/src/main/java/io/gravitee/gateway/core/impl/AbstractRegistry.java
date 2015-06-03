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
package io.gravitee.gateway.core.impl;

import io.gravitee.gateway.core.Registry;
import io.gravitee.model.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractRegistry implements Registry {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Set<Api> apis = new HashSet<Api>();

    protected void register(Api api) {
        LOGGER.info("Register a new API : {}", api);

        if (validate(api)) {
            // TODO: internal check
            // - Api not added twice
            // - Context path not already registered

            apis.add(api);
        }
    }

    private boolean validate(Api api) {
        if (api.getName() == null || api.getName().isEmpty()) {
            LOGGER.error("Unable to register API {} : name is missing", api);
            return false;
        }

        if (api.getContextPath() == null || api.getContextPath().isEmpty()) {
            LOGGER.error("Unable to register API {} : context path is missing", api);
            return false;
        }

        return true;
    }

    @Override
    public Set<Api> listAll() {
        return apis;
    }

    @Override
    public Api findMatchingApi(String uri) {
        return null;
    }
}
