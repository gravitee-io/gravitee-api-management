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
package io.gravitee.gateway.jupiter.reactor.handler.context;

import static io.gravitee.gateway.jupiter.api.context.RequestExecutionContext.TEMPLATE_ATTRIBUTE_CONTEXT;
import static io.gravitee.gateway.jupiter.api.context.RequestExecutionContext.TEMPLATE_ATTRIBUTE_REQUEST;
import static io.gravitee.gateway.jupiter.api.context.RequestExecutionContext.TEMPLATE_ATTRIBUTE_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.gravitee.definition.model.Api;
import io.gravitee.el.TemplateContext;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.core.context.MutableRequest;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
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
class DefaultRequestExecutionContextTest extends AbstractExecutionContextTest {

    protected static final String TEST_CONTENT = "Test content";

    @Mock
    protected MutableRequest request;

    @Mock
    protected MutableResponse response;

    @Mock
    protected Api api;

    @BeforeEach
    public void init() {
        executionContext = new DefaultRequestExecutionContext(request, response);
    }

    @Test
    public void shouldPopulateTemplateContextWithVariables() {
        final TemplateEngine templateEngine = executionContext.getTemplateEngine();
        final TemplateContext templateContext = templateEngine.getTemplateContext();

        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_REQUEST));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_RESPONSE));
        assertNotNull(templateContext.lookupVariable(TEMPLATE_ATTRIBUTE_CONTEXT));
    }

    @Test
    public void shouldInitializeTemplateEngineOnlyOnce() {
        final TemplateEngine templateEngine = executionContext.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, executionContext.getTemplateEngine());
        }
    }
}
