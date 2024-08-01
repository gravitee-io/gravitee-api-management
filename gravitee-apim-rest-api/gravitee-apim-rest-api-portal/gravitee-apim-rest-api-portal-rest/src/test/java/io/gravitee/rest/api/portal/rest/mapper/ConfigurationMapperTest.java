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
import io.gravitee.rest.api.model.settings.Enabled;
import io.gravitee.rest.api.model.settings.PortalNext;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.portal.rest.model.ConfigurationPortalNext;
import io.gravitee.rest.api.portal.rest.model.ConfigurationResponse;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConfigurationMapperTest {

    private ConfigurationMapper configurationMapper;

    @BeforeEach
    public void setup() {
        configurationMapper = new ConfigurationMapper();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideParameters")
    public void convertPortalNextShouldReturnConfigurationPortalNextWithCorrectValues(
        String siteTitle,
        Boolean bannerEnabled,
        String bannerTitle,
        String bannerSubtitle,
        Boolean inputEnabled,
        Boolean expectedEnabled
    ) {
        PortalNext portalNext = new PortalNext();
        portalNext.setSiteTitle(siteTitle);
        var banner = new PortalNext.Banner();
        banner.setEnabled(bannerEnabled);
        banner.setTitle(bannerTitle);
        banner.setSubtitle(bannerSubtitle);
        portalNext.setBanner(banner);
        portalNext.setAccess(new Enabled(inputEnabled));

        ConfigurationPortalNext configurationPortalNext = configurationMapper.convert(portalNext);

        Assertions.assertEquals(siteTitle, configurationPortalNext.getSiteTitle());
        Assertions.assertNotNull(configurationPortalNext.getBanner());
        Assertions.assertEquals(bannerTitle, configurationPortalNext.getBanner().getTitle());
        Assertions.assertEquals(bannerSubtitle, configurationPortalNext.getBanner().getSubtitle());
        Assertions.assertEquals(bannerEnabled, configurationPortalNext.getBanner().getEnabled());
        Assertions.assertNotNull(configurationPortalNext.getAccess());
        Assertions.assertEquals(expectedEnabled, configurationPortalNext.getAccess().getEnabled());
    }

    private static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("Test Site Title", true, "Test Banner Title", "Test Banner Subtitle", true, true),
            Arguments.of("Test Site Title", false, "Test Banner Title", "Test Banner Subtitle", false, false)
        );
    }

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
