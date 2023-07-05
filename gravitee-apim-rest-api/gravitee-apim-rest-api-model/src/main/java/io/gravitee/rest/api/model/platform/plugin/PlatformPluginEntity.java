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
package io.gravitee.rest.api.model.platform.plugin;

import java.util.Objects;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Data
@NoArgsConstructor
@SuperBuilder
public class PlatformPluginEntity {

    /**
     * The plugin identifier
     */
    private String id;

    /**
     * The plugin name
     */
    private String name;

    /**
     * The plugin description
     */
    private String description;

    /**
     * The plugin category
     */
    private String category;

    /**
     * The plugin version
     */
    private String version;

    /**
     * The plugin version
     */
    private String icon;

    /**
     * The plugin status
     */
    private boolean deployed;

    /**
     * The license feature to unlock the plugin, if enterprise
     */
    private String feature;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformPluginEntity that = (PlatformPluginEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
