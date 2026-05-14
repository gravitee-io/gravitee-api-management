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
import { Button, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';
import { InfoIcon, PlusIcon } from '@gravitee/graphene-core/icons';

export interface ApplicationsPageHeaderProps {
    readonly canCreate: boolean;
    readonly onRegisterApplication: () => void;
    readonly showInfoTooltip?: boolean;
}

export function ApplicationsPageHeader({ canCreate, onRegisterApplication, showInfoTooltip = false }: ApplicationsPageHeaderProps) {
    return (
        <div className="flex items-start justify-between">
            <div className="space-y-1">
                <div className="flex items-center gap-2">
                    <h1 className="text-2xl font-semibold tracking-tight">Applications</h1>
                    {showInfoTooltip ? (
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <button type="button" className="text-muted-foreground" aria-label="About applications">
                                    <InfoIcon className="size-4" aria-hidden />
                                </button>
                            </TooltipTrigger>
                            <TooltipContent>Consumer applications that subscribe to your APIs and receive credentials.</TooltipContent>
                        </Tooltip>
                    ) : null}
                </div>
                <p className="text-sm text-muted-foreground">
                    Manage consumer applications and their API subscriptions across the platform.
                </p>
            </div>
            {canCreate && (
                <Button onClick={onRegisterApplication} className="shrink-0">
                    <PlusIcon className="size-4" aria-hidden />
                    Register Application
                </Button>
            )}
        </div>
    );
}
