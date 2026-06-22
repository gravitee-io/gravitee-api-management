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
import type { TaskArea, TaskCategory, TaskEntity, TaskIconKey, TaskMetadata, TaskType, TaskView } from './tasks.types';

const CATEGORY_BY_TYPE: Record<TaskType, TaskCategory> = {
    SUBSCRIPTION_APPROVAL: 'SUBSCRIPTION',
    IN_REVIEW: 'API_REVIEW',
    REQUEST_FOR_CHANGES: 'CHANGES_REQUESTED',
    USER_REGISTRATION_APPROVAL: 'USER_REGISTRATION',
    PROMOTION_APPROVAL: 'API_PROMOTION',
};

const ICON_BY_TYPE: Record<TaskType, TaskIconKey> = {
    SUBSCRIPTION_APPROVAL: 'subscription',
    IN_REVIEW: 'review',
    REQUEST_FOR_CHANGES: 'changes',
    USER_REGISTRATION_APPROVAL: 'registration',
    PROMOTION_APPROVAL: 'promotion',
};

const CATEGORY_LABEL: Record<TaskCategory, string> = {
    SUBSCRIPTION: 'Subscription Approval',
    API_REVIEW: 'API Review',
    CHANGES_REQUESTED: 'Changes Requested',
    USER_REGISTRATION: 'User Registration',
    API_PROMOTION: 'API Promotion',
};

const ACTION_LABEL: Record<TaskType, string> = {
    SUBSCRIPTION_APPROVAL: 'Validate subscription',
    IN_REVIEW: 'Review API',
    REQUEST_FOR_CHANGES: 'Address feedback',
    USER_REGISTRATION_APPROVAL: 'Validate user',
    PROMOTION_APPROVAL: 'Review promotion',
};

type ConsumerLevel = 'detail' | 'list' | 'none';

interface ApiTypeConfig {
    readonly area: TaskArea;
    readonly moduleId: string;
    readonly route?: { readonly section: string; readonly consumers: ConsumerLevel };
}

const API_MANAGEMENT_AREA: TaskArea = { key: 'apim', label: 'API Management' };
const USERS_AREA: TaskArea = { key: 'users', label: 'Users' };

const API_TYPE_CONFIG: Record<string, ApiTypeConfig> = {
    proxy: { area: API_MANAGEMENT_AREA, moduleId: 'apim', route: { section: 'apis', consumers: 'detail' } },
    message: { area: API_MANAGEMENT_AREA, moduleId: 'apim', route: { section: 'apis', consumers: 'detail' } },
    'mcp-proxy': { area: { key: 'mcp', label: 'MCP' }, moduleId: 'aim', route: { section: 'mcp-proxy', consumers: 'list' } },
    'llm-proxy': { area: { key: 'llm', label: 'LLM' }, moduleId: 'aim', route: { section: 'llm-router', consumers: 'detail' } },
    'a2a-proxy': { area: { key: 'ai', label: 'AI Agent' }, moduleId: 'aim', route: { section: 'agent-runtime', consumers: 'none' } },
    native: { area: { key: 'kafka', label: 'Kafka' }, moduleId: 'esm' },
};

const DEFAULT_CONFIG: ApiTypeConfig = { area: API_MANAGEMENT_AREA, moduleId: 'apim', route: { section: 'apis', consumers: 'detail' } };

const API_PRODUCT_CONFIG: ApiTypeConfig = {
    area: API_MANAGEMENT_AREA,
    moduleId: 'apim',
    route: { section: 'api-products', consumers: 'detail' },
};

function configFor(apiType: string | undefined, referenceType: string | undefined): ApiTypeConfig {
    if (referenceType === 'API_PRODUCT') {
        return API_PRODUCT_CONFIG;
    }
    return (apiType ? API_TYPE_CONFIG[apiType] : undefined) ?? DEFAULT_CONFIG;
}

export type TaskSortOrder = 'newest' | 'oldest';

const EMPTY_COUNTS: Record<TaskCategory, number> = {
    SUBSCRIPTION: 0,
    API_REVIEW: 0,
    CHANGES_REQUESTED: 0,
    USER_REGISTRATION: 0,
    API_PROMOTION: 0,
};

export function categoryOf(type: TaskType): TaskCategory {
    return CATEGORY_BY_TYPE[type];
}

export function countByCategory(tasks: readonly TaskView[]): Record<TaskCategory, number> {
    const counts = { ...EMPTY_COUNTS };
    for (const task of tasks) {
        counts[task.category] += 1;
    }
    return counts;
}

export function sortTasks(tasks: readonly TaskView[], order: TaskSortOrder): TaskView[] {
    return [...tasks].sort((a, b) => (order === 'newest' ? b.createdAt - a.createdAt : a.createdAt - b.createdAt));
}

export function resolveArea(apiType?: string | null): TaskArea {
    if (!apiType) {
        return API_MANAGEMENT_AREA;
    }
    return API_TYPE_CONFIG[apiType]?.area ?? API_MANAGEMENT_AREA;
}

export function formatRelativeTime(createdAt: number, now: number = Date.now()): string {
    const seconds = Math.max(0, Math.round((now - createdAt) / 1000));
    if (seconds < 60) {
        return 'just now';
    }
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) {
        return `${minutes} min ago`;
    }
    const hours = Math.round(minutes / 60);
    if (hours < 24) {
        return `${hours} hr ago`;
    }
    const days = Math.round(hours / 24);
    return `${days} day${days === 1 ? '' : 's'} ago`;
}

function str(value: unknown): string | undefined {
    return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function metaField(metadata: TaskMetadata, id: string | undefined, field: string): string | undefined {
    if (!id) {
        return undefined;
    }
    return str(metadata[id]?.[field]);
}

function envPath(envHrid: string, ...segments: string[]): string {
    return ['/environments', envHrid, ...segments].join('/');
}

function apimPath(envHrid: string, ...segments: string[]): string {
    return envPath(envHrid, 'apim', ...segments);
}

function moduleRoot(envHrid: string, moduleId: string): string {
    return `/environments/${envHrid}/${moduleId}`;
}

function resolveApiTarget(config: ApiTypeConfig, envHrid: string, refId: string | undefined): string {
    if (!config.route || !refId) {
        return moduleRoot(envHrid, config.moduleId);
    }
    return envPath(envHrid, config.moduleId, config.route.section, refId);
}

function resolveConsumerTarget(config: ApiTypeConfig, envHrid: string, refId: string | undefined, subscriptionId: string): string {
    if (!config.route || !refId) {
        return moduleRoot(envHrid, config.moduleId);
    }
    const { section, consumers } = config.route;
    if (consumers === 'detail' && subscriptionId) {
        return envPath(envHrid, config.moduleId, section, refId, 'consumers', subscriptionId);
    }
    if (consumers === 'list') {
        return envPath(envHrid, config.moduleId, section, refId, 'consumers');
    }
    return envPath(envHrid, config.moduleId, section, refId);
}

export type ResolveEnvHrid = (environmentId?: string) => string;

export function toTaskView(entity: TaskEntity, metadata: TaskMetadata, resolveEnvHrid: ResolveEnvHrid): TaskView {
    const { type, data } = entity;
    const category = categoryOf(type);
    const base = {
        type,
        category,
        categoryLabel: CATEGORY_LABEL[category],
        actionLabel: ACTION_LABEL[type],
        iconKey: ICON_BY_TYPE[type],
        createdAt: entity.created_at,
        entity,
    };

    switch (type) {
        case 'SUBSCRIPTION_APPROVAL': {
            const subscriptionId = str(data.id) ?? '';
            const application = str(data.application);
            const plan = str(data.plan);
            const referenceType = str(data.referenceType);
            const refId = str(data.referenceId) ?? str(data.api) ?? metaField(metadata, plan, 'api');
            const appName = metaField(metadata, application, 'name') ?? application ?? 'Application';
            const refName = metaField(metadata, refId, 'name') ?? refId ?? 'API';
            const planName = metaField(metadata, plan, 'name') ?? plan ?? '';
            const apiType = metaField(metadata, refId, 'apiType');
            const config = configFor(apiType, referenceType);
            const envHrid = resolveEnvHrid(metaField(metadata, refId, 'environmentId'));
            const to = resolveConsumerTarget(config, envHrid, refId, subscriptionId);
            return {
                ...base,
                id: `SUBSCRIPTION_APPROVAL:${subscriptionId || refId || application}`,
                area: config.area,
                title: `${appName} → ${refName}`,
                subtitle: planName ? `Plan: ${planName}` : '',
                to,
                toModuleId: config.moduleId,
            };
        }
        case 'IN_REVIEW':
        case 'REQUEST_FOR_CHANGES': {
            const referenceId = str(data.referenceId) ?? '';
            const apiName = metaField(metadata, referenceId, 'name') ?? referenceId ?? 'API';
            const apiType = metaField(metadata, referenceId, 'apiType');
            const config = configFor(apiType, undefined);
            const comment = str(data.comment);
            const envHrid = resolveEnvHrid(metaField(metadata, referenceId, 'environmentId'));
            const to = resolveApiTarget(config, envHrid, referenceId);
            return {
                ...base,
                id: `${type}:${referenceId}`,
                area: config.area,
                title: apiName,
                subtitle: type === 'IN_REVIEW' ? 'Ready to be reviewed' : 'Changes requested by reviewer',
                comment,
                to,
                toModuleId: config.moduleId,
            };
        }
        case 'USER_REGISTRATION_APPROVAL': {
            const userId = str(data.id) ?? '';
            const displayName = str(data.displayName) ?? 'New user';
            return {
                ...base,
                id: `USER_REGISTRATION_APPROVAL:${userId || displayName}`,
                area: USERS_AREA,
                title: displayName,
                subtitle: str(data.email) ?? 'Awaiting validation',
                to: null,
                toModuleId: null,
            };
        }
        case 'PROMOTION_APPROVAL': {
            const promotionId = str(data.promotionId) ?? '';
            const apiName = str(data.apiName) ?? 'API';
            const sourceEnv = str(data.sourceEnvironmentName);
            const targetEnv = str(data.targetEnvironmentName);
            const targetApiId = str(data.targetApiId);
            const envHrid = resolveEnvHrid(undefined);
            const to = targetApiId ? apimPath(envHrid, 'apis', targetApiId) : apimPath(envHrid);
            return {
                ...base,
                id: `PROMOTION_APPROVAL:${promotionId || apiName}`,
                area: API_MANAGEMENT_AREA,
                title: apiName,
                subtitle: sourceEnv && targetEnv ? `${sourceEnv} → ${targetEnv}` : 'Promotion requested',
                to,
                toModuleId: 'apim',
            };
        }
    }
}
