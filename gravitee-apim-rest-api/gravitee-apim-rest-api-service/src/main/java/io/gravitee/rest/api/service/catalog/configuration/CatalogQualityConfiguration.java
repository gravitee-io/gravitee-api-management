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
package io.gravitee.rest.api.service.catalog.configuration;

import io.gravitee.rest.api.service.catalog.quality.CatalogQualityChatClient;
import io.gravitee.rest.api.service.catalog.quality.OllamaCatalogQualityChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CatalogQualityConfiguration {

    @Bean
    public CatalogQualityChatClient catalogQualityChatClient(Environment env) {
        var url = env.getProperty("catalog.quality.ollama.url", "http://localhost:11434");
        var model = env.getProperty("catalog.quality.ollama.model", "llama3.2:1b");
        return new OllamaCatalogQualityChatClient(url, model);
    }
}
