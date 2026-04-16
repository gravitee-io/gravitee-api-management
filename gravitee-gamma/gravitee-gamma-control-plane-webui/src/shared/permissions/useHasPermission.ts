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
import { useMemo, useSyncExternalStore } from 'react';

import { permissionService } from './permission-service';
import type { PermissionCheck } from './types';

export type UseHasPermissionOptions = PermissionCheck;

export function useHasPermission(options: UseHasPermissionOptions): boolean {
    const version = useSyncExternalStore(
        cb => permissionService.subscribe(cb),
        () => permissionService.getSnapshot(),
        () => permissionService.getSnapshot(),
    );

    const optionsKey = JSON.stringify(options);
    return useMemo(() => {
        void version;
        const parsed = JSON.parse(optionsKey) as UseHasPermissionOptions;
        if (parsed.anyOf) {
            return permissionService.hasAnyOf(parsed.anyOf);
        }
        if (parsed.allOf) {
            return permissionService.hasAllOf(parsed.allOf);
        }
        return false;
    }, [version, optionsKey]);
}
