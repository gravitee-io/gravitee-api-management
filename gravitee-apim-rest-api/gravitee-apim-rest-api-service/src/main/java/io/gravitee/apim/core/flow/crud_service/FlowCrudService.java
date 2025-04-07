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
package io.gravitee.apim.core.flow.crud_service;

import io.gravitee.definition.model.flow.FlowV2Impl;
import io.gravitee.definition.model.v4.flow.FlowV4Impl;
import io.gravitee.definition.model.v4.nativeapi.NativeFlowImpl;
import java.util.List;

public interface FlowCrudService {
    List<FlowV4Impl> savePlanFlows(String planId, List<FlowV4Impl> flows);

    List<FlowV4Impl> saveApiFlows(String apiId, List<FlowV4Impl> flows);

    List<FlowV4Impl> getApiV4Flows(String apiId);

    List<FlowV4Impl> getPlanV4Flows(String planId);

    List<FlowV2Impl> getApiV2Flows(String apiId);

    List<FlowV2Impl> getPlanV2Flows(String planId);

    //    Native APIs
    List<NativeFlowImpl> saveNativeApiFlows(String apiId, List<NativeFlowImpl> flows);

    List<NativeFlowImpl> saveNativePlanFlows(String planId, List<NativeFlowImpl> flows);

    List<NativeFlowImpl> getNativeApiFlows(String apiId);

    List<NativeFlowImpl> getNativePlanFlows(String planId);
}
