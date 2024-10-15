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
package io.gravitee.apim.core.api.model;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class AbstractNewApi {

    protected String name;

    protected String apiVersion;

    @Builder.Default
    protected DefinitionVersion definitionVersion = DefinitionVersion.V4;

    protected ApiType type;

    protected String description;

    @Builder.Default
    protected Set<String> tags = Set.of();

    @Builder.Default
    protected Set<String> groups = Set.of();

    /**
     * @return An instance of {@link Api.ApiBuilder} based on the current state of this NewApi.
     */
    public Api.ApiBuilder<?, ?> toApiBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return Api
            .builder()
            .name(name)
            .version(apiVersion)
            .type(type)
            .definitionVersion(definitionVersion)
            .description(description)
            .groups(groups);
    }
}
