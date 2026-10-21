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
package io.gravitee.gamma.infra.gravitee_plugin.resource.dto;

import io.gravitee.gamma.core.domain.gravitee_plugin.model.PlatformPlugin;

public record PlatformPluginDto(
    String id,
    String name,
    String description,
    String category,
    String version,
    String icon,
    String feature,
    boolean deployed
) {
    public static PlatformPluginDto from(PlatformPlugin plugin) {
        return new PlatformPluginDto(
            plugin.id(),
            plugin.name(),
            plugin.description(),
            plugin.category(),
            plugin.version(),
            plugin.icon(),
            plugin.feature(),
            plugin.deployed()
        );
    }
}
