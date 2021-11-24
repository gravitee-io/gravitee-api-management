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
package io.gravitee.gateway.handlers.api.path.impl;

import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Rule;

import java.util.stream.Collectors;

/**
 * A simple path resolver based on context paths definition.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPathResolverImpl extends AbstractPathResolver {

    public ApiPathResolverImpl(Api api) {
        super();

        // Paths may be empty with definition v2
        if (api.getPaths() != null) {
            api.getPaths().forEach((key, value) -> {
                io.gravitee.gateway.handlers.api.path.Path path = new io.gravitee.gateway.handlers.api.path.Path();
                path.setPath(key);

                // Keeping only enabled rules
                path.setRules(value.getRules().stream().filter(Rule::isEnabled).collect(Collectors.toList()));

                register(path);
            });
        }
    }
}
