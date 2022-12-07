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
package io.gravitee.definition.model.v4.listener.entrypoint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import javax.validation.constraints.NotEmpty;
import lombok.*;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(name = "EntrypointV4")
public class Entrypoint implements Serializable {

    @JsonProperty(required = true)
    @NotEmpty
    private String type;

    private Qos qos = Qos.AUTO;

    private Dlq dlq;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String configuration;

    @JsonSetter
    public void setConfiguration(final JsonNode configuration) {
        if (configuration != null) {
            this.configuration = configuration.toString();
        }
    }

    public void setConfiguration(final String configuration) {
        this.configuration = configuration;
    }
}
