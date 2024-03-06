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
package io.gravitee.apim.infra.query_service.parameters;

import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.springframework.stereotype.Service;

@Service
public class ParameterQueryServiceLegacyWrapper implements ParametersQueryService {

    private final ParameterService parameterService;

    public ParameterQueryServiceLegacyWrapper(ParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @Override
    public boolean findAsBoolean(Key key, ParameterContext context) {
        return parameterService.findAsBoolean(
            new ExecutionContext(context.organizationId(), context.environmentId()),
            key,
            context.referenceType()
        );
    }

    @Override
    public String findAsString(Key key, ParameterContext context) {
        return parameterService.find(new ExecutionContext(context.organizationId(), context.environmentId()), key, context.referenceType());
    }
}
