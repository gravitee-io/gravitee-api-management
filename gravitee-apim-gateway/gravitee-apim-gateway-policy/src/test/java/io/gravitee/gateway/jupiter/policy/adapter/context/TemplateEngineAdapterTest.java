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
package io.gravitee.gateway.jupiter.policy.adapter.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class TemplateEngineAdapterTest {

    protected static final String EXPRESSION = "expression";

    @Mock
    private TemplateEngine templateEngine;

    private TemplateEngineAdapter cut;

    @BeforeEach
    void init() {
        cut = new TemplateEngineAdapter(templateEngine);
    }

    @Test
    void shouldCallDelegateGetValue() {
        cut.getValue(EXPRESSION, String.class);
        verify(templateEngine).getValue(EXPRESSION, String.class);
    }

    @Test
    void shouldCallDelegateConvert() {
        cut.convert(EXPRESSION);
        verify(templateEngine).convert(EXPRESSION);
    }

    @Test
    void shouldCallDelegateEval() {
        cut.eval(EXPRESSION, String.class);
        verify(templateEngine).eval(EXPRESSION, String.class);
    }

    @Test
    void shouldInstantiateAdaptedTemplateContextOnce() {
        final TemplateContext templateContext = cut.getTemplateContext();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateContext, cut.getTemplateContext());
        }
    }

    @Test
    void shouldRestoreTemplateContext() {
        final TemplateContextAdapter templateContext = mock(TemplateContextAdapter.class);
        ReflectionTestUtils.setField(cut, "adaptedTemplateContext", templateContext);

        cut.restore();

        verify(templateContext).restore();
    }
}
