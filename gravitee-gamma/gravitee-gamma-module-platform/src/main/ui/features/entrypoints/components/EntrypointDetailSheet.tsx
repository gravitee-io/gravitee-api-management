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

import { Badge, Button, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';

import { ShardingTagsCell } from './ShardingTagsCell';
import type { EntrypointMappingRow } from '../types/entrypoint';

export function EntrypointDetailSheet({
    entrypoint,
    canEdit,
    onClose,
    onEdit,
}: Readonly<{
    entrypoint: EntrypointMappingRow | null;
    canEdit?: boolean;
    onClose: () => void;
    onEdit?: (row: EntrypointMappingRow) => void;
}>) {
    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open) onClose();
        },
        [onClose],
    );

    return (
        <Sheet open={entrypoint !== null} onOpenChange={handleOpenChange}>
            <SheetContent side="right" showCloseButton={false} className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <SheetTitle>Entrypoint details</SheetTitle>
                        <SheetDescription>Read-only view of this entrypoint mapping.</SheetDescription>
                    </div>
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-8 shrink-0"
                        aria-label="Close"
                        onClick={() => handleOpenChange(false)}
                    >
                        <XIcon className="size-4" aria-hidden />
                    </Button>
                </SheetHeader>
                {entrypoint ? (
                    <dl className="space-y-4 px-4 py-2 text-sm">
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Entrypoint</dt>
                            <dd className="font-medium break-all">{entrypoint.value || '—'}</dd>
                        </div>
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Type</dt>
                            <dd>
                                <Badge variant="secondary">{entrypoint.targetLabel}</Badge>
                            </dd>
                        </div>
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Sharding Tags</dt>
                            <dd>
                                <ShardingTagsCell tags={entrypoint.tagsName} />
                            </dd>
                        </div>
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Environments</dt>
                            <dd>{entrypoint.environmentNames.length > 0 ? entrypoint.environmentNames.join(', ') : 'All'}</dd>
                        </div>
                    </dl>
                ) : null}
                <SheetFooter className="flex-row justify-end gap-2 border-t">
                    <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                        Close
                    </Button>
                    {canEdit && entrypoint && onEdit ? (
                        <Button
                            type="button"
                            onClick={() => {
                                onEdit(entrypoint);
                            }}
                        >
                            Edit
                        </Button>
                    ) : null}
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
