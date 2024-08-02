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

import io.gravitee.apim.core.parameters.domain_service.ParametersDomainService;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ParametersDomainServiceInMemory implements ParametersDomainService, InMemoryAlternative<Parameter> {

    final ArrayList<Parameter> parameters = new ArrayList<>();

    @Override
    public Map<Key, String> getSystemParameters(List<Key> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        return parameters
            .stream()
            .filter(parameter -> keys.contains(Key.findByKey(parameter.getKey())))
            .collect(Collectors.toMap(parameter -> Key.findByKey(parameter.getKey()), Parameter::getValue));
    }

    @Override
    public Map<Key, String> getEnvironmentParameters(ExecutionContext executionContext, List<Key> keys) {
        if (keys.isEmpty()) {
            return Collections.emptyMap();
        }

        return parameters
            .stream()
            .filter(parameter ->
                keys.contains(Key.findByKey(parameter.getKey())) &&
                Objects.equals(executionContext.getEnvironmentId(), parameter.getReferenceId()) &&
                ParameterReferenceType.ENVIRONMENT.equals(parameter.getReferenceType())
            )
            .collect(Collectors.toMap(parameter -> Key.findByKey(parameter.getKey()), Parameter::getValue));
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
