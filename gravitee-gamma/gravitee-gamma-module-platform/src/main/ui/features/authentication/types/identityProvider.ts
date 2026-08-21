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

export type IdentityProviderType = 'GOOGLE' | 'GITHUB' | 'GRAVITEEIO_AM' | 'OIDC';

export interface IdentityProviderListItem {
    id: string;
    name: string;
    description: string;
    enabled: boolean;
    sync: boolean;
    type: IdentityProviderType;
    created_at: number;
    updated_at: number;
}

export interface IdentityProviderActivation {
    identityProvider: string;
    referenceId?: string;
    referenceType?: 'ENVIRONMENT' | 'ORGANIZATION';
    created_at?: number;
}

export interface IdentityProviderRow extends IdentityProviderListItem {
    /** Present once activations have loaded; omitted when activation state is unknown. */
    activated?: boolean;
}

export interface IdentityProviderUserProfileMapping {
    id: string;
    firstname?: string;
    lastname?: string;
    email?: string;
    picture?: string;
}

export interface IdentityProviderConfiguration {
    clientId: string;
    clientSecret: string;
    serverURL?: string;
    domain?: string;
    scopes?: string[];
    color?: string;
    tokenEndpoint?: string;
    tokenIntrospectionEndpoint?: string;
    authorizeEndpoint?: string;
    userInfoEndpoint?: string;
    userLogoutEndpoint?: string;
}

export interface GroupMapping {
    condition: string;
    groups: string[];
}

export interface RoleMapping {
    condition: string;
    organizations: string[];
    environments: Record<string, string[]>;
}

export interface NewIdentityProviderPayload {
    name: string;
    description?: string;
    type: IdentityProviderType;
    enabled: boolean;
    emailRequired: boolean;
    syncMappings: boolean;
    configuration: IdentityProviderConfiguration;
    userProfileMapping?: IdentityProviderUserProfileMapping;
}

export interface UpdateIdentityProviderPayload {
    name: string;
    description?: string;
    enabled: boolean;
    emailRequired: boolean;
    syncMappings: boolean;
    configuration: IdentityProviderConfiguration;
    groupMappings: GroupMapping[];
    roleMappings: RoleMapping[];
    userProfileMapping?: IdentityProviderUserProfileMapping;
}

export interface IdentityProvider {
    id: string;
    name: string;
    description?: string;
    type: IdentityProviderType;
    enabled: boolean;
    configuration?: IdentityProviderConfiguration;
    userProfileMapping?: IdentityProviderUserProfileMapping;
    groupMappings: GroupMapping[];
    roleMappings: RoleMapping[];
    emailRequired?: boolean;
    syncMappings?: boolean;
}
