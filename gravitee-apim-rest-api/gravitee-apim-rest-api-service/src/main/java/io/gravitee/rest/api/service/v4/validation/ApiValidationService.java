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
package io.gravitee.rest.api.service.v4.validation;

<<<<<<< HEAD
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
=======
>>>>>>> cea4485fb9 (feat(definition): add api definition v4 classes)
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiValidationService {
<<<<<<< HEAD
    void validateAndSanitizeNewApi(
        final ExecutionContext executionContext,
        final NewApiEntity newApiEntity,
        final PrimaryOwnerEntity primaryOwnerEntity
    );
=======
    void validateAndSanitizeNewApi(ExecutionContext executionContext, NewApiEntity newApiEntity);
>>>>>>> cea4485fb9 (feat(definition): add api definition v4 classes)
}
