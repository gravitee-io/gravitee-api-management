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
package gamma.inmemory;

import io.gravitee.gamma.core.domain.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.core.domain.gravitee_plugin.model.PluginMoreInformation;
import io.gravitee.gamma.core.port.service_provider.gravitee_plugin.ResourcePluginProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourcePluginProviderInMemory implements ResourcePluginProvider {

    Map<String, PlatformPlugin> plugins = new HashMap<>();
    Map<String, String> schema = new HashMap<>();

    public void addPlugin(PlatformPlugin plugin, String schema) {
        this.plugins.put(plugin.id(), plugin);
        this.schema.put(plugin.id(), schema);
    }

    @Override
    public Set<PlatformPlugin> findAll() {
        return new HashSet<>(plugins.values());
    }

    @Override
    public PlatformPlugin findById(String id) {
        return plugins.get(id);
    }

    @Override
    public String getSchema(String id) {
        return schema.get(id);
    }

    @Override
    public String getIcon(String id) {
        return plugins.get(id).icon();
    }

    @Override
    public String getDocumentation(String id) {
        return "Documentation for " + id + " plugin";
    }

    @Override
    public PluginMoreInformation getMoreInformation(String id) {
        return new PluginMoreInformation("Description of " + id + " plugin", "http://documentation.gravitee.io/" + id, "schema-img-" + id);
    }
}
