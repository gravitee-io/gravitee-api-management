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
package io.gravitee.definition.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.http.HttpMethod;
import io.swagger.v3.oas.annotations.Hidden;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
public class Rule extends HashMap<String, Object> /* This is to generate the correct Open-API definition*/ implements Serializable {

    @JsonProperty("methods")
    @Builder.Default
    private Set<HttpMethod> methods = EnumSet.allOf(HttpMethod.class);

    @JsonIgnore
    private Policy policy;

    @JsonProperty("description")
    private String description;

    @JsonProperty("enabled")
    @Builder.Default
    private boolean enabled = true;

    @Hidden
    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }
}
