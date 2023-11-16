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
package inmemory;

import static io.gravitee.rest.api.service.impl.ParameterServiceImpl.SEPARATOR;
import static java.util.Arrays.stream;

import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ParametersQueryServiceInMemory implements ParametersQueryService, InMemoryAlternative<Parameter> {

    final ArrayList<Parameter> parameters = new ArrayList<>();

    @Override
    public Map<Key, List<String>> findAll(ExecutionContext executionContext, List<Key> keys, ParameterReferenceType referenceType) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Predicate<Parameter> predicate = parameter -> keys.contains(Key.findByKey(parameter.getKey()));

        if (!referenceType.equals(ParameterReferenceType.SYSTEM)) {
            predicate = predicate.and(parameter -> referenceType.name().equals(parameter.getReferenceType().name()));
        }

        if (ParameterReferenceType.ORGANIZATION.equals(referenceType)) {
            predicate = predicate.and(parameter -> parameter.getReferenceId().equals(executionContext.getOrganizationId()));
        }

        if (ParameterReferenceType.ENVIRONMENT.equals(referenceType)) {
            predicate = predicate.and(parameter -> parameter.getReferenceId().equals(executionContext.getEnvironmentId()));
        }

        return parameters
            .stream()
            .filter(predicate)
            .collect(
                Collectors.toMap(
                    parameter -> Key.findByKey(parameter.getKey()),
                    parameter -> stream(parameter.getValue().split(SEPARATOR)).collect(Collectors.toList())
                )
            );
    }

    @Override
    public void initWith(List<Parameter> items) {
        parameters.clear();
        parameters.addAll(items);
    }

    @Override
    public void reset() {
        parameters.clear();
    }

    @Override
    public List<Parameter> storage() {
        return Collections.unmodifiableList(parameters);
    }
}
