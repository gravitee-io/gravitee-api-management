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

import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParametersQueryServiceImpl implements ParametersQueryService {

    private final ParameterService parameterService;

    @Override
    public Map<Key, List<String>> findAll(ExecutionContext executionContext, List<Key> keys, ParameterReferenceType referenceType) {
        var parameters = this.parameterService.findAll(executionContext, keys, referenceType);
        if (null != parameters) {
            return parameters.entrySet().stream().collect(Collectors.toMap(e -> Key.findByKey(e.getKey()), Map.Entry::getValue));
        }
        return null;
    }
}
