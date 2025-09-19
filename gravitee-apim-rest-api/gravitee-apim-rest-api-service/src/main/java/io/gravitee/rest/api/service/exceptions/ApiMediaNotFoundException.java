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
package io.gravitee.rest.api.service.exceptions;

import java.util.Map;

/**
 * @author GraviteeSource Team
 */
public class ApiMediaNotFoundException extends AbstractNotFoundException {

    // NOSONAR

    private final String hash;
    private final String apiId;

    public ApiMediaNotFoundException(String hash, String apiId) {
        this.hash = hash;
        this.apiId = apiId;
    }

    @Override
    public String getMessage() {
        return String.format("No media with hash [%s] could be found for API [%s]", hash, apiId);
    }

    @Override
    public String getTechnicalCode() {
        return "apiMedia.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        return Map.of("hash", hash, "api", apiId);
    }
}
