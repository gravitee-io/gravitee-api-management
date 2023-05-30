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
package fixtures;

import io.gravitee.rest.api.management.v2.rest.model.Resource;
import io.gravitee.rest.api.management.v2.rest.model.Role;
import io.gravitee.rest.api.management.v2.rest.model.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class ResourceFixtures {

    private static final Resource.ResourceBuilder BASE_RESOURCE = Resource
        .builder()
        .name("role-name")
        .type("resource-type")
        .enabled(true)
        .configuration(new LinkedHashMap<>(Map.of("key", "value")));

    private static final io.gravitee.definition.model.plugins.resources.Resource.ResourceBuilder BASE_RESOURCE_ENTITY_V2 =
        io.gravitee.definition.model.plugins.resources.Resource
            .builder()
            .name("role-name")
            .type("resource-type")
            .enabled(true)
            .configuration("{\"key\":\"value\"}");

    private static final io.gravitee.definition.model.v4.resource.Resource.ResourceBuilder BASE_RESOURCE_ENTITY_V4 =
        io.gravitee.definition.model.v4.resource.Resource
            .builder()
            .name("role-name")
            .type("resource-type")
            .enabled(true)
            .configuration("{\"key\":\"value\"}");

    public static Resource aResource() {
        return BASE_RESOURCE.build();
    }

    public static io.gravitee.definition.model.plugins.resources.Resource aResourceEntityV2() {
        return BASE_RESOURCE_ENTITY_V2.build();
    }

    public static io.gravitee.definition.model.v4.resource.Resource aResourceEntityV4() {
        return BASE_RESOURCE_ENTITY_V4.build();
    }
}
