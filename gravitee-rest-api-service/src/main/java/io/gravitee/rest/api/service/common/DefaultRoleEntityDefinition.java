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
package io.gravitee.rest.api.service.common;

import io.gravitee.common.util.Maps;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.ApplicationPermission;
import io.gravitee.rest.api.model.permissions.ManagementPermission;
import io.gravitee.rest.api.model.permissions.PortalPermission;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;
import static io.gravitee.rest.api.model.permissions.RoleScope.*;

public interface DefaultRoleEntityDefinition {

    public static final NewRoleEntity ROLE_MANAGEMENT_API_PUBLISHER = new NewRoleEntity(
            "API_PUBLISHER",
            "Management Role. Created by Gravitee.io.",
            MANAGEMENT,
            false,
            Maps.<String, char[]>builder()
                .put(ManagementPermission.API.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ManagementPermission.APPLICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ManagementPermission.TAG.getName(), new char[]{READ.getId()})
                .put(ManagementPermission.GROUP.getName(), new char[]{READ.getId()})
                .put(ManagementPermission.TENANT.getName(), new char[]{READ.getId()})
                .put(ManagementPermission.ROLE.getName(), new char[]{READ.getId()})
                .put(ManagementPermission.PLATFORM.getName(), new char[]{READ.getId()})
                .build()
            );
    
    public static final NewRoleEntity DEFAULT_ROLE_MANAGEMENT_USER = new NewRoleEntity(
            "USER",
            "Default Management Role. Created by Gravitee.io.",
            MANAGEMENT,
            true,
            Maps.<String, char[]>builder()
                .put(ManagementPermission.API.getName(), new char[]{READ.getId()})
                .put(ManagementPermission.APPLICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ManagementPermission.ROLE.getName(), new char[]{READ.getId()})
                .put(ManagementPermission.GROUP.getName(), new char[]{READ.getId()})
                .build()
            );
    
    public static final NewRoleEntity DEFAULT_ROLE_PORTAL_USER = new NewRoleEntity(
            "USER",
            "Default Portal Role. Created by Gravitee.io.",
            PORTAL,
            true,
            Maps.<String, char[]>builder()
                .put(PortalPermission.APPLICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(PortalPermission.DOCUMENTATION.getName(), new char[]{READ.getId()})
                .build()
            );
    
    public static final NewRoleEntity DEFAULT_ROLE_API_USER = new NewRoleEntity(
            "USER",
            "Default API Role. Created by Gravitee.io.",
            API,
            true,
            Maps.<String, char[]>builder()
                .put(ApiPermission.DEFINITION.getName(), new char[]{READ.getId()})
                .put(ApiPermission.PLAN.getName(), new char[]{READ.getId()})
                .put(ApiPermission.SUBSCRIPTION.getName(), new char[]{READ.getId()})
                .put(ApiPermission.MEMBER.getName(), new char[]{READ.getId()})
                .put(ApiPermission.METADATA.getName(), new char[]{READ.getId()})
                .put(ApiPermission.EVENT.getName(), new char[]{READ.getId()})
                .put(ApiPermission.DOCUMENTATION.getName(), new char[]{READ.getId()})
                .put(ApiPermission.RATING.getName(), new char[]{CREATE.getId(), READ.getId()})
                .build()
            );
    
    public static final NewRoleEntity ROLE_API_OWNER = new NewRoleEntity(
            "OWNER",
            "API Role. Created by Gravitee.io.",
            API,
            false,
            Maps.<String, char[]>builder()
                .put(ApiPermission.DEFINITION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.GATEWAY_DEFINITION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.PLAN.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.SUBSCRIPTION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.MEMBER.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.METADATA.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.ANALYTICS.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.EVENT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.HEALTH.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.LOG.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.DOCUMENTATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.AUDIT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.RATING.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.RATING_ANSWER.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.DISCOVERY.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.NOTIFICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApiPermission.ALERT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .build()
            );

    public static final NewRoleEntity ROLE_API_REVIEWER = new NewRoleEntity(
            "REVIEWER",
            "API Role. Created by Gravitee.io.",
            API,
            false,
            Maps.<String, char[]>builder()
                .put(ApiPermission.DEFINITION.getName(), new char[]{READ.getId()})
                .put(ApiPermission.GATEWAY_DEFINITION.getName(), new char[]{READ.getId()})
                .put(ApiPermission.PLAN.getName(), new char[]{READ.getId()})
                .put(ApiPermission.METADATA.getName(), new char[]{READ.getId()})
                .put(ApiPermission.DOCUMENTATION.getName(), new char[]{READ.getId()})
                .put(ApiPermission.DISCOVERY.getName(), new char[]{READ.getId()})
                .put(ApiPermission.NOTIFICATION.getName(), new char[]{READ.getId(), UPDATE.getId()})
                .put(ApiPermission.ALERT.getName(), new char[]{READ.getId()})
                .put(ApiPermission.REVIEWS.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .build()
            );
    
    public static final NewRoleEntity DEFAULT_ROLE_APPLICATION_USER = new NewRoleEntity(
            "USER",
            "Default Application Role. Created by Gravitee.io.",
            APPLICATION,
            true,
            Maps.<String, char[]>builder()
                .put(ApplicationPermission.DEFINITION.getName(), new char[]{READ.getId()})
                .put(ApplicationPermission.MEMBER.getName(), new char[]{READ.getId()})
                .put(ApplicationPermission.ANALYTICS.getName(), new char[]{READ.getId()})
                .put(ApplicationPermission.LOG.getName(), new char[]{READ.getId()})
                .put(ApplicationPermission.SUBSCRIPTION.getName(), new char[]{CREATE.getId(), READ.getId()})
                .build()
            );
    
    public static final NewRoleEntity ROLE_APPLICATION_OWNER = new NewRoleEntity(
            "OWNER",
            "Application Role. Created by Gravitee.io.",
            APPLICATION,
            false,
            Maps.<String, char[]>builder()
                .put(ApplicationPermission.DEFINITION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApplicationPermission.MEMBER.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApplicationPermission.ANALYTICS.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApplicationPermission.LOG.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApplicationPermission.SUBSCRIPTION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApplicationPermission.NOTIFICATION.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .put(ApplicationPermission.ALERT.getName(), new char[]{CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId()})
                .build()
            );
}
