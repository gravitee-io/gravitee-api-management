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
package io.gravitee.gateway.reactive.policy.adapter.context;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.RuntimeProcessorFailure;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.reactor.handler.context.DefaultRequestExecutionContext;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class ExecutionContextAdapterTest {

    @Test
    public void shouldAddInternalExecutionFailureFromProcessorFailure() {
        DefaultRequestExecutionContext requestExecutionContext = new DefaultRequestExecutionContext(null, null);
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(requestExecutionContext);

        contextAdapter.setAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE, new RuntimeProcessorFailure("error"));
        assertNotNull(
            requestExecutionContext.getInternalAttribute(
                io.gravitee.gateway.reactive.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE
            )
        );
    }

    @Test
    public void shouldGetProcessorFailureFromInternalExecutionFailure() {
        DefaultRequestExecutionContext requestExecutionContext = new DefaultRequestExecutionContext(null, null);
        requestExecutionContext.setInternalAttribute(
            io.gravitee.gateway.reactive.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE,
            new ExecutionFailure().statusCode(200)
        );
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(requestExecutionContext);

        Object adapterAttribute = contextAdapter.getAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNotNull(adapterAttribute);
        assertInstanceOf(ProcessorFailure.class, adapterAttribute);
        assertEquals(200, ((ProcessorFailure) adapterAttribute).statusCode());
    }

    @Test
    public void shouldRemoveInternalExecutionFailure() {
        DefaultRequestExecutionContext requestExecutionContext = new DefaultRequestExecutionContext(null, null);
        requestExecutionContext.setInternalAttribute(
            io.gravitee.gateway.reactive.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE,
            new ExecutionFailure().statusCode(200)
        );
        ExecutionContextAdapter contextAdapter = ExecutionContextAdapter.create(requestExecutionContext);

        contextAdapter.removeAttribute(ExecutionContext.ATTR_FAILURE_ATTRIBUTE);
        assertNull(
            requestExecutionContext.getInternalAttribute(
                io.gravitee.gateway.reactive.api.context.ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE
            )
        );
    }
}
