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

import {
    Button,
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@gravitee/graphene-core';
import { TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { ShardingTagRow } from '../types/entrypoint';

function EntrypointValueList({ values }: Readonly<{ values: string[] }>) {
    return (
        <ul className="mt-1 list-disc space-y-1 pl-5">
            {values.map(value => (
                <li key={value}>
                    <strong className="break-all">{value}</strong>
                </li>
            ))}
        </ul>
    );
}

export function ShardingTagDeleteDialog({
    open,
    tag,
    entrypointsToUpdate = [],
    entrypointsToDelete = [],
    onClose,
    onConfirm,
    isDeleting,
}: Readonly<{
    open: boolean;
    tag: ShardingTagRow | null;
    /** Entrypoint values that keep other tags; this tag will be removed from them. */
    entrypointsToUpdate?: string[];
    /** Entrypoint values that only use this tag and will be deleted. */
    entrypointsToDelete?: string[];
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}>) {
    const displayName = tag?.name || tag?.key;

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="w-full max-w-md sm:max-w-md" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <TriangleAlertIcon className="size-5 shrink-0 text-warning" aria-hidden />
                        Delete Sharding Tag
                    </DialogTitle>
                    <DialogDescription>
                        Are you sure you want to delete the tag <strong className="text-foreground">{displayName}</strong>?
                    </DialogDescription>
                </DialogHeader>
                {entrypointsToUpdate.length > 0 || entrypointsToDelete.length > 0 ? (
                    <div className="space-y-3 text-sm text-muted-foreground">
                        {entrypointsToUpdate.length === 1 ? (
                            <p>
                                The tag will be removed for the entrypoint{' '}
                                <strong className="break-all text-foreground">{entrypointsToUpdate[0]}</strong>.
                            </p>
                        ) : null}
                        {entrypointsToUpdate.length > 1 ? (
                            <div>
                                <p>The tag will be removed from all these entrypoints:</p>
                                <EntrypointValueList values={entrypointsToUpdate} />
                            </div>
                        ) : null}
                        {entrypointsToDelete.length === 1 ? (
                            <p>
                                The <strong className="break-all text-foreground">{entrypointsToDelete[0]}</strong> entrypoint will be
                                deleted as it is only using this tag.
                            </p>
                        ) : null}
                        {entrypointsToDelete.length > 1 ? (
                            <div>
                                <p>The following entrypoints will be deleted as they are only using this tag:</p>
                                <EntrypointValueList values={entrypointsToDelete} />
                            </div>
                        ) : null}
                    </div>
                ) : null}
                <DialogFooter className="sm:justify-end gap-2">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isDeleting}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting || !tag}>
                        {isDeleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
