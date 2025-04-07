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
import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.nativeapi.NativeFlowImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FlowCrudServiceInMemory implements FlowCrudService, InMemoryAlternative<AbstractFlow> {

    final Map<String, List<FlowV4Impl>> apiFlowsHttpV4 = new HashMap<>();
    final Map<String, List<FlowV4Impl>> planFlowsHttpV4 = new HashMap<>();

    final Map<String, List<NativeFlowImpl>> apiFlowsNativeV4 = new HashMap<>();
    final Map<String, List<NativeFlowImpl>> planFlowsNativeV4 = new HashMap<>();

    final Map<String, List<FlowV2Impl>> apiFlowsV2 = new HashMap<>();
    final Map<String, List<FlowV2Impl>> planFlowsV2 = new HashMap<>();

    @Override
    public List<FlowV4Impl> savePlanFlows(String planId, List<FlowV4Impl> flows) {
        planFlowsHttpV4.put(planId, flows);
        return flows;
    }

    @Override
    public List<FlowV4Impl> saveApiFlows(String apiId, List<FlowV4Impl> flows) {
        apiFlowsHttpV4.put(apiId, flows);
        return flows;
    }

    @Override
    public List<FlowV4Impl> getApiV4Flows(String apiId) {
        return apiFlowsHttpV4.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<FlowV4Impl> getPlanV4Flows(String planId) {
        return planFlowsHttpV4.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public List<FlowV2Impl> getApiV2Flows(String apiId) {
        return apiFlowsV2.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<FlowV2Impl> getPlanV2Flows(String planId) {
        return planFlowsV2.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public List<NativeFlowImpl> saveNativeApiFlows(String apiId, List<NativeFlowImpl> flows) {
        apiFlowsNativeV4.put(apiId, flows);
        return flows;
    }

    @Override
    public List<NativeFlowImpl> saveNativePlanFlows(String planId, List<NativeFlowImpl> flows) {
        planFlowsNativeV4.put(planId, flows);
        return flows;
    }

    @Override
    public List<NativeFlowImpl> getNativeApiFlows(String apiId) {
        return apiFlowsNativeV4.getOrDefault(apiId, new ArrayList<>());
    }

    @Override
    public List<NativeFlowImpl> getNativePlanFlows(String planId) {
        return planFlowsNativeV4.getOrDefault(planId, new ArrayList<>());
    }

    @Override
    public void initWith(List<AbstractFlow> items) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        planFlowsHttpV4.clear();
    }

    @Override
    public List<AbstractFlow> storage() {
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

        var flows = new ArrayList<AbstractFlow>();
        flows.addAll(v4Flows);
        flows.addAll(nativeFlows);
        return flows;
    }

    public List<FlowV2Impl> savePlanFlowsV2(String planId, List<FlowV2Impl> flows) {
        planFlowsV2.put(planId, flows);
        return flows;
    }

    public List<FlowV2Impl> saveApiFlowsV2(String apiId, List<FlowV2Impl> flows) {
        apiFlowsV2.put(apiId, flows);
        return flows;
    }
}
