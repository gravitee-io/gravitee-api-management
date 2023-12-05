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
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PlanCRD {

    @NotNull
    @NotEmpty
    private String id;

    private String name;

    private String description;

    private PlanSecurity security;

    private List<String> characteristics;

    private String commentMessage;

    private boolean commentRequired;

    private List<String> excludedGroups;

    private String generalConditions;

    private int order;

    private OffsetDateTime publishedAt;

    private String selectionRule;

    @NotNull
    private PlanStatus status;

    private List<String> tags;

    @NotNull
    private PlanType type;

    private PlanValidation validation;

    private List<FlowV4> flows;

    @NotNull
    private PlanMode mode;
}
