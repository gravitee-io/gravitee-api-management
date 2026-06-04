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
import { formatEntityUid } from './entity-adapter';
import type { EntityInstance } from './entity.types';

export type AttrInferredType = 'String' | 'Integer' | 'Decimal' | 'Boolean' | 'Set';

export function inferAttrType(value: unknown): AttrInferredType {
    if (typeof value === 'boolean') return 'Boolean';
    if (typeof value === 'number') return Number.isInteger(value) ? 'Integer' : 'Decimal';
    if (Array.isArray(value)) return 'Set';
    return 'String';
}

export interface GaplShape {
    readonly uid: { readonly type: string; readonly id: string };
    readonly attrs: Record<string, unknown>;
    readonly parents: string[];
}

/**
 * The canonical document the Policy Decision Point evaluates against: the entity
 * type, its canonical dotted uid, its visible attributes, and its parents as
 * canonical dotted strings. Mirrors what the PDP stages into the GAPL engine.
 */
export function buildGaplShape(entity: EntityInstance): GaplShape {
    return {
        uid: { type: entity.uid.type, id: formatEntityUid(entity.uid) },
        attrs: { ...entity.attrs },
        parents: entity.parents.map(formatEntityUid),
    };
}

export function toGaplJson(entity: EntityInstance): string {
    return JSON.stringify(buildGaplShape(entity), null, 2);
}
