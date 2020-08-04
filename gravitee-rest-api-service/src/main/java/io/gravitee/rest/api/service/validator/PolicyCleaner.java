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
package io.gravitee.rest.api.service.validator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyCleaner {
    private static Logger LOGGER = LoggerFactory.getLogger(PolicyCleaner.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String clearNullValues(String jsonPayload){
        try {
            // #4087 - ugly fix to remove null entries in the PolicyConfiguration
            // otherwise updating the API is impossible.
            Object staleObject = objectMapper.readValue(jsonPayload, Object.class);
            return objectMapper.writeValueAsString(staleObject);
        } catch (IOException e) {
            LOGGER.debug("Unable to remove 'null' values from policy configuration, return the original value", e);
            return jsonPayload;
        }
    }
}
