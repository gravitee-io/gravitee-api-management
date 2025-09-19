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
package io.gravitee.apim.core.api.domain_service;

import static io.gravitee.apim.core.api.model.Api.ApiLifecycleState.ARCHIVED;
import static io.gravitee.apim.core.api.model.Api.ApiLifecycleState.CREATED;
import static io.gravitee.apim.core.api.model.Api.ApiLifecycleState.DEPRECATED;
import static io.gravitee.apim.core.api.model.Api.ApiLifecycleState.UNPUBLISHED;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.rest.api.service.exceptions.LifecycleStateChangeNotAllowedException;

public class ValidateApiLifecycleService {

    public static Api.ApiLifecycleState validateFederatedApiLifecycleState(
        Api.ApiLifecycleState existingLifecycleState,
        Api.ApiLifecycleState newLifecycleState
    ) {
        // if lifecycle state not provided, return the existing one
        if (newLifecycleState == null) {
            return existingLifecycleState;
        } else if (DEPRECATED == existingLifecycleState) {
            //  Otherwise, we should first check that existingAPI and updateApi have the same lifecycleState and THEN check for deprecation status of the exiting API //  if we don't want a deprecated API to be updated, then we should have a specific check // TODO FCY: because of this, you can't update a deprecated API but the reason is not clear.
            throw new LifecycleStateChangeNotAllowedException(newLifecycleState.name());
        } else if (ARCHIVED == existingLifecycleState) {
            throw new LifecycleStateChangeNotAllowedException(newLifecycleState.name());
        } else if (existingLifecycleState == newLifecycleState) {
            return existingLifecycleState;
        } else if (UNPUBLISHED == existingLifecycleState && CREATED == newLifecycleState) {
            throw new LifecycleStateChangeNotAllowedException(newLifecycleState.name());
        }
        return newLifecycleState;
    }
}
