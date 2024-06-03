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
package io.gravitee.rest.api.service.spring;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Getter
@Configuration
public class InstallationConfiguration {

    private final Environment environment;

    private final String apiURL;

    private final String managementProxyPath;

    public InstallationConfiguration(
        @Value("${installation.api.url:http://localhost:8083}") String apiURL,
        @Value(
            "${installation.api.proxyPath.management:${http.api.management.entrypoint:${http.api.entrypoint:/}management}}"
        ) String managementProxyPath,
        Environment environment
    ) {
        this.environment = environment;
        this.apiURL = apiURL;
        this.managementProxyPath = managementProxyPath;
    }

    private Map<String, String> additionalInformation;

    public Map<String, String> getAdditionalInformation() {
        if (additionalInformation == null) {
            additionalInformation = readPropertyAsMap("installation.additionalInformation");
        }
        return additionalInformation;
    }

    private Map<String, String> readPropertyAsMap(String property) {
        String keyName = String.format("%s[%s].%s", property, 0, "name");
        String keyValue = String.format("%s[%s].%s", property, 0, "value");
        Map<String, String> properties = new HashMap<>();
        int index = 0;
        while (environment.containsProperty(keyName)) {
            String name = environment.getProperty(keyName);
            String value = environment.getProperty(keyValue);

            if (name != null) {
                properties.put(name.toUpperCase(), value);
            }
            index++;
            keyName = String.format("%s[%s].%s", property, index, "name");
            keyValue = String.format("%s[%s].%s", property, index, "value");
        }

        return properties;
    }
}
