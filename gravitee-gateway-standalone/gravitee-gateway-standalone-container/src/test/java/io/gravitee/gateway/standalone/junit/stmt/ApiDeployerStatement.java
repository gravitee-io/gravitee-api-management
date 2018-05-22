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
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.standalone.ApiLoaderInterceptor;
import io.gravitee.gateway.standalone.Container;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.policy.PolicyRegister;
import io.gravitee.gateway.standalone.utils.SocketUtils;
import io.gravitee.plugin.policy.PolicyPluginManager;
import org.junit.runners.model.Statement;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
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

        if (target instanceof PolicyRegister) {
            ((PolicyRegister) target).register(container.getApplicationContext().getBean(PolicyPluginManager.class));
        }

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

        if (api.getProxy().getGroups() == null || api.getProxy().getGroups().isEmpty()) {
            // Createa default endpoint group
            EndpointGroup group = new EndpointGroup();
            group.setName("default");
            group.setEndpoints(Collections.emptySet());
            api.getProxy().setGroups(Collections.singleton(group));
        }

        if (ApiLoaderInterceptor.class.isAssignableFrom(target.getClass())) {
            ApiLoaderInterceptor loader = (ApiLoaderInterceptor) target;
            loader.before(api);
        }

        boolean enhanceHttpPort = target.getClass().getAnnotation(ApiDescriptor.class).enhanceHttpPort();

        if (enhanceHttpPort) {
            EndpointGroup group = api.getProxy().getGroups().iterator().next();
            List<Endpoint> endpoints = new ArrayList<>(group.getEndpoints());
            List<Integer> bindPorts = SocketUtils.getBindPorts();

            for(int i = 0 ; i < bindPorts.size() ; i++) {
                int port = SocketUtils.getBindPorts().get(i);
                if (i < endpoints.size()) {
                    Endpoint edpt = endpoints.get(i);
                    URL target = new URL(edpt.getTarget());
                    URL newTarget = new URL(target.getProtocol(), target.getHost(), port, target.getFile());
                    edpt.setTarget(newTarget.toString());
                    edpt.setName(UUID.random().toString());
                } else {
                    // Use the first defined endpoint as reference
                    HttpEndpoint first = (HttpEndpoint) endpoints.get(0);
                    URL target = new URL(first.getTarget());
                    URL newTarget = new URL(target.getProtocol(), target.getHost(), port, target.getFile());
                    HttpEndpoint edpt = new HttpEndpoint(UUID.random().toString(), newTarget.toString());
                    edpt.setHttpClientOptions(first.getHttpClientOptions());
                    group.getEndpoints().add(edpt);
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
