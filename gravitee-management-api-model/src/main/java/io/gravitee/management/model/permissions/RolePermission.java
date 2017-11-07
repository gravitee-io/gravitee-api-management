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
package io.gravitee.management.model.permissions;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public enum RolePermission {
    MANAGEMENT_INSTANCE     (RoleScope.MANAGEMENT, ManagementPermission.INSTANCE),
    MANAGEMENT_GROUP        (RoleScope.MANAGEMENT, ManagementPermission.GROUP),
    MANAGEMENT_TAG          (RoleScope.MANAGEMENT, ManagementPermission.TAG),
    MANAGEMENT_TENANT       (RoleScope.MANAGEMENT, ManagementPermission.TENANT),
    MANAGEMENT_API          (RoleScope.MANAGEMENT, ManagementPermission.API),
    MANAGEMENT_ROLE         (RoleScope.MANAGEMENT, ManagementPermission.ROLE),
    MANAGEMENT_APPLICATION  (RoleScope.MANAGEMENT, ManagementPermission.APPLICATION),
    MANAGEMENT_PLATFORM     (RoleScope.MANAGEMENT, ManagementPermission.PLATFORM),

    PORTAL_METADATA         (RoleScope.PORTAL, PortalPermission.METADATA),
    PORTAL_DOCUMENTATION    (RoleScope.PORTAL, PortalPermission.DOCUMENTATION),
    PORTAL_APPLICATION      (RoleScope.PORTAL, PortalPermission.APPLICATION),
    PORTAL_VIEW             (RoleScope.PORTAL, PortalPermission.VIEW),

    API_DEFINITION          (RoleScope.API, ApiPermission.DEFINITION),
    API_PLAN                (RoleScope.API, ApiPermission.PLAN),
    API_SUBSCRIPTION        (RoleScope.API, ApiPermission.SUBSCRIPTION),
    API_MEMBER              (RoleScope.API, ApiPermission.MEMBER),
    API_METADATA            (RoleScope.API, ApiPermission.METADATA),
    API_ANALYTICS           (RoleScope.API, ApiPermission.ANALYTICS),
    API_EVENT               (RoleScope.API, ApiPermission.EVENT),
    API_HEALTH              (RoleScope.API, ApiPermission.HEALTH),
    API_LOG                 (RoleScope.API, ApiPermission.LOG),
    API_DOCUMENTATION       (RoleScope.API, ApiPermission.DOCUMENTATION),
    API_GATEWAY_DEFINITION  (RoleScope.API, ApiPermission.GATEWAY_DEFINITION),
    API_RATING              (RoleScope.API, ApiPermission.RATING),
    API_RATING_ANSWER       (RoleScope.API, ApiPermission.RATING_ANSWER),

    APPLICATION_DEFINITION  (RoleScope.APPLICATION, ApplicationPermission.DEFINITION),
    APPLICATION_MEMBER      (RoleScope.APPLICATION, ApplicationPermission.MEMBER),
    APPLICATION_ANALYTICS   (RoleScope.APPLICATION, ApplicationPermission.ANALYTICS),
    APPLICATION_LOG         (RoleScope.APPLICATION, ApplicationPermission.LOG),
    APPLICATION_SUBSCRIPTION(RoleScope.APPLICATION, ApplicationPermission.SUBSCRIPTION);


    RoleScope scope;
    Permission permission;

    RolePermission(RoleScope scope, Permission permission) {
        this.scope = scope;
        this.permission = permission;
    }

    public RoleScope getScope() {
        return scope;
    }

    public void setScope(RoleScope scope) {
        this.scope = scope;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}
