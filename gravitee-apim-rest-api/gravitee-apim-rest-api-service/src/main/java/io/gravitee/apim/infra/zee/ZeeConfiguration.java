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
package io.gravitee.apim.infra.zee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Zee Mode — AI-assisted resource creation.
 *
 * <p>
 * Reads Azure OpenAI connection properties and rate limiting settings
 * from {@code gravitee.yml} or environment variables.
 *
 * @author Derek Burger
 */
@Configuration
public class ZeeConfiguration {

    @Value("${ai.zee.enabled:false}")
    private boolean enabled;

    @Value("${ai.zee.azure.url:#{null}}")
    private String azureUrl;

    @Value("${ai.zee.azure.apiKey:#{null}}")
    private String azureApiKey;

    @Value("${ai.zee.rateLimiting.maxRequestsPerMinute:10}")
    private int maxRequestsPerMinute;

    public boolean isEnabled() {
        return enabled;
    }

    public String getAzureUrl() {
        return azureUrl;
    }

    public String getAzureApiKey() {
        return azureApiKey;
    }

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }
}
