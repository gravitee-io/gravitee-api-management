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

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.v4.flow.Flow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowCrudServiceInMemory implements FlowCrudService, InMemoryAlternative<Flow> {

    final Map<String, List<Flow>> planFlows = new HashMap<>();

    @Override
    public List<Flow> savePlanFlows(String planId, List<Flow> flows) {
        planFlows.put(planId, flows);
        return flows;
    }

    @Override
    public void initWith(List<Flow> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        planFlows.clear();
    }

    @Override
    public List<Flow> storage() {
        return Collections.unmodifiableList(
            planFlows
                .values()
                .stream()
                .reduce(
                    new ArrayList<>(),
                    (acc, flows) -> {
                        acc.addAll(flows);
                        return acc;
                    }
                )
        );
    }
}
