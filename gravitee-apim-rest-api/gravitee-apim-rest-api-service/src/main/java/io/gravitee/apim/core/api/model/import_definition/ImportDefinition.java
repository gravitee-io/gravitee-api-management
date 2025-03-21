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
package io.gravitee.apim.core.api.model.import_definition;

import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.ApiType;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class ImportDefinition {

    private ApiExport apiExport;

    private Set<ApiMember> members;

    private Set<NewApiMetadata> metadata;

    private List<Page> pages;

    private Set<PlanWithFlows> plans;

    private List<Media> apiMedia;

    public boolean isNative() {
        return apiExport.getType() == ApiType.NATIVE;
    }
}
