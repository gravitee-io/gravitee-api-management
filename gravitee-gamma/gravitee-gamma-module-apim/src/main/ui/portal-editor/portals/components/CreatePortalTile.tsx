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
import { Button, cn } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../app/PortalAppContext';
import type { PortalTemplateId } from '../templates/portal-templates';
import type { DeveloperPortal } from '../types';
import { CreatePortalTemplateDialog } from './CreatePortalTemplateDialog';

export function CreatePortalTile({
    onCreatePortal,
}: {
    readonly onCreatePortal: (templateId: PortalTemplateId) => Promise<DeveloperPortal>;
}) {
    const navigate = useNavigate();
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();
    const [dialogOpen, setDialogOpen] = useState(false);
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
                const portal = await onCreatePortal(templateId);
                setDialogOpen(false);
                navigateToEditor(portal.id);
            } finally {
                setIsCreating(false);
            }
        },
        [navigateToEditor, onCreatePortal],
    );

    return (
        <>
            <Button
                type="button"
                variant="outline"
                aria-label="Create new portal"
                className={cn('size-full border-dashed bg-transparent hover:bg-muted')}
                onClick={() => setDialogOpen(true)}
            >
                <PlusIcon className="size-8 text-muted-foreground" aria-hidden="true" />
            </Button>
            <CreatePortalTemplateDialog
                open={dialogOpen}
                isPending={isCreating}
                onOpenChange={open => {
                    if (!isCreating) {
                        setDialogOpen(open);
                    }
                }}
                onSelect={templateId => void handleSelectTemplate(templateId)}
            />
        </>
    );
}
