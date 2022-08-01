/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.definition.model.v4.endpointgroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Endpoint implements Serializable {

    private static final int DEFAULT_WEIGHT = 1;

    private String name;

    @JsonProperty(required = true)
    private String type;

    private int weight = DEFAULT_WEIGHT;

    private boolean inheritConfig;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String configuration;

    private EndpointServices services;
}
