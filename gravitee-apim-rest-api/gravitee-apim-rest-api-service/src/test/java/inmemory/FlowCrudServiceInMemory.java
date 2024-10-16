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
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FlowCrudServiceInMemory implements FlowCrudService, InMemoryAlternative<AbstractFlow> {

    final Map<String, List<Flow>> apiFlowsV4 = new HashMap<>();
    final Map<String, List<Flow>> planFlowsV4 = new HashMap<>();

    final Map<String, List<NativeFlow>> apiFlowsNative = new HashMap<>();

    final Map<String, List<io.gravitee.definition.model.flow.Flow>> apiFlowsV2 = new HashMap<>();
    final Map<String, List<io.gravitee.definition.model.flow.Flow>> planFlowsV2 = new HashMap<>();

    @Override
    public List<Flow> savePlanFlows(String planId, List<Flow> flows) {
        planFlowsV4.put(planId, flows);
        return flows;
    }

    @Override
    public List<Flow> saveApiFlows(String apiId, List<Flow> flows) {
        apiFlowsV4.put(apiId, flows);
        return flows;
    }

    @Override
    public List<Flow> getApiV4Flows(String apiId) {
        return apiFlowsV4.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<Flow> getPlanV4Flows(String planId) {
        return planFlowsV4.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public List<io.gravitee.definition.model.flow.Flow> getApiV2Flows(String apiId) {
        return apiFlowsV2.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<io.gravitee.definition.model.flow.Flow> getPlanV2Flows(String planId) {
        return planFlowsV2.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public List<NativeFlow> saveNativeApiFlows(String apiId, List<NativeFlow> flows) {
        apiFlowsNative.put(apiId, flows);
        return flows;
    }

    @Override
    public void initWith(List<AbstractFlow> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        planFlowsV4.clear();
    }

    @Override
    public List<AbstractFlow> storage() {
        var v4Flows = Stream
            .concat(planFlowsV4.values().stream(), apiFlowsV4.values().stream())
            .reduce(
                new ArrayList<>(),
                (acc, flow) -> {
                    acc.addAll(flow);
                    return acc;
                }
            );
        var nativeFlows = apiFlowsNative
            .values()
            .stream()
            .reduce(
                new ArrayList<>(),
                (acc, flow) -> {
                    acc.addAll(flow);
                    return acc;
                }
            );

        var flows = new ArrayList<AbstractFlow>();
        flows.addAll(v4Flows);
        flows.addAll(nativeFlows);
        return flows;
    }

    public List<io.gravitee.definition.model.flow.Flow> savePlanFlowsV2(String planId, List<io.gravitee.definition.model.flow.Flow> flows) {
        planFlowsV2.put(planId, flows);
        return flows;
    }

    public List<io.gravitee.definition.model.flow.Flow> saveApiFlowsV2(String apiId, List<io.gravitee.definition.model.flow.Flow> flows) {
        apiFlowsV2.put(apiId, flows);
        return flows;
    }
}
