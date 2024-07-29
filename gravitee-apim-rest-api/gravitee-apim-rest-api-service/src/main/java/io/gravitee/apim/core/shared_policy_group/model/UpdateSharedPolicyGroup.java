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
package io.gravitee.apim.core.shared_policy_group.model;

import io.gravitee.definition.model.v4.flow.step.Step;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UpdateSharedPolicyGroup {

    /**
     * The shared policy group crossId uniquely identifies a shared policy group across environments.
     * Apis promoted between environments will share the same crossId. Gateways will use this crossId to identify the shared policy group
     */
    private String crossId;
    /**
     * The shared policy group name.
     */
    private String name;
    /**
     * the shared policy group description.
     */
    private String description;
    /**
     * The shared policy group steps
     */
    private List<Step> steps;
}
