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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.NewApiEntity;
import io.gravitee.management.service.SwaggerService;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SwaggerServiceImpl implements SwaggerService {

    public NewApiEntity prepare(String descriptor) {
        Swagger swagger = new SwaggerParser().parse(descriptor);

        NewApiEntity apiEntity = new NewApiEntity();

        apiEntity.setName(swagger.getInfo().getTitle());
        apiEntity.setDescription(swagger.getInfo().getDescription());
        apiEntity.setVersion(swagger.getInfo().getVersion());
        apiEntity.setEndpoint(swagger.getSchemes().iterator().next().toValue() + "://" + swagger.getHost() + swagger.getBasePath());
        apiEntity.setPaths(new ArrayList<>(
                swagger.getPaths().keySet()
                        .stream()
                        .map(path -> path.replaceAll("\\{(.[^/]*)\\}", ":$1"))
                        .collect(Collectors.toList())));

        return apiEntity;
    }
}
