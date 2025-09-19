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
package io.gravitee.repository.log.v4.model.analytics;

import io.gravitee.definition.model.DefinitionVersion;
import java.util.Collection;
import java.util.List;
import lombok.Builder;

@Builder
public record ResponseStatusQueryCriteria(List<String> apiIds, Long from, Long to, Collection<DefinitionVersion> definitionVersions) {
    public ResponseStatusQueryCriteria(List<String> apiIds, Long from, Long to, Collection<DefinitionVersion> definitionVersions) {
        this.apiIds = apiIds;
        this.from = from;
        this.to = to;
        this.definitionVersions = definitionVersions == null || definitionVersions.isEmpty()
            ? List.of(DefinitionVersion.V4)
            : definitionVersions;
    }

    public ResponseStatusQueryCriteria(List<String> apiIds, Long from, Long to) {
        this(apiIds, from, to, null);
    }
}
