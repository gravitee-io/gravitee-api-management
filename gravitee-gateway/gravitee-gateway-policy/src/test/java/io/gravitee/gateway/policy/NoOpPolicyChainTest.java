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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NoOpPolicyChainTest {

    private NoOpPolicyChain noOpPolicyChain;

    @Mock
    private ExecutionContext executionContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldCallResultHandler() {
        Handler<ExecutionContext> resultHandler = Mockito.spy(Handler.class);

        noOpPolicyChain = new NoOpPolicyChain(executionContext);

        noOpPolicyChain.handler(resultHandler);
        noOpPolicyChain.doNext(Mockito.mock(Request.class), Mockito.mock(Response.class));

        Mockito.verify(resultHandler, Mockito.times(1)).handle(executionContext);
    }

    @Test
    public void shouldReturnNullIterator() {
        noOpPolicyChain = new NoOpPolicyChain(executionContext);

        Assert.assertEquals(Collections.emptyIterator(), noOpPolicyChain.iterator());
    }
}
