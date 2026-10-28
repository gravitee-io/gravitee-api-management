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
package io.gravitee.apim.core.api.model;

import io.gravitee.apim.core.api.exception.InvalidPathMappingsException;
import io.gravitee.definition.model.PathMapping;
import java.util.Collection;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public final class PathMappingTemplates {

    private PathMappingTemplates() {}

    public static boolean isCompilable(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        try {
            PathMapping.buildPattern(template);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public static void validate(Collection<String> templates) {
        if (templates == null) {
            return;
        }
        var offending = templates
            .stream()
            .filter(t -> t == null || t.isBlank() || !isCompilable(t))
            .collect(Collectors.joining(", "));

        if (!offending.isEmpty()) {
            throw new InvalidPathMappingsException("Path mappings contain invalid templates: [" + offending + "]", offending);
        }
    }
}
