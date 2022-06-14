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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.RuntimeProcessorFailure;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultRequestExecutionContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class ExecutionContextAdapterTest {

    @Test
    void shouldAddInternalExecutionFailureFromProcessorFailure() {
        DefaultRequestExecutionContext requestExecutionContext = new DefaultRequestExecutionContext(null, null);
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(requestExecutionContext);

        contextAdapter.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, new RuntimeProcessorFailure("error"));
        assertNotNull(
            requestExecutionContext.getInternalAttribute(
                io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE
            )
        );
    }

    @Test
    void shouldGetProcessorFailureFromInternalExecutionFailure() {
        DefaultRequestExecutionContext requestExecutionContext = new DefaultRequestExecutionContext(null, null);
        requestExecutionContext.setInternalAttribute(
            io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE,
            new ExecutionFailure(200)
        );
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(requestExecutionContext);

        Object adapterAttribute = contextAdapter.getAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNotNull(adapterAttribute);
        assertInstanceOf(ProcessorFailure.class, adapterAttribute);
        assertEquals(200, ((ProcessorFailure) adapterAttribute).statusCode());
    }

    @Test
    void shouldRemoveInternalExecutionFailure() {
        DefaultRequestExecutionContext requestExecutionContext = new DefaultRequestExecutionContext(null, null);
        requestExecutionContext.setInternalAttribute(
            io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE,
            new ExecutionFailure(200)
        );
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(requestExecutionContext);

        contextAdapter.removeAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNull(
            requestExecutionContext.getInternalAttribute(
                io.gravitee.gateway.jupiter.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE
            )
        );
    }

    @Test
    void shouldInstantiateAdaptedTemplateEngineOnce() {
        final ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(mock(RequestExecutionContext.class));
        final TemplateEngine templateEngine = contextAdapter.getTemplateEngine();

        for (int i = 0; i < 10; i++) {
            assertEquals(templateEngine, contextAdapter.getTemplateEngine());
        }
    }

    @Test
    void shouldRestoreTemplateEngine() {
        final ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(mock(RequestExecutionContext.class));
        final TemplateEngineAdapter templateEngine = mock(TemplateEngineAdapter.class);

        ReflectionTestUtils.setField(contextAdapter, "adaptedTemplateEngine", templateEngine);

        contextAdapter.restore();

        verify(templateEngine).restore();
    }
}
