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
package io.gravitee.gamma.core.resource.fixture;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.core.domain.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.gamma.core.domain.resource.model.CreateResourceCommand;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.model.UpdateResourceCommand;
import java.util.function.UnaryOperator;

public final class ResourceFixture {

    public static final String DEFAULT_ID = "resource-id";
    public static final String DEFAULT_NEW_ID = "new-resource-id";
    public static final String DEFAULT_ENVIRONMENT_ID = "DEFAULT";
    public static final String DEFAULT_ORGANIZATION_ID = "DEFAULT";
    public static final String DEFAULT_USER_ID = "user-id";

    private ResourceFixture() {}

    public static AuditInfo anAuditInfo() {
        return AuditInfo.builder()
            .organizationId(DEFAULT_ORGANIZATION_ID)
            .environmentId(DEFAULT_ENVIRONMENT_ID)
            .actor(AuditActor.builder().userId(DEFAULT_USER_ID).userSource("console").userSourceId(DEFAULT_USER_ID).build())
            .build();
    }

    public static io.gravitee.definition.model.v4.resource.Resource aDefinition() {
        return aDefinition(d -> d);
    }

    public static io.gravitee.definition.model.v4.resource.Resource aDefinition(
        UnaryOperator<io.gravitee.definition.model.v4.resource.Resource.ResourceBuilder> customizer
    ) {
        var builder = io.gravitee.definition.model.v4.resource.Resource.builder()
            .name("my-cache")
            .type("cache")
            .configuration("{\"ttl\":30}")
            .enabled(true);
        return customizer.apply(builder).build();
    }

    public static PlatformPlugin aPlatformPlugin() {
        return new PlatformPlugin("cache", "Cache", "cache description", "unknown", "1.0.0", "icon", null, true);
    }

    public static String pluginJsonSchema() {
        return "{\"type\":\"object\",\"properties\":{\"ttl\":{\"type\":\"integer\"}},\"required\":[\"ttl\"]}";
    }

    public static CreateResourceCommand aCreateCommand() {
        return new CreateResourceCommand(DEFAULT_NEW_ID, "my-cache", "cache", "{\"ttl\":30}", true);
    }

    public static CreateResourceCommand aCreateCommand(UnaryOperator<CreateResourceCommandValues> customizer) {
        return customizer.apply(new CreateResourceCommandValues(DEFAULT_NEW_ID, "my-cache", "cache", "{\"ttl\":30}", true)).toCommand();
    }

    public record CreateResourceCommandValues(String id, String name, String type, String configuration, boolean enabled) {
        public CreateResourceCommandValues id(String id) {
            return new CreateResourceCommandValues(id, name, type, configuration, enabled);
        }

        public CreateResourceCommandValues name(String name) {
            return new CreateResourceCommandValues(id, name, type, configuration, enabled);
        }

        public CreateResourceCommandValues type(String type) {
            return new CreateResourceCommandValues(id, name, type, configuration, enabled);
        }

        public CreateResourceCommandValues configuration(String configuration) {
            return new CreateResourceCommandValues(id, name, type, configuration, enabled);
        }

        public CreateResourceCommandValues enabled(boolean enabled) {
            return new CreateResourceCommandValues(id, name, type, configuration, enabled);
        }

        public CreateResourceCommand toCommand() {
            return new CreateResourceCommand(id, name, type, configuration, enabled);
        }
    }

    public static UpdateResourceCommand anUpdateCommand() {
        return new UpdateResourceCommand("my-cache", "cache", "{\"ttl\":30}", true);
    }

    public static UpdateResourceCommand anUpdateCommand(UnaryOperator<UpdateResourceCommandValues> customizer) {
        return customizer.apply(new UpdateResourceCommandValues("my-cache", "cache", "{\"ttl\":30}", true)).toCommand();
    }

    public record UpdateResourceCommandValues(String name, String type, String configuration, boolean enabled) {
        public UpdateResourceCommandValues name(String name) {
            return new UpdateResourceCommandValues(name, type, configuration, enabled);
        }

        public UpdateResourceCommandValues type(String type) {
            return new UpdateResourceCommandValues(name, type, configuration, enabled);
        }

        public UpdateResourceCommandValues configuration(String configuration) {
            return new UpdateResourceCommandValues(name, type, configuration, enabled);
        }

        public UpdateResourceCommandValues enabled(boolean enabled) {
            return new UpdateResourceCommandValues(name, type, configuration, enabled);
        }

        public UpdateResourceCommand toCommand() {
            return new UpdateResourceCommand(name, type, configuration, enabled);
        }
    }

    public static Resource aResource() {
        return aResource(r -> r);
    }

    public static Resource aResource(UnaryOperator<Resource.ResourceBuilder> customizer) {
        var builder = Resource.builder()
            .id(DEFAULT_ID)
            .referenceId(DEFAULT_ENVIRONMENT_ID)
            .referenceType(Resource.ReferenceType.ENVIRONMENT)
            .definition(aDefinition())
            .createdAt(TimeProvider.instantNow())
            .updatedAt(TimeProvider.instantNow());
        return customizer.apply(builder).build();
    }
}
