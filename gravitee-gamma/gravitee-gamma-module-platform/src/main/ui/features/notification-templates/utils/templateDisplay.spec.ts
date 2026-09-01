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

import {
    canPersistChannel,
    channelDraftsEqual,
    groupTemplatesByCategory,
    isTemplatesToInclude,
    scopeLabel,
    templateRouteSegment,
    toPersistedTemplate,
    validateChannelDraft,
} from './templateDisplay';
import type { NotificationTemplate } from '../types/notificationTemplate';

function template(overrides: Partial<NotificationTemplate> & Pick<NotificationTemplate, 'scope' | 'name' | 'type'>): NotificationTemplate {
    return {
        content: '<p>body</p>',
        hook: overrides.hook ?? 'APIKEY_REVOKED',
        description: overrides.description ?? 'Triggered when an API Key is revoked.',
        ...overrides,
    };
}

describe('groupTemplatesByCategory', () => {
    it('collapses EMAIL and PORTAL of the same scope and name into one row', () => {
        const categories = groupTemplatesByCategory(
            [
                template({ scope: 'API', name: 'API-Key Revoked', type: 'EMAIL', enabled: false }),
                template({ scope: 'API', name: 'API-Key Revoked', type: 'PORTAL', enabled: true }),
            ],
            { alertEnabled: true },
        );

        expect(categories).toHaveLength(1);
        expect(categories[0]?.rows).toHaveLength(1);
        expect(categories[0]?.rows[0]).toEqual(
            expect.objectContaining({
                name: 'API-Key Revoked',
                overridden: true,
            }),
        );
        expect(categories[0]?.customCount).toBe(1);
    });

    it('uses a curated scope order instead of alphabetical enum keys', () => {
        const categories = groupTemplatesByCategory(
            [
                template({ scope: 'TEMPLATES_FOR_ACTION', name: 'User registration', type: 'EMAIL', hook: 'USER_REGISTRATION' }),
                template({ scope: 'APPLICATION', name: 'New Subscription', type: 'EMAIL', hook: 'SUBSCRIPTION_NEW' }),
                template({ scope: 'API_PRODUCT', name: 'API-Key Expired', type: 'EMAIL', hook: 'APIKEY_EXPIRED' }),
                template({ scope: 'PORTAL', name: 'User Created', type: 'EMAIL', hook: 'USER_CREATED' }),
                template({ scope: 'API', name: 'API Started', type: 'EMAIL', hook: 'API_STARTED' }),
                template({ scope: 'TEMPLATES_TO_INCLUDE', name: 'header.html', type: 'EMAIL', hook: '' }),
                template({ scope: 'TEMPLATES_FOR_ALERT', name: 'HTTP status code', type: 'EMAIL', hook: 'CONSUMER_HTTP_STATUS' }),
            ],
            { alertEnabled: true },
        );

        expect(categories.map(category => category.scope)).toEqual([
            'API',
            'API_PRODUCT',
            'APPLICATION',
            'PORTAL',
            'TEMPLATES_FOR_ACTION',
            'TEMPLATES_FOR_ALERT',
            'TEMPLATES_TO_INCLUDE',
        ]);
    });

    it('omits Templates for alert when the alert engine setting is disabled', () => {
        const categories = groupTemplatesByCategory(
            [
                template({ scope: 'API', name: 'API Started', type: 'EMAIL', hook: 'API_STARTED' }),
                template({ scope: 'TEMPLATES_FOR_ALERT', name: 'HTTP status code', type: 'EMAIL', hook: 'CONSUMER_HTTP_STATUS' }),
            ],
            { alertEnabled: false },
        );

        expect(categories.map(category => category.scope)).toEqual(['API']);
    });

    it('hides alert templates even when the backend scope casing differs', () => {
        const categories = groupTemplatesByCategory(
            [
                template({ scope: 'API', name: 'API Started', type: 'EMAIL', hook: 'API_STARTED' }),
                template({ scope: 'templates_for_alert', name: 'HTTP status code', type: 'EMAIL', hook: 'CONSUMER_HTTP_STATUS' }),
            ],
            { alertEnabled: false },
        );

        expect(categories.map(category => category.scope)).toEqual(['API']);
    });

    it('emits catalog rows under SCOPE_ORDER even when backend scope casing differs', () => {
        const categories = groupTemplatesByCategory([template({ scope: 'api', name: 'API Started', type: 'EMAIL', hook: 'API_STARTED' })], {
            alertEnabled: true,
        });

        expect(categories.map(category => category.scope)).toEqual(['API']);
        expect(categories[0]?.rows[0]?.scope).toBe('API');
    });

    it('sorts rows alphabetically by name within a scope', () => {
        const categories = groupTemplatesByCategory(
            [
                template({ scope: 'API', name: 'API Stopped', type: 'EMAIL', hook: 'API_STOPPED' }),
                template({ scope: 'API', name: 'API Started', type: 'EMAIL', hook: 'API_STARTED' }),
            ],
            { alertEnabled: true },
        );

        expect(categories[0]?.rows.map(row => row.name)).toEqual(['API Started', 'API Stopped']);
    });

    it('uses the template name as the route segment when hook is empty', () => {
        const [include] =
            groupTemplatesByCategory([template({ scope: 'TEMPLATES_TO_INCLUDE', name: 'header.html', type: 'EMAIL', hook: '' })], {
                alertEnabled: true,
            })[0]?.rows ?? [];

        expect(include?.templateSegment).toBe('header.html');
        expect(templateRouteSegment({ hook: '', name: 'header.html' })).toBe('header.html');
        expect(isTemplatesToInclude('TEMPLATES_TO_INCLUDE')).toBe(true);
    });
});

describe('scopeLabel', () => {
    it('maps include and action enums to Classic catalog labels', () => {
        expect(scopeLabel('TEMPLATES_TO_INCLUDE')).toBe('Templates to include');
        expect(scopeLabel('TEMPLATES_FOR_ACTION')).toBe('Templates for action');
        expect(scopeLabel('API')).toBe('API');
    });
});

describe('channelDraftsEqual', () => {
    it('ignores EMAIL/PORTAL key insertion order', () => {
        const email = { enabled: true, title: 'Hi', content: '<p>x</p>' };
        const portal = { enabled: false, title: 'Hi', content: 'x' };
        expect(channelDraftsEqual({ EMAIL: email, PORTAL: portal }, { PORTAL: portal, EMAIL: email })).toBe(true);
        expect(channelDraftsEqual({ EMAIL: email, PORTAL: portal }, { EMAIL: { ...email, title: 'Bye' }, PORTAL: portal })).toBe(false);
    });
});

describe('channel persistence helpers', () => {
    it('requires CREATE for a first override and UPDATE once the row has an id', () => {
        expect(canPersistChannel(template({ scope: 'API', name: 'API Started', type: 'EMAIL' }), true, false)).toBe(true);
        expect(canPersistChannel(template({ scope: 'API', name: 'API Started', type: 'EMAIL' }), false, true)).toBe(false);
        expect(canPersistChannel(template({ id: 't-1', scope: 'API', name: 'API Started', type: 'EMAIL' }), false, true)).toBe(true);
        expect(canPersistChannel(template({ id: 't-1', scope: 'API', name: 'API Started', type: 'EMAIL' }), true, false)).toBe(false);
    });

    it('copies draft override flag, title, and content onto the original template', () => {
        const saved = toPersistedTemplate(template({ id: 't-1', scope: 'API', name: 'API Started', type: 'EMAIL', title: 'Old' }), {
            enabled: false,
            title: 'New title',
            content: '<p>custom</p>',
        });
        expect(saved).toEqual(expect.objectContaining({ id: 't-1', enabled: false, title: 'New title', content: '<p>custom</p>' }));
    });

    it('requires title and content while override is on, except include-fragments have no title', () => {
        expect(validateChannelDraft({ enabled: true, title: '', content: '<p>x</p>' }, false)).toEqual(['title']);
        expect(validateChannelDraft({ enabled: true, title: 'Hi', content: '' }, false)).toEqual(['content']);
        expect(validateChannelDraft({ enabled: true, title: '', content: '<p>x</p>' }, true)).toEqual([]);
        expect(validateChannelDraft({ enabled: false, title: '', content: '' }, false)).toEqual([]);
    });
});
