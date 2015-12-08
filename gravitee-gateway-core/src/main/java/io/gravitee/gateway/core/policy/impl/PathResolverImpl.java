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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Path;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.PathResolver;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

/**
 * A simple path resolver based on context paths definition.
 * This implementation doesn't use regex at all.
 *
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PathResolverImpl implements PathResolver {

    @Autowired
    private Api api;

    @Override
    public Path resolve(Request request) {
        Optional<Path> optPath = api.getPaths().entrySet().stream().filter(
                entry -> request.path().startsWith(entry.getKey())).map(Map.Entry::getValue).findFirst();

        return optPath.orElseGet(() -> api.getPaths().values().iterator().next());
    }

    public void setApi(Api api) {
        this.api = api;
    }
}
