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

import type { PortalSettings } from '../../security-plan-types/services/portalSettings';
import { isPortalSettingReadonly } from '../../security-plan-types/utils/isPortalSettingReadonly';

export const PRIMARY_OWNER_MODES = ['HYBRID', 'USER', 'GROUP'] as const;
export type PrimaryOwnerMode = (typeof PRIMARY_OWNER_MODES)[number];
export const DEFAULT_PRIMARY_OWNER_MODE: PrimaryOwnerMode = 'HYBRID';

export type PrimaryOwnerResource = 'API' | 'API Product';

export interface PrimaryOwnerModeFormState {
    api: PrimaryOwnerMode;
    apiProduct: PrimaryOwnerMode;
}

export interface PrimaryOwnerModeReadonly {
    api: boolean;
    apiProduct: boolean;
}

export interface PrimaryOwnerModeOption {
    readonly value: PrimaryOwnerMode;
    readonly title: string;
    readonly summary: string;
    readonly implication: string;
}

export const API_PRIMARY_OWNER_READONLY_PROPERTY = 'api.primaryOwnerMode';
export const API_PRODUCT_PRIMARY_OWNER_READONLY_PROPERTY = 'apiProduct.primaryOwnerMode';

export function isPrimaryOwnerMode(value: string): value is PrimaryOwnerMode {
    return (PRIMARY_OWNER_MODES as readonly string[]).includes(value);
}

export function parsePrimaryOwnerMode(value: string | undefined): PrimaryOwnerMode {
    const normalized = value?.trim().toUpperCase();
    return normalized && isPrimaryOwnerMode(normalized) ? normalized : DEFAULT_PRIMARY_OWNER_MODE;
}

export function buildPrimaryOwnerModeFormState(settings: PortalSettings | undefined): PrimaryOwnerModeFormState {
    return {
        api: parsePrimaryOwnerMode(settings?.api?.primaryOwnerMode),
        apiProduct: parsePrimaryOwnerMode(settings?.apiProduct?.primaryOwnerMode),
    };
}

export function getPrimaryOwnerModeReadonly(settings: PortalSettings | undefined): PrimaryOwnerModeReadonly {
    return {
        api: isPortalSettingReadonly(settings, API_PRIMARY_OWNER_READONLY_PROPERTY),
        apiProduct: isPortalSettingReadonly(settings, API_PRODUCT_PRIMARY_OWNER_READONLY_PROPERTY),
    };
}

export function applyReadonlyPrimaryOwnerModes(
    local: PrimaryOwnerModeFormState,
    saved: PrimaryOwnerModeFormState,
    readonly: PrimaryOwnerModeReadonly,
): PrimaryOwnerModeFormState {
    return {
        api: readonly.api ? saved.api : local.api,
        apiProduct: readonly.apiProduct ? saved.apiProduct : local.apiProduct,
    };
}

export function isPrimaryOwnerModeDirty(
    local: PrimaryOwnerModeFormState,
    saved: PrimaryOwnerModeFormState,
    readonly: PrimaryOwnerModeReadonly,
): boolean {
    return (!readonly.api && local.api !== saved.api) || (!readonly.apiProduct && local.apiProduct !== saved.apiProduct);
}

export function primaryOwnerModeOptions(resource: PrimaryOwnerResource): readonly PrimaryOwnerModeOption[] {
    const article = resource === 'API' ? 'an API' : 'an API Product';
    return [
        {
            value: 'HYBRID',
            title: 'Hybrid',
            summary: `HYBRID: ${article} primary owner can be either a user or a group (Default)`,
            implication: 'A user or a group can be the primary owner. This is the default.',
        },
        {
            value: 'USER',
            title: 'User',
            summary: `USER: ${article} primary owner can only be a user`,
            implication: 'Only a person can be the primary owner. Groups cannot take that role.',
        },
        {
            value: 'GROUP',
            title: 'Group',
            summary: `GROUP: ${article} primary owner can only be a group`,
            implication: 'Only a group can be the primary owner. Ownership stays with the team, not one person.',
        },
    ];
}
