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
package io.gravitee.definition.model.v4.nativeapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.resource.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
@ToString
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class NativeApi extends AbstractApi {

    @JsonProperty(required = true)
    @NotNull
    @Builder.Default
    private ApiType type = ApiType.NATIVE;

    @JsonProperty(required = true)
    @NotEmpty
    private List<@NotNull NativeListener> listeners;

    @JsonProperty(required = true)
    @NotEmpty
    private List<NativeEndpointGroup> endpointGroups;

    @JsonProperty(required = true)
    @NotNull
    private Map<@NotEmpty String, @NotNull NativePlan> plans;

    private List<NativeFlow> flows;

    private NativeApiServices services;

    public NativePlan getPlan(final String plan) {
        return plans.get(plan);
    }

    public List<NativePlan> getPlans() {
        if (plans != null) {
            return new ArrayList<>(this.plans.values());
        }
        return null;
    }

    public void setPlans(List<NativePlan> plans) {
        if (plans != null) {
            this.plans = plans.stream().collect(Collectors.toMap(NativePlan::getId, Function.identity()));
        } else {
            this.plans = new HashMap<>();
        }
    }

    @JsonIgnore
    public boolean isKafkaNative() {
        return ApiType.NATIVE.equals(type) && listeners.stream().anyMatch(KafkaListener.class::isInstance);
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return Stream
            .of(
                Optional
                    .ofNullable(this.getResources())
                    .map(r -> r.stream().filter(Resource::isEnabled).map(Resource::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getFlows())
                    .map(f -> f.stream().filter(NativeFlow::isEnabled).map(NativeFlow::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getPlans())
                    .map(p -> p.stream().map(NativePlan::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getListeners())
                    .map(l -> l.stream().map(NativeListener::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of()),
                Optional
                    .ofNullable(this.getEndpointGroups())
                    .map(r -> r.stream().map(NativeEndpointGroup::getPlugins).flatMap(List::stream).toList())
                    .orElse(List.of())
            )
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
