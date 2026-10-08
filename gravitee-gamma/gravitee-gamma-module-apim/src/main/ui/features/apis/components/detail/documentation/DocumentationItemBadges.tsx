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
import type { ReactNode } from 'react';

import type { ApiDocumentationItem, PortalVisibility } from '../../../types/apiDocumentation';

type ItemType = ApiDocumentationItem['type'];

const ITEM_NOUNS: Record<ItemType, string> = { PAGE: 'page', FOLDER: 'folder', LINK: 'link' };

export function ItemPublishedBadge({ published, itemType }: Readonly<{ published: boolean; itemType: ItemType }>) {
    return published ? (
        <ExplainedBadge explanation="Shown in the developer portal.">
            <Badge variant="success">Published</Badge>
        </ExplainedBadge>
    ) : (
        <ExplainedBadge explanation={`Not shown in the developer portal until the ${ITEM_NOUNS[itemType]} is published.`}>
            <Badge variant="outline">Unpublished</Badge>
        </ExplainedBadge>
    );
}

export function ItemAccessBadge({ visibility, itemType }: Readonly<{ visibility: PortalVisibility; itemType: ItemType }>) {
    return visibility === 'PRIVATE' ? (
        <ExplainedBadge explanation={`Only signed-in users can view this ${ITEM_NOUNS[itemType]}.`}>
            <Badge variant="secondary">Private</Badge>
        </ExplainedBadge>
    ) : (
        <ExplainedBadge explanation={`Users don't need to sign in to view this ${ITEM_NOUNS[itemType]}.`}>
            <Badge variant="outline">Public</Badge>
        </ExplainedBadge>
    );
}

function ExplainedBadge({ explanation, children }: Readonly<{ explanation: string; children: ReactNode }>) {
    return (
        <TooltipProvider delayDuration={200}>
            <Tooltip>
                <TooltipTrigger asChild>{children}</TooltipTrigger>
                <TooltipContent>{explanation}</TooltipContent>
            </Tooltip>
        </TooltipProvider>
    );
}
