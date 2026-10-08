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

const MAX_SCHEMA_ADAPT_DEPTH = 32;

const SSL_STORE_LOCATION_KEYS = ['path', 'content', 'certPath', 'certContent', 'keyPath', 'keyContent'] as const;

/** Omits blank SSL store location fields so path/content oneOf branches do not both match. */
export function sanitizeSharedConfigurationValues(values: Record<string, unknown>): Record<string, unknown> {
    const ssl = values.ssl;
    if (!ssl || typeof ssl !== 'object' || Array.isArray(ssl)) {
        return values;
    }
    const sslObj = ssl as Record<string, unknown>;
    return {
        ...values,
        ssl: {
            ...sslObj,
            trustStore: sanitizeSslStore(sslObj.trustStore),
            keyStore: sanitizeSslStore(sslObj.keyStore),
        },
    };
}

function sanitizeSslStore(store: unknown): unknown {
    if (!store || typeof store !== 'object' || Array.isArray(store)) {
        return store;
    }
    const next: Record<string, unknown> = { ...(store as Record<string, unknown>) };
    for (const key of SSL_STORE_LOCATION_KEYS) {
        const value = next[key];
        if (typeof value === 'string' && value.trim() === '') {
            delete next[key];
        }
    }
    return next;
}

/** Adapt endpoint shared-configuration JSON Schema for Gamma JsonSchemaForm display. */
export function adaptSharedConfigurationSchemaForForm(schema: Record<string, unknown> | JsonSchema): JsonSchema {
    return adaptNode(schema, 0, new WeakSet<object>()) as JsonSchema;
}

function adaptNode(node: unknown, depth: number, visited: WeakSet<object>, propertyKey?: string): unknown {
    if (!node || typeof node !== 'object' || Array.isArray(node)) {
        return node;
    }
    if (depth >= MAX_SCHEMA_ADAPT_DEPTH || visited.has(node)) {
        return node;
    }
    visited.add(node);
    const obj: Record<string, unknown> = { ...(node as Record<string, unknown>) };

    if (propertyKey === 'trustStore' || propertyKey === 'keyStore') {
        stripSslStoreElHints(obj);
    }

    if (obj.properties && typeof obj.properties === 'object' && !Array.isArray(obj.properties)) {
        obj.properties = Object.fromEntries(
            Object.entries(obj.properties as Record<string, unknown>).map(([key, value]) => [
                key,
                adaptNode(value, depth + 1, visited, key),
            ]),
        );
    }

    if (obj.gioExternalDefinitions && typeof obj.gioExternalDefinitions === 'object' && !Array.isArray(obj.gioExternalDefinitions)) {
        obj.gioExternalDefinitions = Object.fromEntries(
            Object.entries(obj.gioExternalDefinitions as Record<string, unknown>).map(([key, value]) => [
                key,
                adaptExternalDefinition(key, value, depth + 1, visited),
            ]),
        );
    }

    if (obj.definitions && typeof obj.definitions === 'object' && !Array.isArray(obj.definitions)) {
        obj.definitions = Object.fromEntries(
            Object.entries(obj.definitions as Record<string, unknown>).map(([key, value]) => [key, adaptNode(value, depth + 1, visited)]),
        );
    }

    if (obj.items !== undefined) {
        obj.items = Array.isArray(obj.items)
            ? obj.items.map(item => adaptNode(item, depth + 1, visited))
            : adaptNode(obj.items, depth + 1, visited);
    }
    for (const key of ['oneOf', 'anyOf', 'allOf'] as const) {
        const branch = obj[key];
        if (Array.isArray(branch)) {
            obj[key] = branch.map(item => adaptNode(item, depth + 1, visited));
        }
    }
    if (isHeadersArray(obj)) {
        relabelHeaderColumns(obj);
    }
    return obj;
}

function isHeadersArray(node: Record<string, unknown>): boolean {
    const gioConfig = node.gioConfig;
    return (
        node.type === 'array' &&
        !!gioConfig &&
        typeof gioConfig === 'object' &&
        !Array.isArray(gioConfig) &&
        (gioConfig as { uiType?: string }).uiType === 'gio-headers-array'
    );
}

function relabelHeaderColumns(arraySchema: Record<string, unknown>): void {
    const items = arraySchema.items;
    if (!items || typeof items !== 'object' || Array.isArray(items)) return;
    const properties = (items as { properties?: unknown }).properties;
    if (!properties || typeof properties !== 'object' || Array.isArray(properties)) return;
    const titles: Record<string, string> = { name: 'KEY', value: 'VALUE' };
    for (const [key, title] of Object.entries(titles)) {
        const property = (properties as Record<string, unknown>)[key];
        if (property && typeof property === 'object' && !Array.isArray(property)) {
            (properties as Record<string, unknown>)[key] = { ...(property as Record<string, unknown>), title };
        }
    }
}

function adaptExternalDefinition(defKey: string, value: unknown, depth: number, visited: WeakSet<object>): unknown {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
        return adaptNode(value, depth, visited);
    }
    const obj: Record<string, unknown> = { ...(value as Record<string, unknown>) };
    if (isSslStoreFieldDefKey(defKey)) {
        stripGioConfigEl(obj);
    }
    return adaptNode(obj, depth, visited);
}

function isSslStoreFieldDefKey(defKey: string): boolean {
    const key = defKey.toLowerCase();
    if (key.includes('hostnameverifier') || key.includes('trustall')) {
        return false;
    }
    return key.includes('ssltruststore') || key.includes('sslkeystore');
}

const SSL_STORE_EL_HINT = /\bEL\b|expression language|\bsecrets\b/i;

function stripSslStoreElHints(obj: Record<string, unknown>): void {
    if (typeof obj.description === 'string' && SSL_STORE_EL_HINT.test(obj.description)) {
        delete obj.description;
    }
    stripElFromSchemaSubtree(obj, 0, new WeakSet<object>());
}

function stripElFromSchemaSubtree(obj: Record<string, unknown>, depth: number, visited: WeakSet<object>): void {
    if (depth >= MAX_SCHEMA_ADAPT_DEPTH || visited.has(obj)) {
        return;
    }
    visited.add(obj);
    stripGioConfigEl(obj);
    if (obj.properties && typeof obj.properties === 'object' && !Array.isArray(obj.properties)) {
        for (const value of Object.values(obj.properties as Record<string, unknown>)) {
            if (value && typeof value === 'object' && !Array.isArray(value)) {
                stripElFromSchemaSubtree(value as Record<string, unknown>, depth + 1, visited);
            }
        }
    }
    if (obj.items !== undefined) {
        const items = Array.isArray(obj.items) ? obj.items : [obj.items];
        for (const item of items) {
            if (item && typeof item === 'object' && !Array.isArray(item)) {
                stripElFromSchemaSubtree(item as Record<string, unknown>, depth + 1, visited);
            }
        }
    }
    for (const key of ['oneOf', 'anyOf', 'allOf'] as const) {
        const branch = obj[key];
        if (Array.isArray(branch)) {
            for (const item of branch) {
                if (item && typeof item === 'object' && !Array.isArray(item)) {
                    stripElFromSchemaSubtree(item as Record<string, unknown>, depth + 1, visited);
                }
            }
        }
    }
}

function stripGioConfigEl(obj: Record<string, unknown>): void {
    const gioConfig = obj.gioConfig;
    if (!gioConfig || typeof gioConfig !== 'object' || Array.isArray(gioConfig)) {
        return;
    }
    const next = { ...(gioConfig as Record<string, unknown>) };
    delete next.el;
    if (Object.keys(next).length === 0) {
        delete obj.gioConfig;
    } else {
        obj.gioConfig = next;
    }
}
