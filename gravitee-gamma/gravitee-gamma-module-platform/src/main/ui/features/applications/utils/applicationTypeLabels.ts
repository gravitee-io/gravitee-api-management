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
import type { ApplicationGrantType, ApplicationTypeConfig } from '../types/applicationCreate';

const TYPE_ORDER = ['simple', 'browser', 'web', 'native', 'backend_to_backend'] as const;

export function normalizeTypeId(typeId: string): string {
    return typeId.toLowerCase();
}

function typeSortIndex(typeId: string): number {
    const normalized = normalizeTypeId(typeId);
    const index = TYPE_ORDER.indexOf(normalized as (typeof TYPE_ORDER)[number]);
    return index === -1 ? TYPE_ORDER.length : index;
}

function normalizeGrantTypes(grantTypes: ApplicationGrantType[] | undefined): ApplicationGrantType[] {
    return (grantTypes ?? []).map(grantType => ({
        type: grantType.type,
        name: grantType.name,
        ...(grantType.response_types?.length ? { response_types: [...grantType.response_types] } : {}),
    }));
}

/** Normalize enabled types from GET /configuration/applications/types (gravitee-apim-rest-api applications/types.json). */
export function normalizeApplicationTypes(types: readonly ApplicationTypeConfig[]): ApplicationTypeConfig[] {
    return [...types]
        .map(type => ({
            id: normalizeTypeId(type.id),
            name: type.name,
            description: type.description,
            requires_redirect_uris: Boolean(type.requires_redirect_uris),
            allowed_grant_types: normalizeGrantTypes(type.allowed_grant_types),
            default_grant_types: normalizeGrantTypes(type.default_grant_types),
            mandatory_grant_types: normalizeGrantTypes(type.mandatory_grant_types),
        }))
        .sort((left, right) => typeSortIndex(left.id) - typeSortIndex(right.id));
}

export function isSameApplicationType(left: string, right: string): boolean {
    return normalizeTypeId(left) === normalizeTypeId(right);
}
