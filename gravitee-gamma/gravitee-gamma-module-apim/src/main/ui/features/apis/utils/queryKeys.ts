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
