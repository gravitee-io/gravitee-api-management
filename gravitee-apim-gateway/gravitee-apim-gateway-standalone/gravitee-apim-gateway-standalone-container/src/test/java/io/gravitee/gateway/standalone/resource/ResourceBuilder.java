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
package io.gravitee.gateway.standalone.resource;

import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceConfiguration;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResourceBuilder {

    public static ResourcePlugin build(String id, Class<?> resource) {
        return build(id, resource, null);
    }

    public static ResourcePlugin build(String id, Class<?> resource, Class<? extends ResourceConfiguration> resourceConfiguration) {
        return new ResourcePlugin() {
            @Override
            public Class<?> resource() {
                return resource;
            }

            @Override
            public Class configuration() {
                return resourceConfiguration;
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public String clazz() {
                return resource.getClass().getName();
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
            public boolean deployed() {
                return true;
            }
        };
    }
}
