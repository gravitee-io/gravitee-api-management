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
package io.gravitee.gateway.jupiter.core.context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.jupiter.core.context.DefaultDeploymentContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultDeploymentContextTest {

    @Mock
    private TemplateVariableProvider templateVariableProvider;

    @Mock
    private ComponentProvider componentProvider;

    private DefaultDeploymentContext cut;

    @BeforeEach
    void init() {
        cut = new DefaultDeploymentContext();
    }

    @Test
    void shouldGetComponent() {
        final DefaultDeploymentContext cut = new DefaultDeploymentContext();
        cut.componentProvider(componentProvider);

        cut.getComponent(Object.class);

        verify(componentProvider).getComponent(Object.class);
    }

    @Test
    void shouldProvideTemplateVariables() {
        final TemplateVariableProvider templateVariableProvider = mock(TemplateVariableProvider.class);
        cut.templateVariableProviders(List.of(templateVariableProvider));

        cut.getTemplateEngine();

        verify(templateVariableProvider).provide(any(TemplateContext.class));
    }

    @Test
    void shouldInitializeTemplateEngineOnlyOnce() {
        final TemplateEngine templateEngine = cut.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, cut.getTemplateEngine());
        }
    }
}
