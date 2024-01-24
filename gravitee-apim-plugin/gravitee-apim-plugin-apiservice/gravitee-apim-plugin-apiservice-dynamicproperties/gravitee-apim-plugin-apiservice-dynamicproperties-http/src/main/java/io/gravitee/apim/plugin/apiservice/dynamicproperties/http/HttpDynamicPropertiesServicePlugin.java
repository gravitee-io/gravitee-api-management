/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http;

import static io.gravitee.apim.plugin.apiservice.dynamicproperties.http.HttpDynamicPropertiesService.HTTP_DYNAMIC_PROPERTIES_TYPE;

import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpDynamicPropertiesServicePlugin
    implements ApiServicePlugin<HttpDynamicPropertiesServiceFactory, HttpDynamicPropertiesServiceConfiguration> {

    @Override
    public String id() {
        return HTTP_DYNAMIC_PROPERTIES_TYPE;
    }

    @Override
    public String clazz() {
        return HttpDynamicPropertiesServiceFactory.class.getCanonicalName();
    }

    @Override
    public Class<HttpDynamicPropertiesServiceFactory> connectorFactory() {
        return HttpDynamicPropertiesServiceFactory.class;
    }

    @Override
    public String type() {
        return "management-api-service";
    }

    @Override
    public Path path() {
        return null;
    }

    @Override
    public PluginManifest manifest() {
        return null;
    }

    @Override
    public URL[] dependencies() {
        return new URL[0];
    }

    @Override
    public Class<HttpDynamicPropertiesServiceConfiguration> configuration() {
        return HttpDynamicPropertiesServiceConfiguration.class;
    }

    @Override
    public boolean deployed() {
        return true;
    }
}
