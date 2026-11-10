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
package io.gravitee.apim.core.api_product.model;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import java.util.List;

/**
 * The flows an API Product runs on every request to its APIs, in order, and the mode they run in.
 *
 * <p>No mode stands for {@code DEFAULT} without {@code matchRequired}, and no list for no flow: products created
 * before product flows existed store neither, and they must read exactly as a product saved with those values.
 */
public record ApiProductFlows(FlowExecution flowExecution, List<Flow> flows) {
    public ApiProductFlows {
        flowExecution = flowExecution != null ? flowExecution : new FlowExecution();
        flows = flows != null ? List.copyOf(flows) : List.of();
    }
}
