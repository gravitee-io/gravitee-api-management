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
package io.gravitee.apim.core.theme.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ThemeCrudServiceInMemory;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ThemeDomainServiceTest {

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ThemeDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new ThemeDomainService(themeCrudService);
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
    }

    @Nested
    class Create {

        @Test
        void should_create_portal_theme() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var newTheme = NewTheme.builder().name("name").type(ThemeType.PORTAL).definitionPortal(portalDefinition).build();

            var result = cut.create(newTheme);
            assertThat(result).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull().isEqualTo(result.getUpdatedAt());
            assertThat(result.getId()).isNotBlank();
        }

        @Test
        void should_create_portal_next_theme() {
            var portalDefinition = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                .builder()
                .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
                .build();

            var newTheme = NewTheme.builder().name("name").type(ThemeType.PORTAL_NEXT).definitionPortalNext(portalDefinition).build();

            var result = cut.create(newTheme);
            assertThat(result).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull().isEqualTo(result.getUpdatedAt());
            assertThat(result.getId()).isNotBlank();
        }
    }
}
