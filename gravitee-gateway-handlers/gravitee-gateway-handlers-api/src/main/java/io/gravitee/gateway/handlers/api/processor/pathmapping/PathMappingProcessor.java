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
package io.gravitee.gateway.handlers.api.processor.pathmapping;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.core.processor.AbstractProcessor;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PathMappingProcessor extends AbstractProcessor<ExecutionContext> {

    private final Map<String, Pattern> mapping;

    public PathMappingProcessor(final Map<String, Pattern> mapping) {
        this.mapping = mapping;
    }

    @Override
    public void handle(ExecutionContext result) {
        String path = result.request().pathInfo();
        if (path.length() == 0 || path.charAt(path.length() - 1) != '/') {
            path += '/';
        }

        String finalPath = path;
        mapping.entrySet().stream()
                .filter(regexMappedPath -> regexMappedPath.getValue().matcher(finalPath).matches())
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(resolvedMappedPath -> result.request().metrics().setMappedPath(resolvedMappedPath));

        next.handle(null);
    }
}
