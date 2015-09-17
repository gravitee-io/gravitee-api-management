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
package io.gravitee.gateway.core.reactor.handler.impl;

import io.gravitee.common.http.GraviteeHttpHeader;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.http.HttpServerResponse;
import io.gravitee.gateway.core.http.client.HttpClient;
import io.gravitee.gateway.core.policy.Policy;
import io.gravitee.gateway.core.policy.impl.AbstractPolicyChain;
import io.gravitee.gateway.core.reactor.handler.ContextHandler;
import io.gravitee.gateway.core.reporter.ReporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiHandler extends ContextHandler {

    @Autowired
    private ApiDefinition apiDefinition;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private ReporterService reporterService;

    @Override
    public void handle(Request request, Response response, Handler<Response> handler) {
        request.headers().set(GraviteeHttpHeader.X_GRAVITEE_API_NAME.toString(), apiDefinition.getName());

        // 1_ Calculate policies
        List<Policy> policies = getPolicyResolver().resolve(request);

        // 2_ Apply request policies
        AbstractPolicyChain requestPolicyChain = getRequestPolicyChainBuilder().newPolicyChain(policies);
        requestPolicyChain.setResultHandler(requestPolicyResult -> {
            if (requestPolicyResult.isFailure()) {
                ((HttpServerResponse) response).setStatus(requestPolicyResult.httpStatusCode());
                handler.handle(response);
                reporterService.report(request, response);
            } else {
                // 3_ Call remote service
                httpClient.invoke(request, response, result -> {
                    // 4_ Apply response policies

                    // FIXME: we are never go here because policy does not implement @OnResponse
                    // and so do not apply doNext in policy chain
                    /*
                    AbstractPolicyChain responsePolicyChain = ApiHandler.this.getResponsePolicyChainBuilder().newPolicyChain(policies);
                    responsePolicyChain.setResultHandler(responsePolicyResult -> {
                        if (responsePolicyResult.isFailure()) {
                            ((HttpServerResponse) response).setStatus(requestPolicyResult.httpStatusCode());
                            handler.handle(response);
                            reporterService.report(request, response);
                        } else {

                            // 5_ Transfer the proxy response to the initial consumer
                            handler.handle(response);
                            reporterService.report(request, response);
                        }
                    });

                    responsePolicyChain.doNext(request, response);
                    */

                    // 5_ Transfer the proxy response to the initial consumer
                    handler.handle(response);
                    reporterService.report(request, response);
                });
            }
        });

        requestPolicyChain.doNext(request, response);
    }

    @Override
    public String getContextPath() {
        return apiDefinition.getProxy().getContextPath();
    }

    @Override
    public String getVirtualHost() {
        return apiDefinition.getProxy().getTarget().getAuthority();
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        httpClient.start();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        httpClient.stop();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiHandler{");
        sb.append("contextPath=").append(apiDefinition.getProxy().getContextPath());
        sb.append('}');
        return sb.toString();
    }
}
