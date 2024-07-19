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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ImportPageEntity {

    @NotNull
    private PageType type;

    private boolean published;

    private Visibility visibility = Visibility.PUBLIC;

    private String lastContributor;
    private PageSourceEntity source;
    private Map<String, String> configuration;

    @JsonProperty("excluded_groups")
    private List<String> excludedGroups;

    private boolean excludedAccessControls;
    private Set<AccessControlEntity> accessControls;
}
