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
package io.gravitee.gamma.rest.core.gravitee_plugin.model;

import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Builder
@AllArgsConstructor
public class PlatformPlugin {

    /**
     * The order every catalog lists its plugins in: by name whatever its case, then by id. The catalogs
     * are rendered side by side, and "Zebra" before "alpha" in one of them would be a difference the user
     * sees with nothing to explain it.
     */
    public static final Comparator<PlatformPlugin> BY_NAME_THEN_ID = Comparator.comparing(
        PlatformPlugin::name,
        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
    ).thenComparing(PlatformPlugin::id);

    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final String version;
    private final String icon;
    private final String feature;
    private final boolean deployed;

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String category() {
        return category;
    }

    public String version() {
        return version;
    }

    public String icon() {
        return icon;
    }

    public String feature() {
        return feature;
    }

    public boolean deployed() {
        return deployed;
    }
}
