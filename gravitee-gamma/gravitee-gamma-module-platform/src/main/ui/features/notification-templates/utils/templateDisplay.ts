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

import type {
    NotificationTemplate,
    NotificationTemplateCategory,
    NotificationTemplateDraft,
    NotificationTemplateListRow,
    NotificationTemplateType,
} from '../types/notificationTemplate';

export const TEMPLATES_TO_INCLUDE_SCOPE = 'TEMPLATES_TO_INCLUDE';
export const TEMPLATES_FOR_ALERT_SCOPE = 'TEMPLATES_FOR_ALERT';

export const CHANNEL_ORDER: readonly NotificationTemplateType[] = ['EMAIL', 'PORTAL'];

const SCOPE_ORDER = [
    'API',
    'API_PRODUCT',
    'APPLICATION',
    'PORTAL',
    'TEMPLATES_FOR_ACTION',
    'TEMPLATES_FOR_ALERT',
    'TEMPLATES_TO_INCLUDE',
] as const;

const SCOPE_META: Record<(typeof SCOPE_ORDER)[number], { label: string; description: string }> = {
    API: { label: 'API', description: 'Sent when an API changes state, or when someone subscribes to one.' },
    API_PRODUCT: {
        label: 'API Product',
        description: 'Sent for API Product lifecycle and subscription events.',
    },
    APPLICATION: { label: 'Application', description: 'Sent to application members about their subscriptions and support tickets.' },
    PORTAL: { label: 'Portal', description: 'Sent for user, group and Developer Portal events.' },
    TEMPLATES_FOR_ACTION: {
        label: 'Templates for action',
        description: 'Emails the platform sends directly to a person, outside the notification hooks.',
    },
    TEMPLATES_FOR_ALERT: { label: 'Templates for alert', description: 'Emails sent when a consumer alert is triggered.' },
    TEMPLATES_TO_INCLUDE: {
        label: 'Templates to include',
        description: 'Fragments other templates pull in, rather than notifications of their own.',
    },
};

export function isTemplatesToInclude(scope: string): boolean {
    return scope.toUpperCase() === TEMPLATES_TO_INCLUDE_SCOPE;
}

export function scopeLabel(scope: string): string {
    const meta = SCOPE_META[scope.toUpperCase() as keyof typeof SCOPE_META];
    return meta?.label ?? scope;
}

export function templateRouteSegment(row: { hook?: string; name: string }): string {
    return row.hook && row.hook !== '' ? row.hook : row.name;
}

export function canPersistChannel(template: NotificationTemplate, canCreate: boolean, canUpdate: boolean): boolean {
    return template.id ? canUpdate : canCreate;
}

export function toPersistedTemplate(original: NotificationTemplate, draft: NotificationTemplateDraft): NotificationTemplate {
    return {
        ...original,
        enabled: draft.enabled,
        title: draft.title,
        content: draft.content,
    };
}

export function validateChannelDraft(draft: NotificationTemplateDraft, isInclude: boolean): string[] {
    if (!draft.enabled) {
        return [];
    }
    const errors: string[] = [];
    if (!isInclude && !draft.title.trim()) {
        errors.push('title');
    }
    if (!draft.content.trim()) {
        errors.push('content');
    }
    return errors;
}

export function channelDraftsEqual(
    left: Record<string, NotificationTemplateDraft>,
    right: Record<string, NotificationTemplateDraft>,
): boolean {
    const keys = new Set([...Object.keys(left), ...Object.keys(right)]);
    for (const key of keys) {
        const a = left[key];
        const b = right[key];
        if (!a || !b) {
            return false;
        }
        if (a.enabled !== b.enabled || a.title !== b.title || a.content !== b.content) {
            return false;
        }
    }
    return true;
}

export function groupTemplatesByCategory(
    templates: readonly NotificationTemplate[],
    options: { alertEnabled: boolean },
): NotificationTemplateCategory[] {
    const visible = options.alertEnabled ? templates : templates.filter(item => item.scope.toUpperCase() !== TEMPLATES_FOR_ALERT_SCOPE);

    const grouped = new Map<string, NotificationTemplate[]>();
    for (const item of visible) {
        const key = `${item.scope.toUpperCase()}-${item.name}`;
        const bucket = grouped.get(key);
        if (bucket) {
            bucket.push(item);
        } else {
            grouped.set(key, [item]);
        }
    }

    const rowsByScope = new Map<string, NotificationTemplateListRow[]>();
    for (const variants of grouped.values()) {
        const first = variants[0];
        if (!first) {
            continue;
        }
        const scope = first.scope.toUpperCase();
        const row: NotificationTemplateListRow = {
            scope,
            name: first.name,
            hook: first.hook ?? '',
            description: first.description ?? '',
            overridden: variants.some(variant => variant.enabled === true),
            templateSegment: templateRouteSegment({ hook: first.hook, name: first.name }),
        };
        const existing = rowsByScope.get(scope) ?? [];
        existing.push(row);
        rowsByScope.set(scope, existing);
    }

    return SCOPE_ORDER.filter(scope => (rowsByScope.get(scope)?.length ?? 0) > 0).map(scope => {
        const rows = [...(rowsByScope.get(scope) ?? [])].sort((a, b) => a.name.localeCompare(b.name));
        const meta = SCOPE_META[scope];
        return {
            scope,
            label: meta.label,
            description: meta.description,
            rows,
            customCount: rows.filter(row => row.overridden).length,
        };
    });
}
