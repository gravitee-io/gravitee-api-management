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
package io.gravitee.apim.core.api.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import java.util.List;
import java.util.Map;

public class ApiCreatedWithErrorException extends ValidationDomainException {

    public ApiCreatedWithErrorException(String apiId, List<String> errors) {
        super("API created with error:\n" + String.join("\n", errors), Map.of("apiId", apiId));
    }

    public static class ApiCreatedWithErrorExceptionBuilder {

        private String apiId;
        private final List<String> errors = new java.util.ArrayList<>();

        public ApiCreatedWithErrorExceptionBuilder apiId(String apiId) {
            this.apiId = apiId;
            return this;
        }

        public ApiCreatedWithErrorExceptionBuilder addError(String scope, Exception e) {
            this.errors.add("- (" + scope + ") " + e.getMessage());
            return this;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public ApiCreatedWithErrorException build() {
            return new ApiCreatedWithErrorException(apiId, errors);
        }
    }
}
