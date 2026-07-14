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
import { TriangleAlertIcon } from '@gravitee/graphene-core/icons';
import { useNavigate } from 'react-router-dom';

import { usePortalsNavigation } from '../../portals/config/navigation';

interface TenantPreviewBannerProps {
    readonly tenantName: string;
    readonly portalId: string;
}

export function TenantPreviewBanner({ tenantName, portalId }: TenantPreviewBannerProps) {
    const navigate = useNavigate();
    const { portalViewPath } = usePortalsNavigation();

    return (
        <div className="flex shrink-0 items-center justify-between gap-4 border-b border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-950 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-100">
            <div className="flex items-center gap-2">
                <TriangleAlertIcon className="size-4 shrink-0" aria-hidden="true" />
                <span>
                    Previewing portal as <strong>{tenantName}</strong> user
                </span>
            </div>
            <Button
                variant="outline"
                size="sm"
                onClick={() => navigate(portalViewPath(portalId), { replace: true })}
            >
                Exit preview
            </Button>
        </div>
    );
}
