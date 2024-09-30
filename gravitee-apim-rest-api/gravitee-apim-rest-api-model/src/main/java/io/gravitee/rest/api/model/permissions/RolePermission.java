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
package io.gravitee.rest.api.model.permissions;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Schema(enumAsRef = true)
public enum RolePermission {
    API_DEFINITION(RoleScope.API, ApiPermission.DEFINITION),
    API_PLAN(RoleScope.API, ApiPermission.PLAN),
    API_SUBSCRIPTION(RoleScope.API, ApiPermission.SUBSCRIPTION),
    API_MEMBER(RoleScope.API, ApiPermission.MEMBER),
    API_METADATA(RoleScope.API, ApiPermission.METADATA),
    API_ANALYTICS(RoleScope.API, ApiPermission.ANALYTICS),
    API_EVENT(RoleScope.API, ApiPermission.EVENT),
    API_HEALTH(RoleScope.API, ApiPermission.HEALTH),
    API_LOG(RoleScope.API, ApiPermission.LOG),
    API_DOCUMENTATION(RoleScope.API, ApiPermission.DOCUMENTATION),
    API_GATEWAY_DEFINITION(RoleScope.API, ApiPermission.GATEWAY_DEFINITION),
    API_AUDIT(RoleScope.API, ApiPermission.AUDIT),
    API_RATING(RoleScope.API, ApiPermission.RATING),
    API_RATING_ANSWER(RoleScope.API, ApiPermission.RATING_ANSWER),
    API_NOTIFICATION(RoleScope.API, ApiPermission.NOTIFICATION),
    API_MESSAGE(RoleScope.API, ApiPermission.MESSAGE),
    API_ALERT(RoleScope.API, ApiPermission.ALERT),
    API_RESPONSE_TEMPLATES(RoleScope.API, ApiPermission.RESPONSE_TEMPLATES),
    API_REVIEWS(RoleScope.API, ApiPermission.REVIEWS),
    API_QUALITY_RULE(RoleScope.API, ApiPermission.QUALITY_RULE),

    APPLICATION_DEFINITION(RoleScope.APPLICATION, ApplicationPermission.DEFINITION),
    APPLICATION_MEMBER(RoleScope.APPLICATION, ApplicationPermission.MEMBER),
    APPLICATION_ANALYTICS(RoleScope.APPLICATION, ApplicationPermission.ANALYTICS),
    APPLICATION_LOG(RoleScope.APPLICATION, ApplicationPermission.LOG),
    APPLICATION_SUBSCRIPTION(RoleScope.APPLICATION, ApplicationPermission.SUBSCRIPTION),
    APPLICATION_NOTIFICATION(RoleScope.APPLICATION, ApplicationPermission.NOTIFICATION),
    APPLICATION_ALERT(RoleScope.APPLICATION, ApiPermission.ALERT),
    APPLICATION_METADATA(RoleScope.APPLICATION, ApiPermission.METADATA),

    GROUP_MEMBER(RoleScope.GROUP, GroupPermission.MEMBER),
    GROUP_INVITATION(RoleScope.GROUP, GroupPermission.INVITATION),

    ENVIRONMENT_INSTANCE(RoleScope.ENVIRONMENT, EnvironmentPermission.INSTANCE),
    ENVIRONMENT_GROUP(RoleScope.ENVIRONMENT, EnvironmentPermission.GROUP),
    ENVIRONMENT_TAG(RoleScope.ENVIRONMENT, EnvironmentPermission.TAG),
    ENVIRONMENT_TENANT(RoleScope.ENVIRONMENT, EnvironmentPermission.TENANT),
    ENVIRONMENT_API(RoleScope.ENVIRONMENT, EnvironmentPermission.API),
    ENVIRONMENT_APPLICATION(RoleScope.ENVIRONMENT, EnvironmentPermission.APPLICATION),
    ENVIRONMENT_PLATFORM(RoleScope.ENVIRONMENT, EnvironmentPermission.PLATFORM),
    ENVIRONMENT_AUDIT(RoleScope.ENVIRONMENT, EnvironmentPermission.AUDIT),
    ENVIRONMENT_NOTIFICATION(RoleScope.ENVIRONMENT, EnvironmentPermission.NOTIFICATION),
    ENVIRONMENT_MESSAGE(RoleScope.ENVIRONMENT, EnvironmentPermission.MESSAGE),
    ENVIRONMENT_DICTIONARY(RoleScope.ENVIRONMENT, EnvironmentPermission.DICTIONARY),
    ENVIRONMENT_ALERT(RoleScope.ENVIRONMENT, EnvironmentPermission.ALERT),
    ENVIRONMENT_ENTRYPOINT(RoleScope.ENVIRONMENT, EnvironmentPermission.ENTRYPOINT),
    ENVIRONMENT_SETTINGS(RoleScope.ENVIRONMENT, EnvironmentPermission.SETTINGS),
    ENVIRONMENT_QUALITY_RULE(RoleScope.ENVIRONMENT, EnvironmentPermission.QUALITY_RULE),
    ENVIRONMENT_DASHBOARD(RoleScope.ENVIRONMENT, EnvironmentPermission.DASHBOARD),
    ENVIRONMENT_METADATA(RoleScope.ENVIRONMENT, EnvironmentPermission.METADATA),
    ENVIRONMENT_DOCUMENTATION(RoleScope.ENVIRONMENT, EnvironmentPermission.DOCUMENTATION),
    ENVIRONMENT_CATEGORY(RoleScope.ENVIRONMENT, EnvironmentPermission.CATEGORY),
    ENVIRONMENT_TOP_APIS(RoleScope.ENVIRONMENT, EnvironmentPermission.TOP_APIS),
    ENVIRONMENT_API_HEADER(RoleScope.ENVIRONMENT, EnvironmentPermission.API_HEADER),
    ENVIRONMENT_CLIENT_REGISTRATION_PROVIDER(RoleScope.ENVIRONMENT, EnvironmentPermission.CLIENT_REGISTRATION_PROVIDER),
    ENVIRONMENT_THEME(RoleScope.ENVIRONMENT, EnvironmentPermission.THEME),
    ENVIRONMENT_IDENTITY_PROVIDER_ACTIVATION(RoleScope.ENVIRONMENT, EnvironmentPermission.IDENTITY_PROVIDER_ACTIVATION),
    ENVIRONMENT_INTEGRATION(RoleScope.ENVIRONMENT, EnvironmentPermission.INTEGRATION),

    ORGANIZATION_USERS(RoleScope.ORGANIZATION, OrganizationPermission.USER),
    ORGANIZATION_USERS_TOKEN(RoleScope.ORGANIZATION, OrganizationPermission.USER_TOKEN),
    ORGANIZATION_ROLE(RoleScope.ORGANIZATION, OrganizationPermission.ROLE),
    ORGANIZATION_ENVIRONMENT(RoleScope.ORGANIZATION, OrganizationPermission.ENVIRONMENT),
    ORGANIZATION_CUSTOM_USER_FIELDS(RoleScope.ORGANIZATION, OrganizationPermission.CUSTOM_USER_FIELDS),
    ORGANIZATION_IDENTITY_PROVIDER(RoleScope.ORGANIZATION, OrganizationPermission.IDENTITY_PROVIDER),
    ORGANIZATION_IDENTITY_PROVIDER_ACTIVATION(RoleScope.ORGANIZATION, OrganizationPermission.IDENTITY_PROVIDER_ACTIVATION),
    ORGANIZATION_NOTIFICATION_TEMPLATES(RoleScope.ORGANIZATION, OrganizationPermission.NOTIFICATION_TEMPLATES),
    ORGANIZATION_SETTINGS(RoleScope.ORGANIZATION, OrganizationPermission.SETTINGS),
    ORGANIZATION_INSTALLATION(RoleScope.ORGANIZATION, OrganizationPermission.INSTALLATION),
    ORGANIZATION_TAG(RoleScope.ORGANIZATION, OrganizationPermission.TAG),
    ORGANIZATION_TENANT(RoleScope.ORGANIZATION, OrganizationPermission.TENANT),
    ORGANIZATION_ENTRYPOINT(RoleScope.ORGANIZATION, OrganizationPermission.ENTRYPOINT),
    ORGANIZATION_POLICIES(RoleScope.ORGANIZATION, OrganizationPermission.POLICIES),
    ORGANIZATION_AUDIT(RoleScope.ORGANIZATION, OrganizationPermission.AUDIT),
    ORGANIZATION_LICENSE_MANAGEMENT(RoleScope.ORGANIZATION, OrganizationPermission.LICENSE_MANAGEMENT);

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
