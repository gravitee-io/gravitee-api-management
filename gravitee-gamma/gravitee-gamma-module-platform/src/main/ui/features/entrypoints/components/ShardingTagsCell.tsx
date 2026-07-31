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

interface ShardingTagsCellProps {
    tags?: string[];
}

export function ShardingTagsCell({ tags }: Readonly<ShardingTagsCellProps>) {
    const sorted = [...(tags ?? [])]
        .map(tag => tag.trim())
        .filter(Boolean)
        .sort((a, b) => a.localeCompare(b));
    if (sorted.length === 0) {
        return null;
    }
    return (
        <div className="flex flex-wrap items-center gap-1">
            {sorted.map(tag => (
                <Badge key={tag} variant="secondary" className="font-normal text-xs">
                    {tag}
                </Badge>
            ))}
        </div>
    );
}
