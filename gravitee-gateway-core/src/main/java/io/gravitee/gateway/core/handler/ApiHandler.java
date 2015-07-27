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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.gateway.core.http.client.HttpClient;
import io.gravitee.gateway.core.policy.Policy;
import io.gravitee.gateway.core.reporter.ReporterManager;
import io.gravitee.model.Api;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;
import rx.Subscriber;

import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiHandler extends ContextHandler {

    /*
    private AccessLogWriter accessLogWriter = new AccessLogWriter();
    private long timeInMs;
    */

    @Autowired
    private Api api;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ReporterManager reporterManager;

    @Override
    public Observable<Response> handle(final Request request, final Response response) {
        //    timeInMs = System.currentTimeMillis();
        return Observable.create(
                new Observable.OnSubscribe<Response>() {

                    @Override
                    public void call(final Subscriber<? super Response> observer) {
                        // 1_ Calculate policies
                        List<Policy> policies = getPolicyResolver().resolve(request);

                        // 2_ Apply request policies
                        getRequestPolicyChainBuilder().newPolicyChain(policies).doNext(request, response);

                        // TODO: How to know that something goes wrong in policy chain and skip
                        // remote service invocation...

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
                                getResponsePolicyChainBuilder().newPolicyChain(policies).doNext(request, response);
                                observer.onNext(response);

                                for (Reporter reporter : reporterManager.getReporters()) {
                                    reporter.report(request, response);
                                }

                                // TODO: must be part of reporting system
                                /*
                                new Thread() {
                                    public void run() {
                                        accessLogWriter.path(request.path()).httpMethod(request.method().name()).apiName(api.getName())
                                            .responseSize(response.content().length).requestDuration(System.currentTimeMillis() - timeInMs).write();
                                    }
                                }.start();
                                */
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
