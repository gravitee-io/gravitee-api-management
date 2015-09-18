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
package io.gravitee.gateway.policies;

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.policy.PolicyChain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * <em>
 * Note: As we need to mock the java.lang.System class, then we need
 * to prepare the underlying tested class. That's why we use the @PrepareForTest on the ResponseTimePolicy class.
 * For more information, see https://github.com/jayway/powermock/wiki/MockitoUsage#mocking-static-method
 * </em>
 *
 * @author Aurélien Bourdon (aurelien.bourdon at gmail.com)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ResponseTimePolicy.class)
public class ResponseTimePolicyTest {

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private PolicyChain policyChain;

    private ResponseTimePolicy policy;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        policy = new ResponseTimePolicy();
    }

    @Test
    public void testOnRequest() throws Exception {
        doNothing().when(policyChain).doNext(request, response);

        policy.onRequest(request, response, policyChain);

        verify(policyChain, times(1)).doNext(request, response);
    }

    @Test
    public void testOnResponse() throws Exception {
        mockStatic(System.class);

        HttpHeaders headers = new HttpHeaders();
        stub(response.headers()).toReturn(headers);

        doNothing().when(policyChain).doNext(request, response);

        long startTime = 10l;
        when(System.currentTimeMillis()).thenReturn(startTime);
        policy.onRequest(request, response, policyChain);

        long endTime = 45l;
        when(System.currentTimeMillis()).thenReturn(endTime);
        policy.onResponse(request, response, policyChain);

        verify(policyChain, times(2)).doNext(request, response);
        assertEquals(String.valueOf(endTime - startTime), headers.getFirst(GraviteeHttpHeader.X_GRAVITEE_RESPONSE_TIME));
    }

}