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
package io.gravitee.apim.infra.domain_service.parameters;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
public class ParametersDomainServiceImplTest {

    private ParameterService parameterService;
    private MockEnvironment environment;
    private ParametersDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        parameterService = mock(ParameterService.class);
        environment = new MockEnvironment();
        cut = new ParametersDomainServiceImpl(environment, parameterService);
    }

    @Nested
    class GetSystemParameters {

        @BeforeEach
        void setUp() {
            environment.setProperty(Key.CONSOLE_CUSTOMIZATION_TITLE.key(), "title");
            environment.setProperty(Key.CONSOLE_CUSTOMIZATION_LOGO.key(), "logo");
        }

        @Test
        void should_find_all_parameters() {
            var result = cut.getSystemParameters(List.of(Key.CONSOLE_CUSTOMIZATION_TITLE, Key.CONSOLE_CUSTOMIZATION_LOGO));
            Assertions.assertFalse(result.isEmpty());
            assertThat(result.get(Key.CONSOLE_CUSTOMIZATION_TITLE)).isEqualTo("title");
            assertThat(result.get(Key.CONSOLE_CUSTOMIZATION_LOGO)).isEqualTo("logo");
        }

        @Test
        void should_not_find_any_parameters() {
            var result = cut.getSystemParameters(List.of(Key.CONSOLE_CUSTOMIZATION_THEME_MENUACTIVE));
            Assertions.assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetEnvironmentParameters {

        private final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");

        @Test
        void should_find_parameters() {
            var keyList = List.of(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY, Key.PORTAL_NEXT_THEME_COLOR_SECONDARY);
            when(parameterService.findAll(eq(EXECUTION_CONTEXT), eq(keyList), any(), eq(ParameterReferenceType.ENVIRONMENT)))
                .thenReturn(
                    Map.of(
                        Key.PORTAL_NEXT_THEME_COLOR_PRIMARY.key(),
                        List.of("primary-color"),
                        Key.PORTAL_NEXT_THEME_COLOR_SECONDARY.key(),
                        List.of("secondary-color")
                    )
                );

            var result = cut.getEnvironmentParameters(EXECUTION_CONTEXT, keyList);
            assertThat(result)
                .isEqualTo(
                    Map.of(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY, "primary-color", Key.PORTAL_NEXT_THEME_COLOR_SECONDARY, "secondary-color")
                );
        }

        @Test
        void should_not_return_parameters_without_value() {
            var keyList = List.of(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY, Key.PORTAL_NEXT_THEME_COLOR_SECONDARY);
            when(parameterService.findAll(eq(EXECUTION_CONTEXT), eq(keyList), any(), eq(ParameterReferenceType.ENVIRONMENT)))
                .thenReturn(
                    Map.of(
                        Key.PORTAL_NEXT_THEME_COLOR_PRIMARY.key(),
                        List.of("primary-color"),
                        Key.PORTAL_NEXT_THEME_COLOR_SECONDARY.key(),
                        List.of()
                    )
                );

            var result = cut.getEnvironmentParameters(EXECUTION_CONTEXT, keyList);
            assertThat(result).isEqualTo(Map.of(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY, "primary-color"));
        }

        @Test
        void should_not_find_any_parameters() {
            var keyList = List.of(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY, Key.PORTAL_NEXT_THEME_COLOR_SECONDARY);
            when(parameterService.findAll(eq(EXECUTION_CONTEXT), eq(keyList), any(), eq(ParameterReferenceType.ENVIRONMENT)))
                .thenReturn(Map.of());

            var result = cut.getEnvironmentParameters(EXECUTION_CONTEXT, keyList);
            assertThat(result).isEqualTo(Map.of());
        }
    }
}
