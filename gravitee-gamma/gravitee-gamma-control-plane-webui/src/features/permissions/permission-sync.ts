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
import { normalizeOrganizationPermissionsFromRoles, permissionService } from '@gravitee/gamma-modules-sdk';

import { loadEnvironmentPermissions } from './loadEnvironmentPermissions';
import { useAuthStore } from '../auth/auth.store';
import { useEnvironmentStore } from '../environment/environment.store';

type EnvironmentSnapshot = ReturnType<typeof useEnvironmentStore.getState>;

/**
 * Bumped whenever the permissions held are dropped: an answer to a request made before belongs to a user or an environment
 * left since, and would pass for the permissions of the current one.
 */
let generation = 0;

function dropPermissions(drop: () => void): void {
    generation += 1;
    drop();
}

/** Loads env permissions when both a user and an environment are selected (shared catch logging). */
function loadEnvironmentPermissionsIfContextReady(environmentId: string | undefined): void {
    if (!environmentId || !useAuthStore.getState().user) return;
    const requestedIn = generation;
    const isCurrent = () => generation === requestedIn;
    loadEnvironmentPermissions(environmentId, isCurrent).catch((error: unknown) => {
        console.error('Failed to load environment permissions', error);
        if (isCurrent()) {
            permissionService.markLoadFailed('environment');
        }
    });
}

export function reloadEnvironmentPermissions(): void {
    loadEnvironmentPermissionsIfContextReady(useEnvironmentStore.getState().environmentId);
}

function onEnvironmentChangeForPermissions(state: EnvironmentSnapshot, prev: EnvironmentSnapshot): void {
    if (state.environmentId === prev.environmentId) return;
    dropPermissions(() => permissionService.clear('environment'));
    loadEnvironmentPermissionsIfContextReady(state.environmentId);
}

/**
 * Establishes reactive subscriptions so that permission state is always
 * derived from auth and environment state — no manual sync needed.
 *
 * Returns a teardown function that removes the subscriptions.
 */
export function startPermissionSync(): () => void {
    const unsubAuth = useAuthStore.subscribe((state, prev) => {
        if (state.user === prev.user) return;
        dropPermissions(() => permissionService.reset());
        if (state.user) {
            permissionService.load('organization', normalizeOrganizationPermissionsFromRoles(state.user.roles));
            // Env store may not emit a change when env id is unchanged (e.g. login); reload env perms after reset.
            loadEnvironmentPermissionsIfContextReady(useEnvironmentStore.getState().environmentId);
        }
    });

    const unsubEnv = useEnvironmentStore.subscribe(onEnvironmentChangeForPermissions);

    return () => {
        unsubAuth();
        unsubEnv();
    };
}
