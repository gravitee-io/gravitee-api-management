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
package io.gravitee.rest.api.management.v2.rest.resource.group.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupSearchParams {

    @Valid
    @NotNull
    @JsonProperty("ids")
    private Set<String> ids;

    @Valid
    @NotNull
    @JsonProperty("paginated")
    private boolean paginated;

    /**
     * Constructor with only ids parameter, setting paginated to true by default.
     * @param ids The set of group IDs to search for
     */
    public GroupSearchParams(Set<String> ids) {
        this.ids = ids;
        this.paginated = true;
    }
}
