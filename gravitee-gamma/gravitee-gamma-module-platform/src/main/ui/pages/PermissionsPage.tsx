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
import { UnifiedPermissionsScreen } from '@portal-gamma/features/permissions/components/UnifiedPermissionsScreen';

/**
 * The unified variant of the portals-module Permissions screen. Both directories live here:
 * portal consumers (tenants, groups, View/Consume grants) and console users and teams (authoring roles).
 */
export function PermissionsPage() {
    return (
        <div className="flex h-full min-h-0 flex-col gap-4 overflow-hidden">
            <div className="shrink-0">
                <h1 className="text-2xl font-semibold tracking-tight">Permissions</h1>
                <p className="text-sm text-muted-foreground">
                    Grant portal consumers access to assets and portal content, and decide which console users and
                    teams author the documentation.
                </p>
            </div>

            <UnifiedPermissionsScreen className="min-h-0 flex-1" />
        </div>
    );
}
