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
package io.gravitee.apim.infra.query_service.parameters;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ParametersQueryServiceImplTest {

    @Mock
    private ParameterService parameterService;

    private ParametersQueryServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new ParametersQueryServiceImpl(parameterService);
    }

    @Test
    void should_find_all_parameters() {
        when(
            parameterService.findAll(
                GraviteeContext.getExecutionContext(),
                List.of(Key.CONSOLE_CUSTOMIZATION_TITLE),
                ParameterReferenceType.SYSTEM
            )
        )
            .thenReturn(Map.of(Key.CONSOLE_CUSTOMIZATION_TITLE.key(), List.of("title")));

        var result = cut.findAll(
            GraviteeContext.getExecutionContext(),
            List.of(Key.CONSOLE_CUSTOMIZATION_TITLE),
            ParameterReferenceType.SYSTEM
        );

        Assertions.assertFalse(result.isEmpty());
        assertThat(result.get(Key.CONSOLE_CUSTOMIZATION_TITLE)).isEqualTo(List.of("title"));
    }

    @Test
    void should_not_find_any_parameters_when_null() {
        when(
            parameterService.findAll(
                GraviteeContext.getExecutionContext(),
                List.of(Key.CONSOLE_CUSTOMIZATION_TITLE),
                ParameterReferenceType.SYSTEM
            )
        )
            .thenReturn(null);

        var result = cut.findAll(
            GraviteeContext.getExecutionContext(),
            List.of(Key.CONSOLE_CUSTOMIZATION_TITLE),
            ParameterReferenceType.SYSTEM
        );

        assertThat(result).isNull();
    }

    @Test
    void should_not_find_any_parameters_when_empty() {
        when(
            parameterService.findAll(
                GraviteeContext.getExecutionContext(),
                List.of(Key.CONSOLE_CUSTOMIZATION_TITLE),
                ParameterReferenceType.SYSTEM
            )
        )
            .thenReturn(Map.of());

        var result = cut.findAll(
            GraviteeContext.getExecutionContext(),
            List.of(Key.CONSOLE_CUSTOMIZATION_TITLE),
            ParameterReferenceType.SYSTEM
        );

        Assertions.assertTrue(result.isEmpty());
    }
}
