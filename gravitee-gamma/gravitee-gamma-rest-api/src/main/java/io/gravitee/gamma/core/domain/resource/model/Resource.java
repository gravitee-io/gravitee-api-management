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
package io.gravitee.gamma.core.domain.resource.model;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.utils.TimeProvider;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Accessors(fluent = true)
public class Resource {

    public enum ReferenceType {
        ENVIRONMENT,
    }

    private String id;
    private String referenceId;
    private ReferenceType referenceType;
    private io.gravitee.definition.model.v4.resource.Resource definition;
    private Instant createdAt;
    private Instant updatedAt;

    public static Resource from(CreateResourceCommand command, AuditInfo audit) {
        Instant now = TimeProvider.instantNow();
        return Resource.builder()
            .id(command.id())
            .referenceId(audit.environmentId())
            .referenceType(ReferenceType.ENVIRONMENT)
            .definition(
                io.gravitee.definition.model.v4.resource.Resource.builder()
                    .name(command.name())
                    .type(command.type())
                    .enabled(command.enabled())
                    .configuration(command.configuration())
                    .build()
            )
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public Resource mergedWith(UpdateResourceCommand command) {
        return this.toBuilder()
            .definition(
                io.gravitee.definition.model.v4.resource.Resource.builder()
                    .name(command.name())
                    .type(command.type())
                    .enabled(command.enabled())
                    .configuration(command.configuration())
                    .build()
            )
            .updatedAt(TimeProvider.instantNow())
            .build();
    }
}
