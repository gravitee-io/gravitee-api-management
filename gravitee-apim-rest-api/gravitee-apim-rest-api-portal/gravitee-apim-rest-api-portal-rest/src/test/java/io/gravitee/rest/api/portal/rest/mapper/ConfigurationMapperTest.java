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
package io.gravitee.rest.api.portal.rest.mapper;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.gravitee.rest.api.model.settings.ConsoleSettingsEntity;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.portal.rest.model.ConfigurationResponse;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationMapperTest {

    @Test
    public void testConvert() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        PortalSettingsEntity portalSettingsEntity = mapper.readValue(
            this.getClass().getResourceAsStream("portalSettingsEntity.json"),
            PortalSettingsEntity.class
        );
        ConsoleSettingsEntity consoleSettingsEntity = mapper.readValue(
            this.getClass().getResourceAsStream("consoleSettingsEntity.json"),
            ConsoleSettingsEntity.class
        );
        String expected = IOUtils.toString(this.getClass().getResourceAsStream("expectedPortalConfiguration.json"), "UTF-8");
        ConfigurationMapper configurationMapper = new ConfigurationMapper();
        ConfigurationResponse configuration = configurationMapper.convert(portalSettingsEntity, consoleSettingsEntity);

        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        String configurationAsJSON = mapper.writeValueAsString(configuration);
        assertEquals(mapper.readTree(expected), mapper.readTree(configurationAsJSON));
    }
}
