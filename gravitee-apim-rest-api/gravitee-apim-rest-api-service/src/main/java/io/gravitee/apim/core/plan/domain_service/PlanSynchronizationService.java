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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.v4.flow.FlowV4;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.List;

public interface PlanSynchronizationService {
    boolean checkSynchronized(Plan oldPlan, List<FlowV4> oldFlows, Plan newPlan, List<FlowV4> newFlows);
    boolean checkNativePlanSynchronized(Plan oldPlan, List<NativeFlow> oldFlows, Plan newPlan, List<NativeFlow> newFlows);
}
