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
package io.gravitee.definition.model.services.discovery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.Service;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointDiscoveryService extends Service {

    public static final String SERVICE_KEY = "discovery";
    public static final Map<String, String> PROVIDERS_PLUGIN_MAPPING = Collections.singletonMap("CONSUL", "consul-service-discovery");

    public EndpointDiscoveryService() {
        super(SERVICE_KEY);
    }

    private String provider;

    private Object configuration;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        if (provider == null) {
            this.provider = null;
        } else {
            this.provider = PROVIDERS_PLUGIN_MAPPING.getOrDefault(provider.toUpperCase(), provider.toLowerCase());
        }
    }

    @Schema(implementation = Object.class)
    @JsonRawValue
    public String getConfiguration() {
        return configuration == null ? null : configuration.toString();
    }

    @JsonIgnore
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @JsonSetter
    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public static String getServiceKey() {
        return SERVICE_KEY;
    }
}
