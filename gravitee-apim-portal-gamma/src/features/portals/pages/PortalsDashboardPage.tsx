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
import { PortalsGrid } from '../components/PortalsGrid';
import { usePortals } from '../hooks/usePortals';

export function PortalsDashboardPage() {
    const { portals, loading, deletePortal, createPortal } = usePortals();

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-bold tracking-tight">Developer Portals</h1>
                <p className="text-sm text-muted-foreground">Preview and edit your developer portals.</p>
            </div>
            <PortalsGrid
                portals={portals}
                loading={loading}
                onDeletePortal={deletePortal}
                onCreatePortal={createPortal}
            />
        </div>
    );
}
