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
import { PortalConsumerPermissions } from '../components/PortalConsumerPermissions';

export function PermissionsPage() {
    return (
        <div className="mx-auto flex h-full min-h-0 max-w-screen-2xl flex-col gap-4 overflow-hidden px-6 pb-0 pt-6">
            <div className="shrink-0 space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">Permissions</h1>
                <p className="text-sm text-muted-foreground">
                    Manage tenants, groups, and what portal consumers can view or consume across every developer
                    portal.
                </p>
            </div>

            <PortalConsumerPermissions className="min-h-0 flex-1" />
        </div>
    );
}
