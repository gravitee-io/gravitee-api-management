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
package io.gravitee.gateway.core.definition.validator;

import io.gravitee.gateway.core.definition.Api;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PathValidator implements Validator {

    private static final String CONTEXT_PATH_PATTERN = "^\\\\/([a-zA-Z0-9_-]+\\\\/?+)++";

    @Override
    public void validate(Api definition) throws ValidationException {

        definition.getPaths().entrySet().stream().filter(path -> path.getValue().getMethods() == null).forEach(path -> {
            if (! path.getKey().matches(CONTEXT_PATH_PATTERN)) {
                throw new ValidationException("A path definition is not valid and must start with '/'");
            }
            if (path.getValue().getMethods() == null || path.getValue().getMethods().isEmpty()) {
                throw new ValidationException("A path definition must have at least, one declaration of HTTP methods");
            }
        });
    }
}
