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

import { Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';
import { CircleHelpIcon, CircleStopIcon, PlayIcon } from '@gravitee/graphene-core/icons';

import type { InstanceState } from '../types/instance';
import { instanceStatusLabel, toInstanceStatusTone } from '../utils/instanceStatus';

export function GatewayInstanceStatusIcon({ state }: Readonly<{ state: InstanceState }>) {
    const tone = toInstanceStatusTone(state);
    const label = instanceStatusLabel(state);

    let icon;
    if (tone === 'running') {
        icon = <PlayIcon className="size-5 text-success" aria-hidden />;
    } else if (tone === 'error') {
        icon = <CircleStopIcon className="size-5 text-destructive" aria-hidden />;
    } else {
        icon = <CircleHelpIcon className="size-5 text-muted-foreground" aria-hidden />;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className="inline-flex" aria-label={label}>
                    {icon}
                </span>
            </TooltipTrigger>
            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
}
