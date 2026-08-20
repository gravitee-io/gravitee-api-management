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
import type { JsonSchema } from '@gravitee/graphene-core';

/**
 * Graphene only renders a multi-select when `type: array` has `uniqueItems: true` and
 * `items.enum`. Classic Angular schema-form treats the same enum array as checkboxes without
 * that flag, so plugin notifier schemas otherwise become an "Item 1 / Item 2" list.
 */
export function adaptNotifierSchemaForForm(schema: Record<string, unknown>): JsonSchema {
    return adaptNode(schema) as JsonSchema;
}

function adaptNode(node: unknown): unknown {
    if (!node || typeof node !== 'object' || Array.isArray(node)) {
        return node;
    }
    const obj: Record<string, unknown> = { ...(node as Record<string, unknown>) };
    if (obj.type === 'array' && isEnumItems(obj.items)) {
        obj.uniqueItems = true;
    }
    if (obj.properties && typeof obj.properties === 'object' && !Array.isArray(obj.properties)) {
        obj.properties = Object.fromEntries(
            Object.entries(obj.properties as Record<string, unknown>).map(([key, value]) => [key, adaptNode(value)]),
        );
    }
    if (obj.items !== undefined) {
        obj.items = Array.isArray(obj.items) ? obj.items.map(adaptNode) : adaptNode(obj.items);
    }
    for (const key of ['oneOf', 'anyOf', 'allOf'] as const) {
        const branch = obj[key];
        if (Array.isArray(branch)) {
            obj[key] = branch.map(adaptNode);
        }
    }
    return obj;
}

function isEnumItems(items: unknown): boolean {
    if (!items || typeof items !== 'object' || Array.isArray(items)) {
        return false;
    }
    const enumValues = (items as { enum?: unknown }).enum;
    return Array.isArray(enumValues) && enumValues.length > 0;
}

export function isNotifierConfigurationComplete(schema: Record<string, unknown>, configuration: Record<string, unknown>): boolean {
    return !hasMissingRequired(schema, applyPropertyDefaults(schema, configuration));
}

export function areAlertNotificationsComplete(
    notifications: ReadonlyArray<{ type: string; configuration: Record<string, unknown> }>,
    schemas: Record<string, Record<string, unknown> | undefined>,
    schemasLoading: boolean,
    failedNotifierIds: ReadonlySet<string> = new Set(),
    options?: { treatSchemaErrorAsComplete?: boolean },
): boolean {
    if (notifications.length === 0) {
        return true;
    }
    if (notifications.some(n => !n.type) || schemasLoading) {
        return false;
    }
    return notifications.every(n => {
        if (failedNotifierIds.has(n.type)) {
            return options?.treatSchemaErrorAsComplete === true;
        }
        const schema = schemas[n.type];
        return !!schema && isNotifierConfigurationComplete(schema, n.configuration ?? {});
    });
}

export function alertNotificationsIncompleteReason(
    notifications: ReadonlyArray<{ type: string; configuration: Record<string, unknown> }>,
    schemas: Record<string, Record<string, unknown> | undefined>,
    schemasLoading: boolean,
    failedNotifierIds: ReadonlySet<string> = new Set(),
    options?: { treatSchemaErrorAsComplete?: boolean },
): string | null {
    if (areAlertNotificationsComplete(notifications, schemas, schemasLoading, failedNotifierIds, options)) {
        return null;
    }
    if (notifications.some(n => !n.type)) {
        return 'Select a channel for each notification.';
    }
    if (schemasLoading) {
        return 'Loading notification fields…';
    }
    if (notifications.some(n => failedNotifierIds.has(n.type))) {
        return 'Notification settings could not be loaded.';
    }
    return 'Fill in the required fields for each notification.';
}

function applyPropertyDefaults(schema: Record<string, unknown>, data: Record<string, unknown>): Record<string, unknown> {
    const properties = schema.properties;
    if (!properties || typeof properties !== 'object' || Array.isArray(properties)) {
        return { ...data };
    }
    const merged: Record<string, unknown> = { ...data };
    for (const [key, prop] of Object.entries(properties as Record<string, unknown>)) {
        if (merged[key] !== undefined || !prop || typeof prop !== 'object' || Array.isArray(prop)) {
            continue;
        }
        if ('default' in prop) {
            merged[key] = (prop as { default: unknown }).default;
        }
    }
    return merged;
}

function hasMissingRequired(schema: Record<string, unknown>, data: unknown): boolean {
    const obj = isPlainObject(data) ? data : {};
    const required = schema.required;
    const properties = isPlainObject(schema.properties) ? (schema.properties as Record<string, Record<string, unknown>>) : undefined;
    if (Array.isArray(required)) {
        for (const key of required) {
            if (typeof key !== 'string' || !isFilled(obj[key])) {
                return true;
            }
        }
    }
    if (properties) {
        for (const [key, prop] of Object.entries(properties)) {
            if (prop?.type === 'object' && isPlainObject(obj[key]) && hasMissingRequired(prop, obj[key])) {
                return true;
            }
        }
    }
    return false;
}

function isFilled(value: unknown): boolean {
    if (value === undefined || value === null) {
        return false;
    }
    if (typeof value === 'string') {
        return value.trim().length > 0;
    }
    if (typeof value === 'number') {
        return !Number.isNaN(value);
    }
    if (Array.isArray(value)) {
        return value.length > 0;
    }
    return true;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return !!value && typeof value === 'object' && !Array.isArray(value);
}
