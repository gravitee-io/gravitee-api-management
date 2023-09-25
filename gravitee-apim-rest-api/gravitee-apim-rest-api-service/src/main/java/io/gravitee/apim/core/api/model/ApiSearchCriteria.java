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

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiSearchCriteria {

    private List<String> ids;
    private List<String> groups;
    private String category;
    private String label;
    private Api.LifecycleState state;
    private Api.Visibility visibility;
    private String version;
    private String name;
    private List<Api.ApiLifecycleState> lifecycleStates;
    private String environmentId;
    private List<String> environments;
    private String crossId;
    private List<Api.DefinitionVersion> definitionVersion;
}
