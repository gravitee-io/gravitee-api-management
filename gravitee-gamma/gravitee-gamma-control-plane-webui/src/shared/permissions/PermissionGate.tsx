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
import type { ReactElement, ReactNode } from 'react';

import type { PermissionCheck } from './types';
import { useHasPermission } from './useHasPermission';

export type PermissionGateProps = PermissionCheck & {
    readonly children: ReactNode;
    readonly fallback?: ReactNode;
};

export function PermissionGate(props: PermissionGateProps): ReactElement | null {
    const allowed = useHasPermission(props.anyOf ? { anyOf: props.anyOf } : { allOf: props.allOf ?? [] });

    if (!allowed) {
        return <>{props.fallback ?? null}</>;
    }

    return <>{props.children}</>;
}
