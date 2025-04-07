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
package io.gravitee.definition.model.v4.flow;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.Selector;
import io.gravitee.definition.model.v4.flow.selector.SelectorType;
import io.gravitee.definition.model.v4.flow.step.StepV4;
import java.util.List;
import java.util.Optional;

@JsonDeserialize(as = FlowV4Impl.class)
public interface FlowV4 extends Flow {
    List<Selector> getSelectors();
    Optional<Selector> selectorByType(SelectorType type);

    List<StepV4> getRequest();
    List<StepV4> getResponse();
    List<StepV4> getSubscribe();
    List<StepV4> getPublish();
}
