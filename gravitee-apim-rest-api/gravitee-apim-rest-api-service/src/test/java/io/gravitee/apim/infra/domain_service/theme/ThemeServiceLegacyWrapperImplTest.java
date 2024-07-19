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
package io.gravitee.apim.infra.domain_service.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.service.ThemeService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ThemeServiceLegacyWrapperImplTest {

    ThemeService themeService;

    ThemeServiceLegacyWrapperImpl service;

    @BeforeEach
    void setUp() {
        themeService = mock(ThemeService.class);

        service = new ThemeServiceLegacyWrapperImpl(themeService);
    }

    @Nested
    class GetCurrentOrCreateDefaultPortalTheme {

        @Test
        @SneakyThrows
        void should_call_legacy_service() {
            var definitionPortal = new ThemeDefinition();
            definitionPortal.setData(List.of());

            var executionContext = new ExecutionContext("ORG_ID", "ENV_ID");

            var themeEntity = ThemeEntity
                .builder()
                .id("portal-id")
                .type(io.gravitee.rest.api.model.theme.ThemeType.PORTAL)
                .definition(definitionPortal)
                .build();

            when(themeService.findOrCreateDefaultPortalTheme(eq(executionContext))).thenReturn(themeEntity);

            var result = service.getCurrentOrCreateDefaultPortalTheme(executionContext);

            assertThat(result)
                .hasFieldOrPropertyWithValue("id", themeEntity.getId())
                .hasFieldOrPropertyWithValue("type", ThemeType.PORTAL)
                .hasFieldOrPropertyWithValue("definitionPortal", definitionPortal);

            verify(themeService, times(1)).findOrCreateDefaultPortalTheme(any());
        }
    }
}
