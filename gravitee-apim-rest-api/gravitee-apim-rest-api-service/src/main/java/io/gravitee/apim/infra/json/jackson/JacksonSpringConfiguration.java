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
package io.gravitee.apim.infra.json.jackson;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.gravitee.apim.core.json.JsonSerializer;
import javax.inject.Singleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonSpringConfiguration {

    /**
     * Define a default ObjectMapper bean to common usage. For example, to process JSON diff in Audit.
     * <p>
     * ⚠️ It does not support (De)Serializing Gravitee API definition. (aka GraviteeMapper)
     * </p>
     * <p>
     * ⚠️ It does not support behave like {@link io.gravitee.rest.api.service.spring.ServiceConfiguration#objectMapper() this ObjectMapper}
     * </p>
     * @return an ObjectMapper
     */
    @Bean
    @Qualifier("defaultMapper")
    @Singleton
    public JsonMapper defaultJsonMapper() {
        return JsonMapperFactory.build();
    }

    @Bean
    public JsonSerializer jsonSerializer(@Qualifier("defaultMapper") JsonMapper mapper) {
        return new JacksonJsonSerializer(mapper);
    }

    @Bean
    public JacksonJsonDeserializer jsonDeserializer(@Qualifier("defaultMapper") JsonMapper mapper) {
        return new JacksonJsonDeserializer(mapper);
    }

    @Bean
    public JacksonJsonDiffProcessor jsonDiffProcessor(@Qualifier("defaultMapper") JsonMapper mapper) {
        return new JacksonJsonDiffProcessor(mapper);
    }

    @Bean
    @Qualifier("GraviteeDefinitionSerializer")
    public GraviteeDefinitionJacksonJsonSerializer graviteeDefinitionSerializer() {
        return new GraviteeDefinitionJacksonJsonSerializer();
    }
}
