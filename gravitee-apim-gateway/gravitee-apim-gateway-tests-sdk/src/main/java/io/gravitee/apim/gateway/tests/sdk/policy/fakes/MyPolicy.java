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
package io.gravitee.apim.gateway.tests.sdk.policy.fakes;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import io.gravitee.policy.api.annotations.OnResponse;

/**
 * Increments a counter each time we pass through this policy and set it in header "my-counter"
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MyPolicy {

    private static final String MY_COUNTER = "my-counter";
    static int counter = 0;

    @OnRequest
    public void onRequest(Request request, Response response, PolicyChain policyChain) {
        // Backend must receive a "my_counter" header with a value of 1
        request.headers().set(MY_COUNTER, Integer.toString(++counter));
        response.headers().set(MY_COUNTER, Integer.toString(counter));
        // Consumer must receive a "my_counter" header with a value of 2
        policyChain.doNext(request, response);
    }

    @OnResponse
    public void onResponse(Request request, Response response, PolicyChain policyChain) {
        // Backend must receive a "my_counter" header with a value of 1
        request.headers().set(MY_COUNTER, Integer.toString(++counter));
        response.headers().set(MY_COUNTER, Integer.toString(counter));
        // Consumer must receive a "my_counter" header with a value of 2
        policyChain.doNext(request, response);
    }

    public static void clear() {
        counter = 0;
    }
}
