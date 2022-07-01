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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.DuplicateApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;

public interface ApiDuplicatorService {
    ApiEntity createWithImportedDefinition(ExecutionContext executionContext, Object apiDefinitionOrURL);

    ApiEntity duplicate(ExecutionContext executionContext, ApiEntity apiEntity, DuplicateApiEntity duplicateApiEntity);

    default ApiEntity updateWithImportedDefinition(ExecutionContext executionContext, Object apiDefinitionOrURL) {
        return updateWithImportedDefinition(executionContext, null, apiDefinitionOrURL);
    }

    ApiEntity updateWithImportedDefinition(ExecutionContext executionContext, String apiId, Object apiDefinitionOrURL);
}
