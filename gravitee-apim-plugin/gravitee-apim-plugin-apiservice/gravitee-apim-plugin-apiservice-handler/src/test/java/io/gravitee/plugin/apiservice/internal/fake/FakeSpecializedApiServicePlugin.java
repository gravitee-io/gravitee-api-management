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
package io.gravitee.plugin.apiservice.internal.fake;

import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class FakeSpecializedApiServicePlugin implements ApiServicePlugin<FakeSpecializedApiServiceFactory, FakeApiServiceConfiguration> {

    private final boolean deployed;

    @Override
    public String id() {
        return "fake-specialized-api-service";
    }

    @Override
    public String clazz() {
        return FakeSpecializedApiServiceFactory.class.getCanonicalName();
    }

    @Override
    public Class<FakeSpecializedApiServiceFactory> connectorFactory() {
        return FakeSpecializedApiServiceFactory.class;
    }

    @Override
    public String type() {
        return "specialized-api-service";
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
    public Class<FakeApiServiceConfiguration> configuration() {
        return null;
    }

    @Override
    public boolean deployed() {
        return deployed;
    }
}
