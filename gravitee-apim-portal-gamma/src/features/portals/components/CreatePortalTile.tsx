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

import { createDefaultPortalScreenshot } from '../storage/dummy-portals';
import { ensureDefaultPageForPortal } from '../storage/ensure-default-page';
import { savePortal } from '../storage/portals.storage';

export function CreatePortalTile() {
    const navigate = useNavigate();

    const handleCreate = async () => {
        const id = crypto.randomUUID();
        const portal = {
            id,
            name: 'New Portal',
            screenshotDataUrl: createDefaultPortalScreenshot('New Portal'),
            updatedAt: new Date().toISOString(),
            layout: 'header-content-footer' as const,
            portalIconUrl: '',
            footerLinks: [],
            userMenuItems: [],
        };
        await savePortal(portal);
        await ensureDefaultPageForPortal(id);
        navigate(`/portals/${id}/edit`);
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
