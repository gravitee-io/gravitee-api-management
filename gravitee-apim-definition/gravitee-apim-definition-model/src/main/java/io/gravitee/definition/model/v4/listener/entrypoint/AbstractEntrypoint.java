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
package io.gravitee.definition.model.v4.listener.entrypoint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import io.gravitee.definition.model.AbstractTypeIdResolver;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonTypeIdResolver(AbstractEntrypoint.EntrypointResolver.class)
public abstract class AbstractEntrypoint implements Serializable {

    @JsonProperty(required = true)
    @NotEmpty
    private String type;

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

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return List.of(new Plugin("entrypoint-connector", type));
    }

    public static class EntrypointResolver extends AbstractTypeIdResolver {

        @Override
        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            if (id.startsWith("native-")) {
                return context.getTypeFactory().constructType(new TypeReference<NativeEntrypoint>() {});
            } else {
                return context.getTypeFactory().constructType(new TypeReference<Entrypoint>() {});
            }
        }
    }
}
