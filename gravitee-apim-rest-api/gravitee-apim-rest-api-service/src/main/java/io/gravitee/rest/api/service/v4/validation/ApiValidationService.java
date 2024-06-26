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
package io.gravitee.rest.api.service.v4.validation;

import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiValidationService {
    void validateAndSanitizeNewApi(
        final ExecutionContext executionContext,
        final NewApiEntity newApiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity
    );

    void validateAndSanitizeUpdateApi(
        final ExecutionContext executionContext,
        final UpdateApiEntity updateApiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity,
        final ApiEntity existingApiEntity
    );

    void validateAndSanitizeImportApiForCreation(
        final ExecutionContext executionContext,
        final ApiEntity apiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity
    );

    boolean canDeploy(ExecutionContext executionContext, String apiId);

    void validateDynamicProperties(Service dynamicProperties);

    List<Resource> validateAndSanitize(List<Resource> resources);
}
