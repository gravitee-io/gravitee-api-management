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
import { categoryOf, countByCategory, formatRelativeTime, type ResolveEnvHrid, resolveArea, sortTasks, toTaskView } from './tasks.mapping';
import type { TaskEntity, TaskMetadata, TaskView } from './tasks.types';

function makeTaskView(overrides: Partial<TaskView> = {}): TaskView {
    return {
        id: 'task-1',
        type: 'SUBSCRIPTION_APPROVAL',
        category: 'SUBSCRIPTION',
        categoryLabel: 'Subscription Approval',
        actionLabel: 'Validate subscription',
        iconKey: 'subscription',
        area: { key: 'apim', label: 'API Management' },
        title: 'Task',
        subtitle: '',
        createdAt: 0,
        to: null,
        toModuleId: null,
        entity: { type: 'SUBSCRIPTION_APPROVAL', created_at: 0, data: {} },
        ...overrides,
    };
}

const resolveEnvHrid: ResolveEnvHrid = environmentId => (environmentId === 'env-2-id' ? 'staging' : 'prod');

describe('resolveArea', () => {
    it('labels mcp-proxy as MCP', () => {
        expect(resolveArea('mcp-proxy')).toEqual({ key: 'mcp', label: 'MCP' });
    });

    it('labels llm-proxy as LLM and a2a-proxy as AI Agent', () => {
        expect(resolveArea('llm-proxy')).toEqual({ key: 'llm', label: 'LLM' });
        expect(resolveArea('a2a-proxy')).toEqual({ key: 'ai', label: 'AI Agent' });
    });

    it('labels native as Kafka', () => {
        expect(resolveArea('native')).toEqual({ key: 'kafka', label: 'Kafka' });
    });

    it('falls back to API Management for proxy, missing, and unknown types', () => {
        expect(resolveArea('proxy').key).toBe('apim');
        expect(resolveArea(undefined).key).toBe('apim');
        expect(resolveArea('something-new').key).toBe('apim');
    });
});

describe('categoryOf', () => {
    it('maps each task type to its category', () => {
        expect(categoryOf('SUBSCRIPTION_APPROVAL')).toBe('SUBSCRIPTION');
        expect(categoryOf('IN_REVIEW')).toBe('API_REVIEW');
        expect(categoryOf('REQUEST_FOR_CHANGES')).toBe('CHANGES_REQUESTED');
        expect(categoryOf('USER_REGISTRATION_APPROVAL')).toBe('USER_REGISTRATION');
        expect(categoryOf('PROMOTION_APPROVAL')).toBe('API_PROMOTION');
    });
});

describe('toTaskView', () => {
    const metadata: TaskMetadata = {
        'app-1': { name: 'Passenger App' },
        'plan-1': { name: 'Gold', api: 'api-http' },
        'api-http': { name: 'Flight Status API', apiType: 'proxy' },
        'api-mcp': { name: 'Booking MCP Server', apiType: 'mcp-proxy' },
        'api-llm': { name: 'Concierge Agent', apiType: 'llm-proxy' },
        'api-a2a': { name: 'Dispatch Agent', apiType: 'a2a-proxy' },
        'api-kafka': { name: 'Orders Stream', apiType: 'native' },
        'product-1': { name: 'Travel Suite' },
    };

    const subscription = (referenceId: string, overrides: Record<string, unknown> = {}): TaskEntity => ({
        type: 'SUBSCRIPTION_APPROVAL',
        created_at: 1,
        data: { id: `sub-${referenceId}`, application: 'app-1', plan: 'plan-1', referenceId, referenceType: 'API', ...overrides },
    });

    it('builds an HTTP subscription with a deep link into the apim module', () => {
        const entity: TaskEntity = {
            type: 'SUBSCRIPTION_APPROVAL',
            created_at: 1,
            data: { id: 'sub-1', application: 'app-1', plan: 'plan-1', referenceId: 'api-http', referenceType: 'API' },
        };

        const view = toTaskView(entity, metadata, resolveEnvHrid);

        expect(view.category).toBe('SUBSCRIPTION');
        expect(view.area.key).toBe('apim');
        expect(view.title).toBe('Passenger App → Flight Status API');
        expect(view.subtitle).toBe('Plan: Gold');
        expect(view.to).toBe('/environments/prod/apim/apis/api-http/consumers/sub-1');
        expect(view.toModuleId).toBe('apim');
    });

    it('deep-links into the task own environment when it differs from the current one', () => {
        const entity: TaskEntity = {
            type: 'SUBSCRIPTION_APPROVAL',
            created_at: 1,
            data: { id: 'sub-3', application: 'app-1', plan: 'plan-1', referenceId: 'api-staging', referenceType: 'API' },
        };
        const crossEnvMetadata: TaskMetadata = {
            ...metadata,
            'api-staging': { name: 'Staging API', apiType: 'proxy', environmentId: 'env-2-id' },
        };

        const view = toTaskView(entity, crossEnvMetadata, resolveEnvHrid);

        expect(view.to).toBe('/environments/staging/apim/apis/api-staging/consumers/sub-3');
    });

    it('deep-links an MCP subscription to the proxy Consumers list (no per-subscription page yet)', () => {
        const entity: TaskEntity = {
            type: 'SUBSCRIPTION_APPROVAL',
            created_at: 1,
            data: { id: 'sub-2', application: 'app-1', plan: 'plan-1', referenceId: 'api-mcp', referenceType: 'API' },
        };

        const view = toTaskView(entity, metadata, resolveEnvHrid);

        expect(view.area).toEqual({ key: 'mcp', label: 'MCP' });
        expect(view.toModuleId).toBe('aim');
        expect(view.to).toBe('/environments/prod/aim/mcp-proxy/api-mcp/consumers');
    });

    it('deep-links LLM subscriptions to the llm-router consumer page and A2A to the agent-runtime API page', () => {
        const llm = toTaskView(subscription('api-llm'), metadata, resolveEnvHrid);
        expect(llm.area).toEqual({ key: 'llm', label: 'LLM' });
        expect(llm.toModuleId).toBe('aim');
        expect(llm.to).toBe('/environments/prod/aim/llm-router/api-llm/consumers/sub-api-llm');

        const a2a = toTaskView(subscription('api-a2a'), metadata, resolveEnvHrid);
        expect(a2a.area).toEqual({ key: 'ai', label: 'AI Agent' });
        expect(a2a.toModuleId).toBe('aim');
        expect(a2a.to).toBe('/environments/prod/aim/agent-runtime/api-a2a');
    });

    it('labels a native subscription as Kafka and routes it to the Event Stream Management module', () => {
        const view = toTaskView(subscription('api-kafka'), metadata, resolveEnvHrid);

        expect(view.area).toEqual({ key: 'kafka', label: 'Kafka' });
        expect(view.toModuleId).toBe('esm');
        expect(view.to).toBe('/environments/prod/esm');
    });

    it('builds an API Product subscription with a deep link into the apim api-products area', () => {
        const view = toTaskView(subscription('product-1', { id: 'sub-9', referenceType: 'API_PRODUCT' }), metadata, resolveEnvHrid);

        expect(view.area.key).toBe('apim');
        expect(view.toModuleId).toBe('apim');
        expect(view.to).toBe('/environments/prod/apim/api-products/product-1/consumers/sub-9');
    });

    it('falls back to the apim consumer deep link when apiType metadata is missing (legacy tasks)', () => {
        const view = toTaskView(subscription('api-legacy'), metadata, resolveEnvHrid);

        expect(view.area.key).toBe('apim');
        expect(view.toModuleId).toBe('apim');
        expect(view.to).toBe('/environments/prod/apim/apis/api-legacy/consumers/sub-api-legacy');
    });

    it('builds a review task linking to the API page', () => {
        const entity: TaskEntity = { type: 'IN_REVIEW', created_at: 1, data: { referenceId: 'api-http' } };

        const view = toTaskView(entity, metadata, resolveEnvHrid);

        expect(view.category).toBe('API_REVIEW');
        expect(view.title).toBe('Flight Status API');
        expect(view.subtitle).toBe('Ready to be reviewed');
        expect(view.to).toBe('/environments/prod/apim/apis/api-http');
        expect(view.toModuleId).toBe('apim');
    });

    it('deep-links an LLM review task to the llm-router API page instead of the module root', () => {
        const entity: TaskEntity = { type: 'IN_REVIEW', created_at: 1, data: { referenceId: 'api-llm' } };

        const view = toTaskView(entity, metadata, resolveEnvHrid);

        expect(view.area.key).toBe('llm');
        expect(view.toModuleId).toBe('aim');
        expect(view.to).toBe('/environments/prod/aim/llm-router/api-llm');
    });

    it('deep-links an MCP review task to the mcp-proxy API page', () => {
        const entity: TaskEntity = { type: 'IN_REVIEW', created_at: 1, data: { referenceId: 'api-mcp' } };

        const view = toTaskView(entity, metadata, resolveEnvHrid);

        expect(view.toModuleId).toBe('aim');
        expect(view.to).toBe('/environments/prod/aim/mcp-proxy/api-mcp');
    });

    it('builds a changes-requested task carrying the reviewer comment', () => {
        const entity: TaskEntity = {
            type: 'REQUEST_FOR_CHANGES',
            created_at: 1,
            data: { referenceId: 'api-http', comment: 'Please add a rate-limit policy.' },
        };

        const view = toTaskView(entity, metadata, resolveEnvHrid);

        expect(view.category).toBe('CHANGES_REQUESTED');
        expect(view.subtitle).toBe('Changes requested by reviewer');
        expect(view.comment).toBe('Please add a rate-limit policy.');
        expect(view.to).toBe('/environments/prod/apim/apis/api-http');
        expect(view.toModuleId).toBe('apim');
    });

    it('builds a user registration task labelled Users with no redirect yet', () => {
        const entity: TaskEntity = {
            type: 'USER_REGISTRATION_APPROVAL',
            created_at: 1,
            data: { id: 'user-1', displayName: 'Maria Schneider' },
        };

        const view = toTaskView(entity, {}, resolveEnvHrid);

        expect(view.area.key).toBe('users');
        expect(view.title).toBe('Maria Schneider');
        expect(view.to).toBeNull();
        expect(view.toModuleId).toBeNull();
    });

    it('builds a promotion task linking to the target API', () => {
        const entity: TaskEntity = {
            type: 'PROMOTION_APPROVAL',
            created_at: 1,
            data: {
                promotionId: 'promo-1',
                apiName: 'Loyalty API',
                sourceEnvironmentName: 'Staging',
                targetEnvironmentName: 'Production',
                targetApiId: 'api-9',
            },
        };

        const view = toTaskView(entity, {}, resolveEnvHrid);

        expect(view.category).toBe('API_PROMOTION');
        expect(view.title).toBe('Loyalty API');
        expect(view.subtitle).toBe('Staging → Production');
        expect(view.to).toBe('/environments/prod/apim/apis/api-9');
    });
});

describe('countByCategory', () => {
    it('counts tasks per category and defaults missing ones to zero', () => {
        const counts = countByCategory([
            makeTaskView({ category: 'SUBSCRIPTION', createdAt: 1 }),
            makeTaskView({ category: 'SUBSCRIPTION', createdAt: 2 }),
            makeTaskView({ category: 'API_REVIEW', createdAt: 3 }),
        ]);

        expect(counts.SUBSCRIPTION).toBe(2);
        expect(counts.API_REVIEW).toBe(1);
        expect(counts.API_PROMOTION).toBe(0);
    });
});

describe('sortTasks', () => {
    it('orders by createdAt descending for newest and ascending for oldest', () => {
        const tasks = [
            makeTaskView({ category: 'SUBSCRIPTION', createdAt: 10 }),
            makeTaskView({ category: 'API_REVIEW', createdAt: 30 }),
            makeTaskView({ category: 'API_PROMOTION', createdAt: 20 }),
        ];

        expect(sortTasks(tasks, 'newest').map(task => task.createdAt)).toEqual([30, 20, 10]);
        expect(sortTasks(tasks, 'oldest').map(task => task.createdAt)).toEqual([10, 20, 30]);
    });
});

describe('formatRelativeTime', () => {
    const now = 1_000_000_000_000;

    it('formats recent, minute, hour, and day distances', () => {
        expect(formatRelativeTime(now, now)).toBe('just now');
        expect(formatRelativeTime(now - 5 * 60_000, now)).toBe('5 min ago');
        expect(formatRelativeTime(now - 3 * 3_600_000, now)).toBe('3 hr ago');
        expect(formatRelativeTime(now - 2 * 86_400_000, now)).toBe('2 days ago');
    });
});
