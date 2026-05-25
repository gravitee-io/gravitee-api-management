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
package io.gravitee.definition.model.v4.edge;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.listener.AbstractListener;
import io.gravitee.definition.model.v4.listener.entrypoint.AbstractEntrypoint;
import io.gravitee.definition.model.v4.plan.Plan;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class EdgeApi extends AbstractApi {

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    private ApiType type = ApiType.EDGE;

    private String gatewayUrl;

    private String reactorUrl;

    @NotNull
    private EdgeProxyDefinition proxy;

    private EdgeShadowAiDefinition shadowAi;

    private List<Plan> plans;

    @Override
    @JsonIgnore
    public List<Plugin> getPlugins() {
        return List.of();
    }

    @Override
    @JsonIgnore
    public List<? extends AbstractListener<? extends AbstractEntrypoint>> getListeners() {
        return List.of();
    }
}
