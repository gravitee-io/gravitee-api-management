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
package io.gravitee.apim.gateway.tests.sdk.service;

import io.gravitee.common.service.AbstractService;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceBuilder {

    public static Plugin build(Class<? extends AbstractService<?>> serviceClass) {
        return new TestServicePlugin(serviceClass);
    }

    private static class TestServicePlugin implements Plugin {

        Class<? extends AbstractService<?>> serviceClass;

        TestServicePlugin(Class<? extends AbstractService<?>> serviceClass) {
            this.serviceClass = serviceClass;
        }

        @Override
        public String id() {
            return "test-service-".concat(serviceClass.getName());
        }

        @Override
        public String clazz() {
            return serviceClass.getName();
        }

        @Override
        public String type() {
            return "service";
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
    }
}
