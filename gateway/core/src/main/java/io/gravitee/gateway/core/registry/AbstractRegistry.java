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
package io.gravitee.gateway.core.registry;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import io.gravitee.gateway.api.Registry;
import io.gravitee.model.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractRegistry implements Registry {

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

    protected boolean deregister(final Api api) {
        LOGGER.info("Deregistering an API : {}", api);
        return apis.remove(api);
    }

    private boolean validate(final Api api) {
        if (api == null) {
            return false;
        }

        if (Strings.isNullOrEmpty(api.getName())) {
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

        final boolean isContextPathExists = FluentIterable.from(apis).anyMatch(new Predicate<Api>() {
            @Override
            public boolean apply(final Api input) {
                return api.getPublicURI().getPath().equals(input.getPublicURI().getPath());
            }
        });

        if (isContextPathExists) {
            LOGGER.error("Unable to register API {} : context path already exists", api);
            return false;
        }

        final boolean isNameExists = FluentIterable.from(apis).anyMatch(new Predicate<Api>() {
            @Override
            public boolean apply(final Api input) {
                return api.getName().equals(input.getName());
            }
        });

        if (isNameExists) {
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
    public Api findMatchingApi(final String uri) {
        if (Strings.isNullOrEmpty(uri)) {
            return null;
        }
        return FluentIterable.from(apis).firstMatch(new Predicate<Api>() {
            @Override
            public boolean apply(final Api input) {
                return uri.startsWith(input.getPublicURI().getPath());
            }
        }).orNull();
    }

    @Override
    public boolean createApi(final Api api) {
        if (validate(api)) {
            writeApi(api);
            return true;
        }
        return false;
    }

    @Override
    public boolean reloadApi(final String name) {
        final Api api = findApiByName(name);
        deregister(api);
        return register(api);
    }

    @Override
    public boolean statusApi(final String name) {
        final Api api = FluentIterable.from(apis).firstMatch(new Predicate<Api>() {
            @Override
            public boolean apply(final Api input) {
                return input.getName().equals(name);
            }
        }).orNull();
        if (api == null) {
            return false;
        }
        return api.isEnabled();
    }

    protected abstract void writeApi(Api api);

    protected abstract Api findApiByName(String name);
}
