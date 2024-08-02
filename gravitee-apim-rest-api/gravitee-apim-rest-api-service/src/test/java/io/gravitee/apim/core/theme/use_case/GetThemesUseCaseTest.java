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
package io.gravitee.apim.core.theme.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class GetThemesUseCaseTest {

    public final ThemeQueryServiceInMemory themeQueryServiceInMemory = new ThemeQueryServiceInMemory();
    private GetThemesUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new GetThemesUseCase(themeQueryServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        themeQueryServiceInMemory.reset();
    }

    @Test
    void should_return_empty_page() {
        var result = cut.execute(GetThemesUseCase.Input.builder().page(1).size(10).enabled(false).type(ThemeType.PORTAL).build()).result();

        assertThat(result).hasFieldOrPropertyWithValue("content", List.of());
    }

    @Test
    void should_return_page_of_results() {
        var portalTheme = Theme.builder().id("portal-id").type(ThemeType.PORTAL).enabled(true).build();
        themeQueryServiceInMemory.initWith(List.of(portalTheme));
        var result = cut.execute(GetThemesUseCase.Input.builder().page(1).size(10).enabled(true).type(ThemeType.PORTAL).build()).result();

        assertThat(result).hasFieldOrPropertyWithValue("content", List.of(portalTheme));
    }
}
