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
    MANAGEMENT_AUDIT        (RoleScope.MANAGEMENT, ManagementPermission.AUDIT),
    MANAGEMENT_NOTIFICATION (RoleScope.MANAGEMENT, ManagementPermission.NOTIFICATION),
    MANAGEMENT_USERS        (RoleScope.MANAGEMENT, ManagementPermission.USER),
    MANAGEMENT_MESSAGE      (RoleScope.MANAGEMENT, ManagementPermission.MESSAGE),
    MANAGEMENT_DICTIONARY   (RoleScope.MANAGEMENT, ManagementPermission.DICTIONARY),
    MANAGEMENT_ALERT        (RoleScope.MANAGEMENT, ManagementPermission.ALERT),
    MANAGEMENT_ENTRYPOINT   (RoleScope.MANAGEMENT, ManagementPermission.ENTRYPOINT),
    MANAGEMENT_SETTINGS     (RoleScope.MANAGEMENT, ManagementPermission.SETTINGS),
    MANAGEMENT_QUALITY_RULE (RoleScope.MANAGEMENT, ManagementPermission.QUALITY_RULE),
    MANAGEMENT_DASHBOARD    (RoleScope.MANAGEMENT, ManagementPermission.DASHBOARD),

    PORTAL_METADATA         (RoleScope.PORTAL, PortalPermission.METADATA),
    PORTAL_DOCUMENTATION    (RoleScope.PORTAL, PortalPermission.DOCUMENTATION),
    PORTAL_APPLICATION      (RoleScope.PORTAL, PortalPermission.APPLICATION),
    PORTAL_VIEW             (RoleScope.PORTAL, PortalPermission.VIEW),
    PORTAL_TOP_APIS         (RoleScope.PORTAL, PortalPermission.TOP_APIS),
    PORTAL_SETTINGS         (RoleScope.PORTAL, PortalPermission.SETTINGS),
    PORTAL_API_HEADER       (RoleScope.PORTAL, PortalPermission.API_HEADER),
    PORTAL_IDENTITY_PROVIDER(RoleScope.PORTAL, PortalPermission.IDENTITY_PROVIDER),
    PORTAL_CLIENT_REGISTRATION_PROVIDER(RoleScope.PORTAL, PortalPermission.CLIENT_REGISTRATION_PROVIDER),

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
    API_AUDIT               (RoleScope.API, ApiPermission.AUDIT),
    API_RATING              (RoleScope.API, ApiPermission.RATING),
    API_RATING_ANSWER       (RoleScope.API, ApiPermission.RATING_ANSWER),
    API_NOTIFICATION        (RoleScope.API, ApiPermission.NOTIFICATION),
    API_MESSAGE             (RoleScope.API, ApiPermission.MESSAGE),
    API_ALERT               (RoleScope.API, ApiPermission.ALERT),
    API_RESPONSE_TEMPLATES  (RoleScope.API, ApiPermission.RESPONSE_TEMPLATES),
    API_REVIEWS             (RoleScope.API, ApiPermission.REVIEWS),
    API_QUALITY_RULE        (RoleScope.API, ApiPermission.QUALITY_RULE),

    APPLICATION_DEFINITION  (RoleScope.APPLICATION, ApplicationPermission.DEFINITION),
    APPLICATION_MEMBER      (RoleScope.APPLICATION, ApplicationPermission.MEMBER),
    APPLICATION_ANALYTICS   (RoleScope.APPLICATION, ApplicationPermission.ANALYTICS),
    APPLICATION_LOG         (RoleScope.APPLICATION, ApplicationPermission.LOG),
    APPLICATION_SUBSCRIPTION(RoleScope.APPLICATION, ApplicationPermission.SUBSCRIPTION),
    APPLICATION_NOTIFICATION(RoleScope.APPLICATION, ApplicationPermission.NOTIFICATION),
    APPLICATION_ALERT       (RoleScope.APPLICATION, ApiPermission.ALERT),
    APPLICATION_METADATA    (RoleScope.APPLICATION, ApiPermission.METADATA),

    GROUP_MEMBER            (RoleScope.GROUP, GroupPermission.MEMBER),
    GROUP_INVITATION        (RoleScope.GROUP, GroupPermission.INVITATION);


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
