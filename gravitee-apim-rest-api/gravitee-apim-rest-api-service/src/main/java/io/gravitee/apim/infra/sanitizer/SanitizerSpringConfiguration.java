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
package io.gravitee.apim.infra.sanitizer;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.gravitee.apim.core.json.JsonSerializer;
import io.gravitee.apim.core.sanitizer.HtmlSanitizer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDeserializer;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.apim.infra.json.jackson.JacksonJsonSerializer;
import io.gravitee.apim.infra.json.jackson.JsonMapperFactory;
import javax.inject.Singleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SanitizerSpringConfiguration {

    @Bean
    @Singleton
    public HtmlSanitizer htmlSanitizer() {
        return new HtmlSanitizerImpl();
    }
}
