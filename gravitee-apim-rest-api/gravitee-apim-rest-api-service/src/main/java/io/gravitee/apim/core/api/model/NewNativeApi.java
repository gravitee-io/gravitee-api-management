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
package io.gravitee.apim.core.api.model;

import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Setter(lombok.AccessLevel.NONE)
public class NewNativeApi extends AbstractNewApi {

    private List<NativeListener> listeners;

    private List<NativeEndpointGroup> endpointGroups;

    @Builder.Default
    private List<NativeFlow> flows = List.of();

    /**
     * @return An instance of {@link NativeApi.NativeApiBuilder} based on the current state of this NewNativeApi.
     */
    public NativeApi.NativeApiBuilder<?, ?> toNativeApiDefinitionBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return NativeApi
            .builder()
            .name(name)
            .type(type)
            .apiVersion(apiVersion)
            .definitionVersion(definitionVersion)
            .tags(tags)
            .listeners(listeners)
            .endpointGroups(endpointGroups)
            .flows(flows);
    }
}
