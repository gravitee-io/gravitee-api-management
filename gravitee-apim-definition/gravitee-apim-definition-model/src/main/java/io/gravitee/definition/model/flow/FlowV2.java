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
package io.gravitee.definition.model.flow;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.ConditionSupplier;
import java.util.List;
import java.util.Set;

public interface FlowV2 extends Flow, ConditionSupplier {
    String getPath();
    Operator getOperator();
    PathOperator getPathOperator();
    Set<HttpMethod> getMethods();

    List<StepV2> getPre();
    List<StepV2> getPost();

    List<Consumer> getConsumers();
    FlowStage getStage();
}
