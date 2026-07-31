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

interface ShardingTagsCellProps {
    /** Tag keys stored on the API / API Product `tags` set. */
    tags?: string[];
}

/**
 * List-table cell for sharding tags. Matches the classic console: shows the first (sorted) tag key
 * plus an "N more" badge whose tooltip lists every tag. Tags are displayed by key (not name),
 * exactly as the classic API / API Product lists do. The tooltip uses `delayDuration={0}` so it
 * appears instantly on hover, matching classic's Material tooltip.
 */
export function ShardingTagsCell({ tags }: Readonly<ShardingTagsCellProps>) {
    if (!tags || tags.length === 0) {
        return <span className="text-muted-foreground text-xs">—</span>;
    }
    const sorted = [...tags].sort((a, b) => a.localeCompare(b));
    const moreCount = sorted.length - 1;
    return (
        <div className="flex items-center gap-1">
            <span className="text-sm">{sorted[0]}</span>
            {moreCount > 0 ? (
                <TooltipProvider delayDuration={0}>
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Badge variant="secondary" className="text-xs tabular-nums cursor-default">
                                {moreCount} more
                            </Badge>
                        </TooltipTrigger>
                        <TooltipContent>{sorted.join(', ')}</TooltipContent>
                    </Tooltip>
                </TooltipProvider>
            ) : null}
        </div>
    );
}
