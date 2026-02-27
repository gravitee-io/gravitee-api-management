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

import java.util.List;
import java.util.Objects;
import lombok.*;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    public static final Organization DEFAULT = Organization.builder().id("DEFAULT").hrids(List.of("default")).name("Default").build();

    public enum AuditEvent implements Audit.AuditEvent {
        ORGANIZATION_FLOWS_UPDATED,
    }

    private String id;
    private String cockpitId;
    private List<String> hrids;
    private String name;
    private String description;
    private String flowMode;
}
