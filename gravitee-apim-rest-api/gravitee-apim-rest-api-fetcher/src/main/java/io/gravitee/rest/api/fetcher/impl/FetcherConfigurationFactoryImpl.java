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
package io.gravitee.rest.api.fetcher.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.rest.api.fetcher.FetcherConfigurationFactory;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Nicolas GERAUD (nicolas.geraud [at] graviteesource [dot] com)
 * @author GraviteeSource Team
 */
@Slf4j
public class FetcherConfigurationFactoryImpl implements FetcherConfigurationFactory {

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public <T extends FetcherConfiguration> T create(Class<T> fetcherConfigurationClass, String configuration) {
        if (configuration == null || configuration.isEmpty()) {
            log.error("Unable to create a Fetcher configuration from a null or empty configuration data");
            return null;
        }

        try {
            return mapper.readValue(configuration, fetcherConfigurationClass);
        } catch (IOException ex) {
            log.error("Unable to instance Fetcher configuration for {}", fetcherConfigurationClass.getName(), ex);
        }

        return null;
    }
}
