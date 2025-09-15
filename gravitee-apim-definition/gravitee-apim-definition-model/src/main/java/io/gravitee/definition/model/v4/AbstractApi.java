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
package io.gravitee.definition.model.v4;

import static io.gravitee.definition.model.v4.AbstractApi.MESSAGE_LABEL;
import static io.gravitee.definition.model.v4.AbstractApi.NATIVE_LABEL;
import static io.gravitee.definition.model.v4.AbstractApi.PROXY_LABEL;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.ApiDefinition;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    // fill 'type' field when creating instance of the object
    visible = true,
    // Fallback to Api definition if no type can be found in the definition string
    defaultImpl = Api.class
)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = Api.class, name = PROXY_LABEL),
        @JsonSubTypes.Type(value = Api.class, name = MESSAGE_LABEL),
        @JsonSubTypes.Type(value = NativeApi.class, name = NATIVE_LABEL),
    }
)
public abstract class AbstractApi implements Serializable, ApiDefinition {

    public static final String PROXY_LABEL = "proxy";
    public static final String MESSAGE_LABEL = "message";
    public static final String NATIVE_LABEL = "native";

    @JsonProperty(required = true)
    @NotBlank
    protected String id;

    @JsonProperty(required = true)
    @NotBlank
    protected String name;

    @JsonProperty(required = true)
    @NotBlank
    protected ApiType type;

    @JsonProperty(required = true)
    @NotBlank
    protected String apiVersion;

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    protected DefinitionVersion definitionVersion = DefinitionVersion.V4;

    protected Set<@NotBlank String> tags;

    protected List<Property> properties;

    protected List<Resource> resources;

    public abstract List<Plugin> getPlugins();

    public abstract List<? extends AbstractListener<? extends AbstractEntrypoint>> getListeners();
}
