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
import type { EngineSchemaJson, EngineSchemaType } from './api/authz-api.types';

export interface ParsedAttribute {
    name: string;
    type: string;
}

export interface ParsedEntity {
    name: string;
    parents: string[];
    attributes: ParsedAttribute[];
}

export interface ParsedAction {
    name: string;
    principals: string[];
    resources: string[];
}

export interface ParsedSchema {
    entities: ParsedEntity[];
    actions: ParsedAction[];
}

function typeLabel(type: EngineSchemaType | undefined): string {
    if (!type) return '';
    if (type.type === 'Entity') return type.name ?? 'Entity';
    if (type.type === 'Set') return `Set<${typeLabel(type.element)}>`;
    return type.type ?? '';
}

function fqn(namespace: string, name: string): string {
    return namespace === '' ? name : `${namespace}::${name}`;
}

export function engineSchemaToParsed(json: EngineSchemaJson | null | undefined): ParsedSchema {
    const entities: ParsedEntity[] = [];
    const actions: ParsedAction[] = [];
    if (!json) return { entities, actions };

    // Every declared entity type, fully qualified. The engine strips the owning namespace from
    // references, so a bare name inside a namespace may be either local or a global — we resolve
    // it the way Cedar does (local first, then the global/empty namespace).
    const declared = new Set<string>();
    for (const [namespace, def] of Object.entries(json)) {
        for (const typeName of Object.keys(def.entityTypes ?? {})) {
            declared.add(fqn(namespace, typeName));
        }
    }

    function resolve(namespace: string, name: string): string {
        if (name.includes('::')) return name;
        const local = fqn(namespace, name);
        if (declared.has(local)) return local;
        if (declared.has(name)) return name;
        return local;
    }

    for (const [namespace, def] of Object.entries(json)) {
        for (const [typeName, entityType] of Object.entries(def.entityTypes ?? {})) {
            entities.push({
                name: resolve(namespace, typeName),
                parents: (entityType.memberOfTypes ?? []).map(parent => resolve(namespace, parent)),
                attributes: Object.entries(entityType.shape?.attributes ?? {}).map(([name, type]) => ({
                    name,
                    type: typeLabel(type),
                })),
            });
        }
        for (const [actionName, action] of Object.entries(def.actions ?? {})) {
            actions.push({
                name: actionName,
                principals: (action.appliesTo?.principalTypes ?? []).map(type => resolve(namespace, type)),
                resources: (action.appliesTo?.resourceTypes ?? []).map(type => resolve(namespace, type)),
            });
        }
    }

    return { entities, actions };
}
