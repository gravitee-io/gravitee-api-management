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
package io.gravitee.apim.core.notification.model.config;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.HookScope;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class NotificationConfig {

    private Type type;
    private String id;
    private String name;
    private String referenceType;
    private String referenceId;
    private String notifier;
    private String config;
    private List<String> hooks;
    private boolean useSystemProxy;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    public enum Type {
        PORTAL,
        GENERIC,
    }

    public static NotificationConfig defaultMailNotificationConfigFor(String apiId) {
        var now = TimeProvider.now();
        return NotificationConfig.builder()
            .type(Type.GENERIC)
            .id(UuidString.generateRandom())
            .name("Default Mail Notifications")
            .referenceType(HookScope.API.name())
            .referenceId(apiId)
            .notifier("default-email")
            .config("${(api.primaryOwner.email)!''}")
            .hooks(Arrays.stream(ApiHook.values()).map(Enum::name).sorted().toList())
            .createdAt(now)
            .updatedAt(now)
            .build();
    }
}
