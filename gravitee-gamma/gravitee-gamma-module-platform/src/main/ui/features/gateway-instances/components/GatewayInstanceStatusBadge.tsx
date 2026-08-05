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

import { Badge } from '@gravitee/graphene-core';
import { CircleCheckIcon, CircleHelpIcon, CircleStopIcon } from '@gravitee/graphene-core/icons';

import type { InstanceState } from '../types/instance';
import { toInstanceStatusTone } from '../utils/instanceStatus';

/**
 * Detail-header status badge.
 * Matches prototype: outline Badge + check icon + "Running" (Graphene tokens instead of raw emerald).
 */
export function GatewayInstanceStatusBadge({ state }: Readonly<{ state: string | undefined }>) {
    const tone = toInstanceStatusTone((state as InstanceState) || 'UNKNOWN');

    if (tone === 'running') {
        return (
            <Badge variant="outline" className="gap-1 rounded-md text-[11px] border-success/40 text-success">
                <CircleCheckIcon className="size-3 text-success" aria-hidden />
                Running
            </Badge>
        );
    }

    if (tone === 'error') {
        return (
            <Badge variant="outline" className="gap-1 rounded-md text-[11px] border-destructive/40 text-destructive">
                <CircleStopIcon className="size-3 text-destructive" aria-hidden />
                Stopped
            </Badge>
        );
    }

    return (
        <Badge variant="outline" className="gap-1 rounded-md text-[11px] text-muted-foreground">
            <CircleHelpIcon className="size-3 text-muted-foreground" aria-hidden />
            Unknown
        </Badge>
    );
}
