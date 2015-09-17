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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.api.policy.annotations.OnRequest;
import io.gravitee.gateway.api.policy.annotations.OnResponse;

/**
 * Compute the time spent to compute the entire Policy chain.
 * <p/>
 * <p>
 * This policy MUST to be declared as the fist AND last Policy in the entire chain.
 * </p>
 *
 * @author Aurélien Bourdon (aurelien.bourdon at gmail.com)
 */
@SuppressWarnings("unused")
public class ResponseTimePolicy {

    private long startTime;

    @OnRequest
    public void onRequest(Request request, Response response, PolicyChain handler) {
        startTime = System.currentTimeMillis();
        handler.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, PolicyChain handler) {
        long endTime = System.currentTimeMillis();
        response.headers().set(GraviteeHttpHeader.X_GRAVITEE_RESPONSE_TIME, String.valueOf(endTime - startTime));
        handler.doNext(request, response);
    }

}
