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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import javax.inject.Inject;

/**
 * A resource that manages API keys.
 *
 * @author GraviteeSource Team
 */
public class AbstractApiKeyResource extends AbstractResource {

    @Inject
    private ApplicationService applicationService;

    protected void checkApplicationUsesSharedApiKey(ApplicationEntity applicationEntity) {
        checkApplicationUsesSharedApiKey(applicationEntity, true);
    }

    protected void checkApplicationDoesntUseSharedApiKey(ApplicationEntity applicationEntity) {
        checkApplicationUsesSharedApiKey(applicationEntity, false);
    }

    protected void checkApplicationDoesntUseSharedApiKey(ExecutionContext executionContext, String applicationId) {
        ApplicationEntity applicationEntity = applicationService.findById(executionContext, applicationId);
        if (applicationEntity == null) {
            throw new ApplicationNotFoundException(applicationId);
        }
        checkApplicationDoesntUseSharedApiKey(applicationEntity);
    }

    private void checkApplicationUsesSharedApiKey(ApplicationEntity applicationEntity, boolean usesSharedApiKey) {
        if (applicationEntity.hasApiKeySharedMode() != usesSharedApiKey) {
            throw new InvalidApplicationApiKeyModeException(
                String.format(
                    "Invalid operation for api key mode [%s] of application [%s]",
                    applicationEntity.getApiKeyMode(),
                    applicationEntity.getId()
                )
            );
        }
    }
}
