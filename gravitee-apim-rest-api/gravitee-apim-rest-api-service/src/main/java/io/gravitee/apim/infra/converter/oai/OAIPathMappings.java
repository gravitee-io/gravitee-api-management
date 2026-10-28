/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.infra.converter.oai;

import io.gravitee.apim.core.api.model.PathMappingTemplates;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import lombok.CustomLog;

@CustomLog
public final class OAIPathMappings {

    private static final Pattern PATH_PARAMS_PATTERN = Pattern.compile("\\{(.[^/\\}]*)\\}");

    private OAIPathMappings() {}

    public static List<String> from(OpenAPI spec) {
        if (spec == null || spec.getPaths() == null || spec.getPaths().isEmpty()) {
            return List.of();
        }
        var result = new LinkedHashSet<String>();
        for (var entry : spec.getPaths().entrySet()) {
            var template = PATH_PARAMS_PATTERN.matcher(entry.getKey()).replaceAll(":$1");
            if (PathMappingTemplates.isCompilable(template)) {
                result.add(template);
            } else {
                log.info("Skipping uncompilable OpenAPI path mapping template [{}]", template);
            }
        }
        return new ArrayList<>(result);
    }
}
