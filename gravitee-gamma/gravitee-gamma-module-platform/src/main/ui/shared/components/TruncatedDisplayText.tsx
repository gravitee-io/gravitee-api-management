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

import { cn, Tooltip, TooltipContent, TooltipTrigger } from '@gravitee/graphene-core';

import { TRUNCATED_LIST_TOOLTIP_CONTENT_CLASS, TruncatedListTooltipContent } from './TruncatedListTooltip';

export function TruncatedDisplayText({
    displayText,
    isPlaceholder,
    showTooltip,
    labels,
}: Readonly<{
    displayText: string;
    isPlaceholder: boolean;
    showTooltip: boolean;
    labels: readonly string[];
}>) {
    const textClassName = cn('min-w-0 flex-1 truncate text-left', isPlaceholder && 'text-muted-foreground');

    if (!showTooltip) {
        return <span className={textClassName}>{displayText}</span>;
    }

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span className={textClassName}>{displayText}</span>
            </TooltipTrigger>
            <TooltipContent side="bottom" align="start" className={TRUNCATED_LIST_TOOLTIP_CONTENT_CLASS}>
                <TruncatedListTooltipContent labels={labels} />
            </TooltipContent>
        </Tooltip>
    );
}
