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
import { savePortalAccessGrant } from './portal-access-grants.storage';
import { savePortalGroupMember } from './portal-group-members.storage';
import { getAllPortalGroups, savePortalGroup } from './portal-groups.storage';
import { DUMMY_PORTAL_USERS } from '../../tenants/storage/dummy-portal-users';
import {
    getMembersByTenantId,
    savePortalTenantMember,
} from '../../tenants/storage/portal-tenant-members.storage';
import { getAllPortalTenants, savePortalTenant } from '../../tenants/storage/portal-tenants.storage';
import {
    DEFAULT_PORTAL_TENANT_FEATURES,
    type PortalTenant,
    type PortalTenantManagementMode,
    type PortalTenantMember,
} from '../../tenants/types/portal-tenant.types';
import type {
    PortalAccessGrant,
    PortalGroup,
    PortalGroupManagementMode,
    PortalGroupMember,
} from '../types/permissions.types';

const SEED_CREATED_AT = '2026-01-10T09:00:00.000Z';
const SEED_UPDATED_AT = '2026-06-15T14:30:00.000Z';

interface TenantSeed {
    id: string;
    name: string;
    hrid: string;
    description: string;
    managementMode: PortalTenantManagementMode;
    /** Tenant directory drawn from the shared dummy user list. */
    userIds: readonly string[];
}

interface GroupSeed {
    id: string;
    tenantId: string;
    name: string;
    description: string;
    managementMode: PortalGroupManagementMode;
    /** User ids that belong to the group; the first one becomes a group admin. */
    userIds: readonly string[];
    adminUserIds?: readonly string[];
}

const ACME_TENANT_ID = 'tenant-acme';

const ACME_USER_IDS = [
    'user-alice',
    'user-bob',
    'user-carol',
    'user-dan',
    'user-eve',
    'user-frank',
    'user-grace',
    'user-henry',
] as const;

/**
 * Acme Corp normally comes from the payments portal tenant seed. It is repeated here so the screen
 * still has data when no portal has been seeded yet, as in the platform module or in tests.
 */
const ACME_TENANT_SEED: TenantSeed = {
    id: ACME_TENANT_ID,
    name: 'Acme Corp',
    hrid: 'acme-corp',
    description: 'Payment integration partner',
    managementMode: 'DELEGATED',
    userIds: ACME_USER_IDS,
};

const TENANT_SEEDS: readonly TenantSeed[] = [
    {
        id: 'tenant-emea',
        name: 'EMEA Org',
        hrid: 'emea-org',
        description: 'European partners consuming the payments and billing APIs',
        managementMode: 'DELEGATED',
        userIds: ['user-carol', 'user-dan', 'user-eve'],
    },
    {
        id: 'tenant-apac',
        name: 'APAC Division',
        hrid: 'apac-division',
        description: 'Regional division managed centrally by the platform team',
        managementMode: 'CENTRAL',
        userIds: ['user-frank', 'user-grace'],
    },
    {
        id: 'tenant-na',
        name: 'NA Enterprise',
        hrid: 'na-enterprise',
        description: 'Self-service enterprise accounts in North America',
        managementMode: 'SELF_MANAGED',
        userIds: ['user-henry', 'user-alice', 'user-bob'],
    },
];

const GROUP_SEEDS: readonly GroupSeed[] = [
    {
        id: 'group-acme-backend-devs',
        tenantId: ACME_TENANT_ID,
        name: 'backend-devs',
        description: 'Backend engineers integrating the payment rails',
        managementMode: 'SELF_MANAGED',
        userIds: ['user-alice', 'user-bob', 'user-carol', 'user-dan'],
        adminUserIds: ['user-alice', 'user-bob'],
    },
    {
        id: 'group-acme-api-consumers',
        tenantId: ACME_TENANT_ID,
        name: 'api-consumers',
        description: 'Application teams subscribing to published APIs',
        managementMode: 'SELF_MANAGED',
        userIds: ['user-eve', 'user-frank', 'user-grace'],
        adminUserIds: ['user-eve'],
    },
    {
        id: 'group-acme-read-only-audit',
        tenantId: ACME_TENANT_ID,
        name: 'read-only-audit',
        description: 'Auditors with view-only access to documentation',
        managementMode: 'PLATFORM_MANAGED',
        userIds: ['user-henry'],
    },
    {
        id: 'group-acme-service-accounts',
        tenantId: ACME_TENANT_ID,
        name: 'service-accounts',
        description: 'Machine identities provisioned by the platform team',
        managementMode: 'PLATFORM_MANAGED',
        userIds: ['user-bob', 'user-grace'],
    },
    {
        id: 'group-emea-partner-devs',
        tenantId: 'tenant-emea',
        name: 'partner-devs',
        description: 'Partner developers onboarded through the EMEA portal',
        managementMode: 'SELF_MANAGED',
        userIds: ['user-carol', 'user-dan'],
        adminUserIds: ['user-carol'],
    },
    {
        id: 'group-emea-support',
        tenantId: 'tenant-emea',
        name: 'support-team',
        description: 'Support engineers troubleshooting partner integrations',
        managementMode: 'PLATFORM_MANAGED',
        userIds: ['user-eve'],
    },
    {
        id: 'group-apac-integrators',
        tenantId: 'tenant-apac',
        name: 'integrators',
        description: 'Systems integrators working on regional rollouts',
        managementMode: 'PLATFORM_MANAGED',
        userIds: ['user-frank', 'user-grace'],
        adminUserIds: ['user-frank'],
    },
    {
        id: 'group-na-platform-team',
        tenantId: 'tenant-na',
        name: 'platform-team',
        description: 'Enterprise platform owners managing their own workspace',
        managementMode: 'SELF_MANAGED',
        userIds: ['user-henry', 'user-alice'],
        adminUserIds: ['user-henry'],
    },
];

function grantSeeds(): PortalAccessGrant[] {
    const base = { tenantId: ACME_TENANT_ID, createdAt: SEED_CREATED_AT, updatedAt: SEED_UPDATED_AT };

    return [
        {
            ...base,
            id: 'grant-backend-devs-payments',
            groupId: 'group-acme-backend-devs',
            scopeType: 'API',
            scopeId: 'api-payments',
            access: 'CONSUME',
            provisioning: 'AUTO',
            defaultPlanId: 'plan-payments-key',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-backend-devs-accounts',
            groupId: 'group-acme-backend-devs',
            scopeType: 'API',
            scopeId: 'api-accounts',
            access: 'CONSUME',
            provisioning: 'CLASSIC',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-backend-devs-commerce',
            groupId: 'group-acme-backend-devs',
            scopeType: 'API_PRODUCT',
            scopeId: 'product-commerce',
            access: 'VIEW',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-backend-devs-ai',
            groupId: 'group-acme-backend-devs',
            scopeType: 'AI_WORKSPACE',
            scopeId: 'ai-workspace-enterprise',
            access: 'CONSUME',
            provisioning: 'AUTO',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-api-consumers-portal',
            groupId: 'group-acme-api-consumers',
            scopeType: 'PORTAL',
            scopeId: 'portal-payments',
            access: 'VIEW',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-api-consumers-payments',
            groupId: 'group-acme-api-consumers',
            scopeType: 'API',
            scopeId: 'api-payments',
            access: 'CONSUME',
            provisioning: 'CLASSIC',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-read-only-audit-portal',
            groupId: 'group-acme-read-only-audit',
            scopeType: 'PORTAL',
            scopeId: 'portal-payments',
            access: 'VIEW',
            overrides: [],
        },
        {
            ...base,
            id: 'grant-service-accounts-ai',
            groupId: 'group-acme-service-accounts',
            scopeType: 'AI_WORKSPACE',
            scopeId: 'ai-workspace-enterprise',
            access: 'CONSUME',
            provisioning: 'AUTO',
            overrides: [],
        },
        {
            ...base,
            tenantId: 'tenant-emea',
            id: 'grant-partner-devs-billing',
            groupId: 'group-emea-partner-devs',
            scopeType: 'API',
            scopeId: 'api-billing',
            access: 'CONSUME',
            provisioning: 'CLASSIC',
            overrides: [],
        },
        {
            ...base,
            tenantId: 'tenant-apac',
            id: 'grant-integrators-orders',
            groupId: 'group-apac-integrators',
            scopeType: 'API',
            scopeId: 'api-orders',
            access: 'VIEW',
            overrides: [],
        },
    ];
}

function createTenant(seed: TenantSeed): PortalTenant {
    return {
        id: seed.id,
        name: seed.name,
        hrid: seed.hrid,
        description: seed.description,
        allowedApiIds: [],
        apiAccessMode: 'all',
        features: DEFAULT_PORTAL_TENANT_FEATURES,
        managementMode: seed.managementMode,
        createdAt: SEED_CREATED_AT,
        updatedAt: SEED_UPDATED_AT,
    };
}

function tenantMemberId(tenantId: string, userId: string): string {
    return `${tenantId}-${userId}`;
}

async function ensureTenantMembers(tenantId: string, userIds: readonly string[]): Promise<void> {
    const existing = await getMembersByTenantId(tenantId);
    const existingUserIds = new Set(existing.map(member => member.userId));

    const missing = userIds.flatMap(userId => {
        if (existingUserIds.has(userId)) {
            return [];
        }

        const user = DUMMY_PORTAL_USERS.find(candidate => candidate.id === userId);
        if (!user) {
            return [];
        }

        const member: PortalTenantMember = {
            id: tenantMemberId(tenantId, userId),
            tenantId,
            userId,
            displayName: user.displayName,
            email: user.email,
            role: 'member',
        };
        return [member];
    });

    await Promise.all(missing.map(member => savePortalTenantMember(member)));
}

async function resolveMemberIdByUserId(tenantId: string): Promise<Map<string, string>> {
    const members = await getMembersByTenantId(tenantId);
    return new Map(members.map(member => [member.userId, member.id]));
}

/**
 * Seeds environment-level tenants, groups, memberships, and access grants once.
 * Runs after tenant seeding so the existing "Acme Corp" tenant is reused rather than duplicated.
 */
export async function seedPermissionsIfEmpty(): Promise<void> {
    const existingGroups = await getAllPortalGroups();
    if (existingGroups.length > 0) {
        return;
    }

    const { seedPortalTenantsIfEmpty } = await import('../../tenants/storage/seed-portal-tenants');
    await seedPortalTenantsIfEmpty();

    const tenants = await getAllPortalTenants();
    const tenantById = new Map(tenants.map(tenant => [tenant.id, tenant]));

    const acme = tenantById.get(ACME_TENANT_ID);
    if (!acme) {
        await savePortalTenant(createTenant(ACME_TENANT_SEED));
    } else if (!acme.managementMode) {
        await savePortalTenant({ ...acme, managementMode: ACME_TENANT_SEED.managementMode });
    }

    await Promise.all(
        TENANT_SEEDS.filter(seed => !tenantById.has(seed.id)).map(seed => savePortalTenant(createTenant(seed))),
    );

    await ensureTenantMembers(ACME_TENANT_ID, ACME_USER_IDS);
    await Promise.all(TENANT_SEEDS.map(seed => ensureTenantMembers(seed.id, seed.userIds)));

    const memberIdsByTenant = new Map<string, Map<string, string>>();
    for (const tenantId of [ACME_TENANT_ID, ...TENANT_SEEDS.map(seed => seed.id)]) {
        memberIdsByTenant.set(tenantId, await resolveMemberIdByUserId(tenantId));
    }

    for (const seed of GROUP_SEEDS) {
        const group: PortalGroup = {
            id: seed.id,
            tenantId: seed.tenantId,
            name: seed.name,
            hrid: seed.name,
            description: seed.description,
            managementMode: seed.managementMode,
            features: { ...DEFAULT_PORTAL_TENANT_FEATURES },
            createdAt: SEED_CREATED_AT,
            updatedAt: SEED_UPDATED_AT,
        };
        await savePortalGroup(group);

        const memberIdByUserId = memberIdsByTenant.get(seed.tenantId);
        const admins = new Set(seed.adminUserIds ?? []);

        for (const userId of seed.userIds) {
            const memberId = memberIdByUserId?.get(userId);
            if (!memberId) {
                continue;
            }

            const groupMember: PortalGroupMember = {
                id: `${seed.id}-${userId}`,
                groupId: seed.id,
                tenantId: seed.tenantId,
                memberId,
                role: admins.has(userId) ? 'admin' : 'member',
            };
            await savePortalGroupMember(groupMember);
        }
    }

    await Promise.all(grantSeeds().map(grant => savePortalAccessGrant(grant)));
}
