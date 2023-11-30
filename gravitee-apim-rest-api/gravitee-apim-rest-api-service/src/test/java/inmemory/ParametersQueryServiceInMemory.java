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

import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.rest.api.model.parameters.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParametersQueryServiceInMemory implements ParametersQueryService, InMemoryAlternative<Parameter> {

    final ArrayList<Parameter> parameters = new ArrayList<>();

    @Override
    public boolean findAsBoolean(Key key, ParameterContext context) {
        return parameters
            .stream()
            .filter(parameter -> key.equals(Key.findByKey(parameter.getKey())))
            .map(parameter -> Boolean.valueOf(parameter.getValue()))
            .findFirst()
            .orElse(false);
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
