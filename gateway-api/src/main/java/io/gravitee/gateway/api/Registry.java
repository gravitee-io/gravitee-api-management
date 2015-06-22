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

import io.gravitee.model.Api;

import java.util.Set;

/**
 * The registry interface used to manage {@code Api}.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface Registry {

    Set<Api> listAll();
    Api get(String name);
    boolean create(Api api);

    // Following methods should not be part of the registry
    // but more in a service layer / manager
    //TODO: remove these methods and use ApiService instead.
    boolean startApi(String name);
    boolean stopApi(String name);
    boolean statusApi(String name);
    boolean reloadApi(String name);
    boolean reloadAll();
}
