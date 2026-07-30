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

import { Button, Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@gravitee/graphene-core';
import { XIcon } from '@gravitee/graphene-core/icons';
import { useCallback } from 'react';

import type { ShardingTagRow } from '../types/entrypoint';

export function ShardingTagDetailSheet({
    tag,
    onClose,
}: Readonly<{
    tag: ShardingTagRow | null;
    onClose: () => void;
}>) {
    const handleOpenChange = useCallback(
        (open: boolean) => {
            if (!open) onClose();
        },
        [onClose],
    );

    return (
        <Sheet open={tag !== null} onOpenChange={handleOpenChange}>
            <SheetContent side="right" showCloseButton={false} className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader className="flex-row items-start justify-between gap-3 space-y-0">
                    <div className="space-y-1.5 text-left">
                        <SheetTitle>Sharding tag details</SheetTitle>
                        <SheetDescription>Read-only view of this sharding tag.</SheetDescription>
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
                {tag ? (
                    <dl className="space-y-4 px-4 py-2 text-sm">
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Key</dt>
                            <dd className="font-medium break-all">{tag.key || '—'}</dd>
                        </div>
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Name</dt>
                            <dd>{tag.name || '—'}</dd>
                        </div>
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Description</dt>
                            <dd className="text-muted-foreground">{tag.description || '—'}</dd>
                        </div>
                        <div className="space-y-1">
                            <dt className="text-muted-foreground">Restricted groups</dt>
                            <dd>{tag.restrictedGroupNames.length > 0 ? tag.restrictedGroupNames.join(', ') : '—'}</dd>
                        </div>
                    </dl>
                ) : null}
                <SheetFooter className="flex-row justify-end border-t">
                    <Button type="button" variant="outline" onClick={() => handleOpenChange(false)}>
                        Close
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
