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
package io.gravitee.apim.infra.domain_service.user;

import io.gravitee.apim.core.user.service_provider.UserRegistrationEnabledService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.exceptions.UserRegistrationUnavailableException;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationEnabledServiceImpl implements UserRegistrationEnabledService {

    private final ParameterService parameterService;

    public UserRegistrationEnabledServiceImpl(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @Override
    public void checkEnabled(ExecutionContext executionContext) {
        var currentContext = executionContext.getReferenceContext();
        boolean userCreationEnabled = switch (currentContext.getReferenceType()) {
            case ORGANIZATION -> parameterService.findAsBoolean(
                executionContext,
                Key.CONSOLE_USERCREATION_ENABLED,
                currentContext.getReferenceId(),
                ParameterReferenceType.ORGANIZATION
            );
            case ENVIRONMENT -> parameterService.findAsBoolean(
                executionContext,
                Key.PORTAL_USERCREATION_ENABLED,
                currentContext.getReferenceId(),
                ParameterReferenceType.ENVIRONMENT
            );
        };
        if (!userCreationEnabled) {
            throw new UserRegistrationUnavailableException();
        }
    }
}
