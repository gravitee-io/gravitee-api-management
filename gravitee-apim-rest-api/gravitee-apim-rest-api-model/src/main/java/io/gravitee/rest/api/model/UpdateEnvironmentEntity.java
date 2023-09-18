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
package io.gravitee.rest.api.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UpdateEnvironmentEntity {

    private String cockpitId;

    private List<String> hrids;

    @NotNull
    @Size(min = 1)
    private String name;

    private String description;

    public UpdateEnvironmentEntity(EnvironmentEntity environment) {
        this.cockpitId = environment.getCockpitId();
        this.hrids = environment.getHrids();
        this.name = environment.getName();
        this.description = environment.getDescription();
    }
}
