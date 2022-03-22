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
package io.gravitee.rest.api.management.rest.resource.swagger;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.WebApplicationContext;

public class GraviteeOpenApiReader extends Reader {

    private Application application;

    @Override
    public void setApplication(Application application) {
        this.application = application;
        super.setApplication(application);
    }

    @Override
    public OpenAPI read(Set<Class<?>> classes, Map<String, Object> resources) {
        OpenAPI api = super.read(classes, resources);
        if (CollectionUtils.isEmpty(api.getServers())) {
            String server = Optional
                .ofNullable(application.getProperties())
                .map(map -> map.get(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .filter(WebApplicationContext.class::isInstance)
                .map(WebApplicationContext.class::cast)
                .map(EnvironmentCapable::getEnvironment)
                .map(
                    environment -> {
                        String url = environment.getProperty("console.api.url");
                        if (url == null) {
                            String hostPortPath = environment.resolvePlaceholders(
                                "${jetty.host:localhost}:${jetty.port:8083}${http.api.management.entrypoint:${http.api.entrypoint:/}management}"
                            );
                            if ("true".equals(environment.getProperty("jetty.secured"))) {
                                url = "https://" + hostPortPath;
                            } else {
                                url = "http://" + hostPortPath;
                            }
                        }
                        return url;
                    }
                )
                .orElse("http://localhost:8083/management");

            api.addServersItem(new Server().url(server));
        }
        return api;
    }
}
