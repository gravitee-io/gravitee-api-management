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

import { Badge, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';

import type { SharedPolicyGroup } from '../types/sharedPolicyGroup';

/** Mirrors classic Console's shared-policy-groups-state-badge.component.html. */
export function SharedPolicyGroupStatusBadge({ lifecycleState }: Readonly<{ lifecycleState: SharedPolicyGroup['lifecycleState'] }>) {
    switch (lifecycleState) {
        case 'DEPLOYED':
            return (
                <Badge variant="success" className="text-xs font-normal">
                    Deployed
                </Badge>
            );
        case 'UNDEPLOYED':
            return (
                <Badge variant="warning" className="text-xs font-normal">
                    Undeployed
                </Badge>
            );
        case 'PENDING':
            return (
                <TooltipProvider delayDuration={200}>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Badge variant="secondary" className="text-xs font-normal">
                                Pending
                            </Badge>
                        </TooltipTrigger>
                        <TooltipContent>Latest changes are not deployed</TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            );
        default:
            return null;
    }
}
