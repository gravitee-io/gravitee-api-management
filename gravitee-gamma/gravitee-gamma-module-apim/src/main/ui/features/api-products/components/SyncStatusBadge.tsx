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
import { Badge, cn } from '@gravitee/graphene-core';
import { CircleCheckIcon, TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { ApiProductDeploymentState } from '../types/apiProduct';

interface SyncStatusBadgeProps {
    state: ApiProductDeploymentState | undefined;
    /** compact=true uses tighter padding for sidebar headers; default is table row size */
    compact?: boolean;
}

export function SyncStatusBadge({ state, compact }: SyncStatusBadgeProps) {
    const cls = compact ? 'gap-1 h-5 px-1.5 text-xs' : 'gap-1 text-xs';

    if (state === 'NEED_REDEPLOY') {
        return (
            <Badge variant="outline" className={cn(cls, 'text-warning border-warning/30')}>
                <TriangleAlertIcon className="size-3" aria-hidden />
                Out of sync
            </Badge>
        );
    }
    if (state === 'DEPLOYED') {
        return (
            <Badge variant="outline" className={cn(cls, 'text-success border-success/20')}>
                <CircleCheckIcon className="size-3" aria-hidden />
                {compact ? 'Synced' : 'In sync'}
            </Badge>
        );
    }
    return <span className="text-muted-foreground text-xs">—</span>;
}
