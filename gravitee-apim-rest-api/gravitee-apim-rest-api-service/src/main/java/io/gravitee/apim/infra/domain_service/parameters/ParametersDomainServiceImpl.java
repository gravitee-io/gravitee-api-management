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
package io.gravitee.apim.infra.domain_service.parameters;

import io.gravitee.apim.core.parameters.domain_service.ParametersDomainService;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParametersDomainServiceImpl implements ParametersDomainService {

    private final ConfigurableEnvironment environment;
    private final ParameterService parameterService;

    @Override
    public Map<Key, String> getSystemParameters(List<Key> keys) {
        return keys
            .stream()
            .map(this::getSystemParameter)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(parameter -> Key.findByKey(parameter.getKey()), Parameter::getValue));
    }

    @Override
    public Map<Key, String> getEnvironmentParameters(ExecutionContext executionContext, List<Key> keys) {
        return parameterService
            .findAll(executionContext, keys, Function.identity(), ParameterReferenceType.ENVIRONMENT)
            .entrySet()
            .stream()
            .filter(es -> !es.getValue().isEmpty())
            .collect(Collectors.toMap(es -> Key.findByKey(es.getKey()), es -> es.getValue().get(0)));
    }

    private Parameter getSystemParameter(Key key) {
        if (environment.containsProperty(key.key())) {
            final Parameter parameter = new Parameter();
            parameter.setKey(key.key());
            parameter.setValue(environment.getProperty(key.key()));
            return parameter;
        }
        return null;
    }
}
