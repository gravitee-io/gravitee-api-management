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
import { useMemo } from 'react';

import { useSettingsContext } from '../context/SettingsContext';
import { ENVIRONMENTS, type MockEnvironment } from '../mock/environments';
import type { SettingsNavGroup, SettingsNavItem } from '../mock/navigation-items';
import { ALL_SETTINGS_ITEMS, SETTINGS_NAV_GROUPS } from '../mock/navigation-items';
import type { MockPersona } from '../mock/personas';

export type AccessLevel = 'full' | 'read-only' | 'denied';

function hasOrgReadAccess(persona: MockPersona, permissionKey: string): boolean {
    return persona.orgPermissions.includes(`organization-${permissionKey}-r`);
}

function hasEnvReadAccess(persona: MockPersona, envId: string, permissionKey: string): boolean {
    const envPerms = persona.envPermissions[envId];
    if (!envPerms) return false;
    return envPerms.permissions.includes(`environment-${permissionKey}-r`);
}

function hasEnvWriteAccess(persona: MockPersona, envId: string, permissionKey: string): boolean {
    const envPerms = persona.envPermissions[envId];
    if (!envPerms) return false;
    return envPerms.permissions.includes(`environment-${permissionKey}-u`);
}

function hasAnyEnvAccess(persona: MockPersona, permissionKey: string): boolean {
    return ENVIRONMENTS.some(env => hasEnvReadAccess(persona, env.id, permissionKey));
}

function computeAccessLevel(persona: MockPersona, item: SettingsNavItem, envId: string | null): AccessLevel {
    if (item.scope === 'org') {
        return hasOrgReadAccess(persona, item.permissionKey) ? 'full' : 'denied';
    }
    if (!envId) return 'denied';
    if (!hasEnvReadAccess(persona, envId, item.permissionKey)) return 'denied';
    if (!hasEnvWriteAccess(persona, envId, item.permissionKey)) return 'read-only';
    return 'full';
}

export interface SettingsPermissions {
    readonly visibleGroups: readonly SettingsNavGroup[];
    readonly userEnvironments: readonly MockEnvironment[];
    readonly accessLevel: AccessLevel;
    readonly selectedItem: SettingsNavItem | undefined;
    getAccessibleEnvironmentsForItem: (itemKey: string) => readonly MockEnvironment[];
    getFirstAccessibleEnvForItem: (itemKey: string) => string | null;
}

function getUserEnvironments(persona: MockPersona): readonly MockEnvironment[] {
    return ENVIRONMENTS.filter(env => env.id in persona.envPermissions);
}

export function useSettingsPermissions(): SettingsPermissions {
    const { state } = useSettingsContext();
    const { persona, selectedItemKey, selectedEnvId } = state;

    return useMemo(() => {
        const visibleGroups = SETTINGS_NAV_GROUPS.map(group => ({
            ...group,
            items: group.items.filter(item => {
                if (item.scope === 'org') return hasOrgReadAccess(persona, item.permissionKey);
                return hasAnyEnvAccess(persona, item.permissionKey);
            }),
        })).filter(group => group.items.length > 0);

        const selectedItem = ALL_SETTINGS_ITEMS.find(i => i.key === selectedItemKey);
        const userEnvironments = getUserEnvironments(persona);

        function getAccessibleEnvironmentsForItem(itemKey: string): readonly MockEnvironment[] {
            const item = ALL_SETTINGS_ITEMS.find(i => i.key === itemKey);
            if (!item || item.scope === 'org') return [];
            return ENVIRONMENTS.filter(env => hasEnvReadAccess(persona, env.id, item.permissionKey));
        }

        function getFirstAccessibleEnvForItem(itemKey: string): string | null {
            const envs = getAccessibleEnvironmentsForItem(itemKey);
            return envs[0]?.id ?? null;
        }

        const accessLevel = selectedItem ? computeAccessLevel(persona, selectedItem, selectedEnvId) : 'denied';

        return {
            visibleGroups,
            userEnvironments,
            accessLevel,
            selectedItem,
            getAccessibleEnvironmentsForItem,
            getFirstAccessibleEnvForItem,
        };
    }, [persona, selectedItemKey, selectedEnvId]);
}
