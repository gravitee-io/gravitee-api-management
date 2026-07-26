/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import type { PortalTenantFeatures } from '../../tenants/types/portal-tenant.types';

/** Consumers either see an asset or may also subscribe to it. They never author it. */
export type PortalAccessLevel = 'VIEW' | 'CONSUME';

/**
 * `CLASSIC` sends the consumer through application → plan → subscription.
 * `AUTO` subscribes each member's default application to a default plan and issues credentials on first access.
 */
export type ConsumeProvisioning = 'CLASSIC' | 'AUTO';

export type PortalGrantScopeType = 'PORTAL' | 'API' | 'API_PRODUCT' | 'AI_WORKSPACE';

export type PortalGroupManagementMode = 'SELF_MANAGED' | 'PLATFORM_MANAGED';

export type PortalGroupMemberRole = 'admin' | 'member';

export interface PortalGroup {
    id: string;
    tenantId: string;
    name: string;
    hrid: string;
    description?: string;
    managementMode: PortalGroupManagementMode;
    /** Portal capabilities available to members of this group (same shape as tenant features). */
    features: PortalTenantFeatures;
    createdAt: string;
    updatedAt: string;
}

export interface PortalGroupMember {
    id: string;
    groupId: string;
    tenantId: string;
    /** References a `PortalTenantMember` id so the tenant directory stays the source of truth. */
    memberId: string;
    role: PortalGroupMemberRole;
}

/** A group member joined with its tenant-directory identity, ready for rendering. */
export interface PortalGroupMemberView extends PortalGroupMember {
    userId: string;
    displayName: string;
    email: string;
}

/** Navigation items inherit their asset's access; only exceptions are stored. */
export interface PortalNavigationOverride {
    navigationItemId: string;
    portalId: string;
    access: PortalAccessLevel | 'NONE';
}

export interface PortalAccessGrant {
    id: string;
    groupId: string;
    tenantId: string;
    scopeType: PortalGrantScopeType;
    /** Portal id for `PORTAL` scopes, asset id otherwise. */
    scopeId: string;
    access: PortalAccessLevel;
    provisioning?: ConsumeProvisioning;
    defaultPlanId?: string;
    overrides: PortalNavigationOverride[];
    createdAt: string;
    updatedAt: string;
}

export interface PortalAccessGrantInput {
    groupId: string;
    tenantId: string;
    scopeType: PortalGrantScopeType;
    scopeId: string;
    access: PortalAccessLevel;
    provisioning?: ConsumeProvisioning;
    defaultPlanId?: string;
}

export type PortalAccessGrantPatch = Partial<
    Pick<PortalAccessGrant, 'access' | 'provisioning' | 'defaultPlanId' | 'overrides'>
>;

export const PORTAL_ACCESS_LEVEL_LABELS: Record<PortalAccessLevel, string> = {
    VIEW: 'View',
    CONSUME: 'Consume',
};

export const PORTAL_GRANT_SCOPE_TYPE_LABELS: Record<PortalGrantScopeType, string> = {
    PORTAL: 'Portal',
    API: 'API',
    API_PRODUCT: 'Product',
    AI_WORKSPACE: 'AI',
};

export const PORTAL_GROUP_MANAGEMENT_MODE_LABELS: Record<
    PortalGroupManagementMode,
    { short: string; long: string }
> = {
    SELF_MANAGED: { short: 'Self', long: 'Self-managed' },
    PLATFORM_MANAGED: { short: 'Plat', long: 'Platform-managed' },
};

/** Roles a console principal can hold on portal content it authors. */
export type ConsoleDocRole = 'READER' | 'AUTHOR' | 'OWNER';

export type ConsolePrincipalType = 'USER' | 'TEAM';

export interface ConsolePrincipal {
    id: string;
    type: ConsolePrincipalType;
    name: string;
    email?: string;
}

export interface ConsoleDocGrant {
    id: string;
    principalType: ConsolePrincipalType;
    principalId: string;
    scopeType: PortalGrantScopeType;
    scopeId: string;
    role: ConsoleDocRole;
    createdAt: string;
    updatedAt: string;
}

export interface ConsoleDocGrantInput {
    principalType: ConsolePrincipalType;
    principalId: string;
    scopeType: PortalGrantScopeType;
    scopeId: string;
    role: ConsoleDocRole;
}

export const CONSOLE_DOC_ROLES: readonly ConsoleDocRole[] = ['READER', 'AUTHOR', 'OWNER'];

export const CONSOLE_DOC_ROLE_LABELS: Record<ConsoleDocRole, { title: string; description: string }> = {
    READER: { title: 'Reader', description: 'Can read documentation and access grants' },
    AUTHOR: { title: 'Author', description: 'Can create and edit documentation pages' },
    OWNER: { title: 'Owner', description: 'Can edit documentation and manage who else can' },
};
