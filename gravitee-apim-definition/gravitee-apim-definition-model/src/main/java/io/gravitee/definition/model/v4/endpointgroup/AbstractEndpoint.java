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
package io.gravitee.definition.model.v4.endpointgroup;

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
import io.gravitee.definition.model.v4.nativeapi.NativeEndpoint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonTypeIdResolver(AbstractEndpoint.EndpointResolver.class)
public class AbstractEndpoint implements Serializable {

    private static final long serialVersionUID = 7139083731513897591L;
    private static final int DEFAULT_WEIGHT = 1;

    @NotBlank
    private String name;

    @JsonProperty(required = true)
    @NotBlank
    private String type;

    private boolean secondary;

    private List<String> tenants;

    @Builder.Default
    private int weight = DEFAULT_WEIGHT;

    private boolean inheritConfiguration;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String configuration;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String sharedConfigurationOverride;

    @JsonSetter
    public void setConfiguration(final JsonNode configuration) {
        if (configuration != null) {
            this.configuration = configuration.toString();
        }
    }

    public void setConfiguration(final String configuration) {
        this.configuration = configuration;
    }

    @JsonSetter
    public void setSharedConfigurationOverride(final JsonNode overriddenSharedConfiguration) {
        if (overriddenSharedConfiguration != null) {
            this.sharedConfigurationOverride = overriddenSharedConfiguration.toString();
        }
    }

    public void setSharedConfigurationOverride(final String overriddenSharedConfiguration) {
        this.sharedConfigurationOverride = overriddenSharedConfiguration;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return List.of(new Plugin("endpoint-connector", type));
    }

    public static class EndpointResolver extends AbstractTypeIdResolver {

        @Override
        public JavaType typeFromId(DatabindContext context, String id) throws IOException {
            if (id.startsWith("native-")) {
                return context.getTypeFactory().constructType(new TypeReference<NativeEndpoint>() {});
            } else {
                return context.getTypeFactory().constructType(new TypeReference<Endpoint>() {});
            }
        }
    }
}
