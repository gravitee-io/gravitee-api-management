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
package io.gravitee.gateway.core.policy;

import io.gravitee.gateway.api.Policy;
import io.gravitee.gateway.api.PolicyResult;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RequestPolicyChain extends AbstractPolicyChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestPolicyChain.class);

    public RequestPolicyChain(Set<Policy> policies) {
        super(policies);
    }

    public Observable<PolicyResult> asyncHandle(final Request request, final Response response) {
        return Observable.create(
                new Observable.OnSubscribe<PolicyResult>() {

                    @Override
                    public void call(Subscriber<? super PolicyResult> observer) {
                        doNext(request, response);

                        observer.onError(new IllegalStateException("je suis ici"));

                        /*
                        observer.onNext(new PolicyResult() {
                        });
                        */


                        //observer.onCompleted();
                    }
                }
        );
    }

    @Override
    public void doNext(final Request request, final Response response) {
        if (iterator().hasNext()) {
            Policy policy = iterator().next();
            policy.onRequest(request, response, this);
        }
    }

}
