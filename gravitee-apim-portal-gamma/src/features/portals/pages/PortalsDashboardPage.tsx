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
import { Button } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { CreatePortalTemplateDialog } from '../components/CreatePortalTemplateDialog';
import { PortalDashboardSummaryCards } from '../components/PortalDashboardSummaryCards';
import { PortalGetMoreSection } from '../components/PortalGetMoreSection';
import { PortalOperationalLog } from '../components/PortalOperationalLog';
import { PortalTrafficOverview } from '../components/PortalTrafficOverview';
import { PortalsTable } from '../components/PortalsTable';
import { usePortals } from '../hooks/usePortals';
import type { PortalTemplateId } from '../templates/portal-templates';

export function PortalsDashboardPage() {
    const { portals, loading, deletePortal, createPortal } = usePortals();
    const navigate = useNavigate();
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();
    const [createOpen, setCreateOpen] = useState(false);
    const [isCreating, setIsCreating] = useState(false);

    const navigateToEditor = useCallback(
        (portalId: string) => {
            const editPath = `/portals/${portalId}/edit`;
            if (embeddedInConsole) {
                window.open(buildStandalonePortalUrl(standaloneEditorBaseUrl, editPath), '_blank', 'noopener,noreferrer');
                return;
            }
            navigate(editPath);
        },
        [embeddedInConsole, navigate, standaloneEditorBaseUrl],
    );

    const handleSelectTemplate = useCallback(
        async (templateId: PortalTemplateId) => {
            setIsCreating(true);
            try {
                const portal = await createPortal(templateId);
                setCreateOpen(false);
                navigateToEditor(portal.id);
            } finally {
                setIsCreating(false);
            }
        },
        [createPortal, navigateToEditor],
    );

    return (
        <div className="mx-auto max-w-screen-2xl space-y-6 p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start justify-between">
                <div className="space-y-1">
                    <h1 className="text-2xl font-bold tracking-tight">Developer Portals</h1>
                    <p className="text-sm text-muted-foreground">
                        Manage, monitor and publish your API portals.
                    </p>
                </div>
                <Button type="button" onClick={() => setCreateOpen(true)}>
                    <PlusIcon className="size-4" aria-hidden />
                    New Portal
                </Button>
            </div>

            <PortalDashboardSummaryCards activePortals={loading ? null : portals.length} />

            <PortalsTable portals={portals} loading={loading} onDeletePortal={deletePortal} />

            <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
                <PortalTrafficOverview />
                <PortalOperationalLog />
            </div>

            <PortalGetMoreSection />

            <CreatePortalTemplateDialog
                open={createOpen}
                isPending={isCreating}
                onOpenChange={open => {
                    if (!isCreating) {
                        setCreateOpen(open);
                    }
                }}
                onSelect={templateId => void handleSelectTemplate(templateId)}
            />
        </div>
    );
}
