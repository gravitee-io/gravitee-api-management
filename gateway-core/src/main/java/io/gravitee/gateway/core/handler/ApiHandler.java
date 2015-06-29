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
package io.gravitee.gateway.core.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.http.client.HttpClient;
import io.gravitee.model.Api;
import rx.Observable;
import rx.Subscriber;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiHandler extends ContextHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

    @Autowired
    private Api api;

    @Autowired
    private HttpClient httpClient;

    @Override
    public Observable<Response> handle(final Request request, final Response response) {
        return Observable.create(
                new Observable.OnSubscribe<Response>() {

                    @Override
                    public void call(final Subscriber<? super Response> observer) {
                        // 1_ Apply request policies
                        getRequestPolicyChainBuilder().newPolicyChain(request).doNext(request, response);

                        // TODO: How to know that something goes wrong in policy chain and skip
                        // remote service invocation...

                        // TODO: remove this part from each call...

                        // 2_ Call remote service
                        httpClient.invoke(request, response).subscribe(new Subscriber<Response>() {
                            @Override
                            public void onCompleted() {
                                observer.onCompleted();
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                observer.onError(throwable);
                            }

                            @Override
                            public void onNext(Response response) {
                                getResponsePolicyChainBuilder().newPolicyChain(request).doNext(request, response);
                                observer.onNext(response);
                            }
                        });
                    }
                }
        );
    }

    @Override
    public String getContextPath() {
        return api.getPublicURI().getPath();
    }

    @Override
    public String getVirtualHost() {
        return api.getPublicURI().getAuthority();
    }
}
