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
package io.gravitee.repository.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationMetadata {

    private AutomationTargetReferenceType referenceType;
    private String referenceId;
    private String name;
    private String location;
    private Integer order;

    /**
     * Copy of this metadata for attaching to a {@link PortalNavigationItem}, with {@code name} and
     * {@code order} dropped: those already live natively on the nav item ({@code title}/{@code order}),
     * so the attached copy only needs to carry {@code referenceType}/{@code referenceId}/{@code location}.
     */
    public AutomationMetadata trimmedForNavItem() {
        return AutomationMetadata.builder().referenceType(referenceType).referenceId(referenceId).location(location).build();
    }
}
