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
package io.gravitee.gateway.standalone.junit.stmt;

import io.gravitee.common.utils.UUID;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.standalone.ApiLoaderInterceptor;
import io.gravitee.gateway.standalone.Container;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.utils.SocketUtils;
import org.junit.runners.model.Statement;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class ApiDeployerStatement extends Statement {

    private final Statement base;
    private final Object target;

    private Container container;

    public ApiDeployerStatement(Statement base, Object target) {
        this.base = base;
        this.target = target;
    }

    @Override
    public void evaluate() throws Throwable {
        URL home = ApiDeployerStatement.class.getResource("/gravitee-01/");
        System.setProperty("gravitee.home", URLDecoder.decode(home.getPath(), "UTF-8"));

        container = new Container();
        container.start();

        Thread.sleep(1000);

        ApiManager apiManager = container.getApplicationContext().getBean(ApiManager.class);
        Api api = loadApi(target.getClass().getAnnotation(ApiDescriptor.class).value());

        try {
            apiManager.deploy(api);
            base.evaluate();
        } finally {
            apiManager.undeploy(api.getId());
            container.stop();
        }
    }

    private Api loadApi(String apiDescriptorPath) throws Exception {
        URL jsonFile = ApiDeployerStatement.class.getResource(apiDescriptorPath);
        Api api = new GraviteeMapper().readValue(jsonFile, Api.class);

        if (ApiLoaderInterceptor.class.isAssignableFrom(target.getClass())) {
            ApiLoaderInterceptor loader = (ApiLoaderInterceptor) target;
            loader.before(api);
        }

        boolean enhanceHttpPort = target.getClass().getAnnotation(ApiDescriptor.class).enhanceHttpPort();

        if (enhanceHttpPort) {
            List<Endpoint> endpoints = api.getProxy().getEndpoints();
            List<Integer> bindPorts = SocketUtils.getBindPorts();

            for(int i = 0 ; i < bindPorts.size() ; i++) {
                int port = SocketUtils.getBindPorts().get(i);
                if (i < endpoints.size()) {
                    Endpoint edpt = endpoints.get(i);
                    URI target = URI.create(edpt.getTarget());
                    URI newTarget = new URI(target.getScheme(), target.getUserInfo(), target.getHost(), port, target.getPath(), target.getQuery(), target.getFragment());
                    edpt.setTarget(newTarget.toString());
                    edpt.setName(UUID.random().toString());
                } else {
                    // Use the first defined endpoint as reference
                    Endpoint first = endpoints.get(0);
                    URI target = URI.create(first.getTarget());
                    URI newTarget = new URI(target.getScheme(), target.getUserInfo(), target.getHost(), port, target.getPath(), target.getQuery(), target.getFragment());
                    Endpoint edpt = new Endpoint(newTarget.toString());
                    edpt.setName(UUID.random().toString());
                    edpt.setHttpClientOptions(first.getHttpClientOptions());
                    api.getProxy().getEndpoints().add(edpt);
                }
            }
        }

        if (ApiLoaderInterceptor.class.isAssignableFrom(target.getClass())) {
            ApiLoaderInterceptor loader = (ApiLoaderInterceptor) target;
            loader.after(api);
        }

        return api;
    }
}
