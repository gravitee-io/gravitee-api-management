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
import type { PlanContext, PlanStatus } from '../types/plan';
import type { SubscriptionContext } from '../types/subscription';

export const apiProxyKeys = {
    all: ['apiProxy'] as const,
    create: () => [...apiProxyKeys.all, 'create'] as const,
} as const;

export const apiListKeys = {
    all: ['api-list'] as const,
    search: (envId: string, query: string, page: number, perPage: number) =>
        [...apiListKeys.all, 'search', envId, query, page, perPage] as const,
    count: (envId: string, filter: object) => [...apiListKeys.all, 'count', envId, JSON.stringify(filter)] as const,
} as const;

export const apiDetailKeys = {
    all: ['api-detail'] as const,
    detail: (envId: string, apiId: string) => [...apiDetailKeys.all, envId, apiId] as const,
};

export const apiEventsKeys = {
    all: ['api-events'] as const,
    list: (envId: string, apiId: string, page: number, perPage: number) => [...apiEventsKeys.all, envId, apiId, page, perPage] as const,
};

export const apiAuditKeys = {
    all: ['api-audits'] as const,
    list: (envId: string, apiId: string, params: object) => [...apiAuditKeys.all, envId, apiId, params] as const,
    events: (envId: string, apiId: string) => [...apiAuditKeys.all, 'events', envId, apiId] as const,
};

export const apiMemberKeys = {
    all: ['api-members'] as const,
    list: (envId: string, apiId: string) => [...apiMemberKeys.all, envId, apiId] as const,
    groups: (envId: string, apiId: string) => [...apiMemberKeys.all, 'groups', envId, apiId] as const,
};

export const apiRoleKeys = {
    all: ['api-roles'] as const,
    list: () => [...apiRoleKeys.all, 'list'] as const,
};

export const groupKeys = {
    all: ['groups'] as const,
    list: (envId: string) => [...groupKeys.all, envId] as const,
    members: (envId: string, groupId: string) => [...groupKeys.all, 'members', envId, groupId] as const,
};

export const orgTagKeys = {
    all: ['org-tags'] as const,
    list: () => [...orgTagKeys.all, 'list'] as const,
};

export const envCategoryKeys = {
    all: ['env-categories'] as const,
    list: (envId: string) => [...envCategoryKeys.all, envId] as const,
};

export const apiPermissionKeys = {
    all: ['api-permissions'] as const,
    detail: (envId: string, apiId: string) => [...apiPermissionKeys.all, envId, apiId] as const,
};

export const apiEntrypointKeys = {
    all: ['api-entrypoints'] as const,
    exposed: (envId: string, apiId: string) => [...apiEntrypointKeys.all, 'exposed', envId, apiId] as const,
};

export const portalSettingsKeys = {
    all: ['portal-settings'] as const,
    env: (envId: string) => [...portalSettingsKeys.all, envId] as const,
};

export const apiAlertKeys = {
    all: ['api-alerts'] as const,
    list: (envId: string, apiId: string) => [...apiAlertKeys.all, envId, apiId] as const,
    history: (envId: string, apiId: string, alertId: string) => [...apiAlertKeys.all, 'history', envId, apiId, alertId] as const,
};

export const apiAnalyticsKeys = {
    all: ['api-analytics'] as const,
    stats: (envId: string, apiId: string, window: string) => [...apiAnalyticsKeys.all, 'stats', envId, apiId, window] as const,
    statusRanges: (envId: string, apiId: string, window: string) =>
        [...apiAnalyticsKeys.all, 'status-ranges', envId, apiId, window] as const,
};

export const apiBroadcastKeys = {
    all: ['api-broadcasts'] as const,
    applicationRoles: () => [...apiBroadcastKeys.all, 'application-roles'] as const,
};

export const apiNotificationKeys = {
    all: ['api-notifications'] as const,
    list: (envId: string, apiId: string) => [...apiNotificationKeys.all, 'list', envId, apiId] as const,
    notifiers: (envId: string, apiId: string) => [...apiNotificationKeys.all, 'notifiers', envId, apiId] as const,
    hooks: (envId: string) => [...apiNotificationKeys.all, 'hooks', envId] as const,
};

export const apiPlanKeys = {
    all: ['api-plans'] as const,
    list: (envId: string, ctx: PlanContext, statuses: PlanStatus[], page: number, perPage: number) =>
        [...apiPlanKeys.all, ctx.type, ctx.entityId, 'list', envId, statuses, page, perPage] as const,
    detail: (envId: string, ctx: PlanContext, planId: string) =>
        [...apiPlanKeys.all, ctx.type, ctx.entityId, 'detail', envId, planId] as const,
    count: (envId: string, ctx: PlanContext, status: PlanStatus) =>
        [...apiPlanKeys.all, ctx.type, ctx.entityId, 'count', envId, status] as const,
};

export const tenantKeys = {
    all: ['tenants'] as const,
    list: (envId: string) => [...tenantKeys.all, envId] as const,
};

// TODO: Subscription query keys — prep for subscriptions feature (no hooks/pages yet)
export const apiSubscriptionKeys = {
    all: ['api-subscriptions'] as const,
    list: (envId: string, ctx: SubscriptionContext, filters: object) =>
        [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'list', envId, filters] as const,
    detail: (envId: string, ctx: SubscriptionContext, subscriptionId: string) =>
        [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'detail', envId, subscriptionId] as const,
    apiKeys: (envId: string, ctx: SubscriptionContext, subscriptionId: string, page: number) =>
        [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'api-keys', envId, subscriptionId, page] as const,
    plans: (envId: string, ctx: SubscriptionContext) => [...apiSubscriptionKeys.all, ctx.type, ctx.entityId, 'plans', envId] as const,
    applications: (envId: string, query: string) => [...apiSubscriptionKeys.all, 'applications', envId, query] as const,
};
