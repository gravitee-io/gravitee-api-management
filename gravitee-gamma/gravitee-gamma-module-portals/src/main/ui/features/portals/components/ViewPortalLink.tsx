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
import { Button, Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@gravitee/graphene-core';
import { ExternalLinkIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

interface ViewPortalLinkProps {
    readonly url: string | null;
    readonly portalName: string;
}

function isPortalUrl(url: string | null): url is string {
    if (!url || !/^https?:\/\//i.test(url)) return false;
    try {
        const parsed = new URL(url);
        return parsed.protocol === 'https:' || parsed.protocol === 'http:';
    } catch {
        return false;
    }
}

export function ViewPortalLink({ url, portalName }: ViewPortalLinkProps) {
    const explanationId = useId();
    const label = `View Portal: ${portalName}`;

    if (isPortalUrl(url)) {
        return (
            <Button variant="outline" size="sm" asChild>
                <a href={url} target="_blank" rel="noopener noreferrer" aria-label={label}>
                    View Portal
                    <ExternalLinkIcon className="size-4" aria-hidden="true" />
                </a>
            </Button>
        );
    }

    return (
        <TooltipProvider>
            <Tooltip>
                <TooltipTrigger asChild>
                    <Button
                        variant="outline"
                        size="sm"
                        type="button"
                        aria-disabled="true"
                        aria-label={label}
                        aria-describedby={explanationId}
                        className="aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
                    >
                        View Portal
                        <ExternalLinkIcon className="size-4" aria-hidden="true" />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>No portal URL is configured.</TooltipContent>
            </Tooltip>
            <span id={explanationId} className="sr-only">
                No portal URL is configured.
            </span>
        </TooltipProvider>
    );
}
