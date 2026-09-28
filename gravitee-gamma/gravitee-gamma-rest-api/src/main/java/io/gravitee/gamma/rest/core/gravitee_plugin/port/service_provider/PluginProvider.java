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
package io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider;

import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PluginMoreInformation;
import java.util.Set;

/**
 * Read access to the plugins of one type loaded on the platform.
 *
 * <p>A plugin is reported as deployed only when it is deployed on the node and the organization's license enables its
 * feature. Every lookup by id throws {@link io.gravitee.gamma.rest.core.gravitee_plugin.exception.PluginNotFoundException}
 * when no plugin of this type has that id.
 */
public interface PluginProvider {
    Set<PlatformPlugin> findAll(String organizationId);
    PlatformPlugin findById(String organizationId, String pluginId);
    String getSchema(String pluginId);
    String getIcon(String pluginId);
    String getDocumentation(String pluginId);
    PluginMoreInformation getMoreInformation(String pluginId);
}
