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
import { Badge, Button } from '@gravitee/graphene-core';
import { EyeOffIcon } from '@gravitee/graphene-core/icons';
import { useId } from 'react';

import type { ApiPortalPublishPortal } from '../../../types/apiDocumentation';
import type { PortalPublication } from '../../../utils/documentationTree';

export function ApiPublicationsList({
    publications,
    canUnpublish,
    onUnpublish,
}: Readonly<{
    publications: PortalPublication[];
    canUnpublish: boolean;
    onUnpublish: (portal: ApiPortalPublishPortal) => void;
}>) {
    const titleId = useId();

    return (
        <section className="rounded-lg border">
            <h2 id={titleId} className="border-b px-4 py-3 text-sm font-semibold">
                Published in
            </h2>
            <ul aria-labelledby={titleId} className="divide-y">
                {publications.map(({ portal, publication }) => (
                    <li key={portal.id} className="flex items-center justify-between gap-4 px-4 py-3">
                        <div className="flex min-w-0 flex-col gap-0.5">
                            <span className="flex items-center gap-2 text-sm font-medium">
                                <span className="truncate">{portal.name}</span>
                                {portal.default ? <Badge variant="outline">Default</Badge> : null}
                            </span>
                            <span className="truncate text-sm text-muted-foreground">Section: {publication.sectionName}</span>
                        </div>
                        {canUnpublish ? (
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                aria-label={`Unpublish from ${portal.name}`}
                                onClick={() => onUnpublish(portal)}
                            >
                                <EyeOffIcon className="size-4" aria-hidden />
                                Unpublish
                            </Button>
                        ) : null}
                    </li>
                ))}
            </ul>
        </section>
    );
}
