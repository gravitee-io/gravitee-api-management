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
package io.gravitee.gateway.handlers.api.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.gateway.handlers.api.definition.Api;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class ApiProperties {

    private final Api api;

    @JsonProperty
    public String getId() {
        return this.api.getId();
    }

    @JsonProperty
    public String getName() {
        return this.api.getName();
    }

    @JsonProperty
    public String getVersion() {
        return this.api.getApiVersion();
    }

    @JsonProperty
    public Map<String, String> getProperties() {
        return this.api.getDefinition().getProperties().getValues();
    }
}
