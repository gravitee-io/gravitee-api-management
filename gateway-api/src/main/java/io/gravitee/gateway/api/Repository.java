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
package io.gravitee.gateway.api;

import java.util.Set;

import io.gravitee.model.Api;

/**
 * The registry interface used to manage {@code Api}.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Repository {

    /**
     * Allows to list all registered APIs.
     * @return the registered APIs
     */
    Set<Api> listAll();

    /**
     * Allows to get a registered API by its name.
     * @param name the name of the API to get
     * @return the registered API
     */
    Api get(String name);

    /**
     * Allows to fetch registered and unregistered APIs from the repository
     * @return the (un)registered APIs
     */
    Set<Api> fetchAll();

    /**
     * Allows to get a registered or unregistered API by its name.
     * @param name the name of the API to get
     * @return the (un)registered API
     */
    Api fetch(String name);

    /**
     * Allows to create a new API
     * @param api the API to create
     * @return {@code true} if created, {@code false} otherwise
     */
    boolean create(Api api);

    /**
     * Allows to update an existing API
     * @param api the API to update
     * @return {@code true} if updated, {@code false} otherwise
     */
    boolean update(Api api);
}
