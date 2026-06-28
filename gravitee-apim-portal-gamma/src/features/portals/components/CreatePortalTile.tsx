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
import { useNavigate } from 'react-router-dom';

import { buildStandalonePortalUrl, usePortalApp } from '../../../app/PortalAppContext';
import { seedDefaultNavigationForPortal } from '../storage/seed-default-navigation';
import { savePortal } from '../storage/portals.storage';
import { DEFAULT_PORTAL_LABEL } from '../types';

export function CreatePortalTile() {
    const navigate = useNavigate();
    const { embeddedInConsole, standaloneEditorBaseUrl } = usePortalApp();

    const handleCreate = async () => {
        const id = crypto.randomUUID();
        const portal = {
            id,
            name: 'New Portal',
            screenshotDataUrl: '',
            updatedAt: new Date().toISOString(),
            layout: 'header-content-footer' as const,
            portalIconUrl: '',
            portalLabel: DEFAULT_PORTAL_LABEL,
            footerLinks: [],
            userMenuItems: [],
        };
        await savePortal(portal);
        await seedDefaultNavigationForPortal(id);

        const editPath = `/portals/${id}/edit`;
        if (embeddedInConsole) {
            window.open(buildStandalonePortalUrl(standaloneEditorBaseUrl, editPath), '_blank', 'noopener,noreferrer');
            return;
        }

        navigate(editPath);
    };

    return (
        <Button
            type="button"
            variant="outline"
            aria-label="Create new portal"
            className={cn('size-full border-dashed bg-transparent hover:bg-muted')}
            onClick={() => void handleCreate()}
        >
            <PlusIcon className="size-8 text-muted-foreground" aria-hidden="true" />
        </Button>
    );
}
