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

import io.gravitee.definition.model.flow.FlowV2;
import io.gravitee.definition.model.v4.flow.FlowV4;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.List;

public interface FlowCrudService {
    List<FlowV4> savePlanFlows(String planId, List<FlowV4> flows);

    List<FlowV4> saveApiFlows(String apiId, List<FlowV4> flows);

    List<FlowV4> getApiV4Flows(String apiId);

    List<FlowV4> getPlanV4Flows(String planId);

    List<FlowV2> getApiV2Flows(String apiId);

    List<FlowV2> getPlanV2Flows(String planId);

    //    Native APIs
    List<NativeFlow> saveNativeApiFlows(String apiId, List<NativeFlow> flows);

    List<NativeFlow> saveNativePlanFlows(String planId, List<NativeFlow> flows);

    List<NativeFlow> getNativeApiFlows(String apiId);

    List<NativeFlow> getNativePlanFlows(String planId);
}
