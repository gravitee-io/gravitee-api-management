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
package io.gravitee.rest.api.management.v2.rest.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
public class SharedPolicyGroupCRD {

    private String hrid;

    @NotNull
    @NotEmpty
    private String crossId;

    @NotNull
    @NotEmpty
    private String name;

    private String description;
    private String prerequisiteMessage;

    @NotNull
    private ApiType apiType;

    @NotNull
    private FlowPhase phase;

    private List<StepV4> steps;

    // Only for update
    private String sharedPolicyGroupId;
}
