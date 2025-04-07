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
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.definition.model.v4.flow.FlowV4;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FlowCrudServiceInMemory implements FlowCrudService, InMemoryAlternative<Flow> {

    final Map<String, List<FlowV4>> apiFlowsHttpV4 = new HashMap<>();
    final Map<String, List<FlowV4>> planFlowsHttpV4 = new HashMap<>();

    final Map<String, List<NativeFlow>> apiFlowsNativeV4 = new HashMap<>();
    final Map<String, List<NativeFlow>> planFlowsNativeV4 = new HashMap<>();

    final Map<String, List<FlowV2>> apiFlowsV2 = new HashMap<>();
    final Map<String, List<FlowV2>> planFlowsV2 = new HashMap<>();

    @Override
    public List<FlowV4> savePlanFlows(String planId, List<FlowV4> flows) {
        planFlowsHttpV4.put(planId, flows);
        return flows;
    }

    @Override
    public List<FlowV4> saveApiFlows(String apiId, List<FlowV4> flows) {
        apiFlowsHttpV4.put(apiId, flows);
        return flows;
    }

    @Override
    public List<FlowV4> getApiV4Flows(String apiId) {
        return apiFlowsHttpV4.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<FlowV4> getPlanV4Flows(String planId) {
        return planFlowsHttpV4.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public List<FlowV2> getApiV2Flows(String apiId) {
        return apiFlowsV2.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<FlowV2> getPlanV2Flows(String planId) {
        return planFlowsV2.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public List<NativeFlow> saveNativeApiFlows(String apiId, List<NativeFlow> flows) {
        apiFlowsNativeV4.put(apiId, flows);
        return flows;
    }

    @Override
    public List<NativeFlow> saveNativePlanFlows(String planId, List<NativeFlow> flows) {
        planFlowsNativeV4.put(planId, flows);
        return flows;
    }

    @Override
    public List<NativeFlow> getNativeApiFlows(String apiId) {
        return apiFlowsNativeV4.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<NativeFlow> getNativePlanFlows(String planId) {
        return planFlowsNativeV4.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public void initWith(List<Flow> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        planFlowsHttpV4.clear();
    }

    @Override
    public List<Flow> storage() {
        var v4Flows = Stream
            .concat(planFlowsHttpV4.values().stream(), apiFlowsHttpV4.values().stream())
            .reduce(
                new ArrayList<>(),
                (acc, flow) -> {
                    acc.addAll(flow);
                    return acc;
                }
            );
        var nativeFlows = Stream
            .concat(planFlowsNativeV4.values().stream(), apiFlowsNativeV4.values().stream())
            .reduce(
                new ArrayList<>(),
                (acc, flow) -> {
                    acc.addAll(flow);
                    return acc;
                }
            );

        var flows = new ArrayList<Flow>();
        flows.addAll(v4Flows);
        flows.addAll(nativeFlows);
        return flows;
    }

    public List<FlowV2> savePlanFlowsV2(String planId, List<FlowV2> flows) {
        planFlowsV2.put(planId, flows);
        return flows;
    }

    public List<FlowV2> saveApiFlowsV2(String apiId, List<FlowV2> flows) {
        apiFlowsV2.put(apiId, flows);
        return flows;
    }
}
