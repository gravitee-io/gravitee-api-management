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
import { Button, Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@gravitee/graphene-core';

import type { Metadata } from '../types/metadata';

export function MetadataDeleteDialog({
    metadata,
    isDeleting,
    onCancel,
    onConfirm,
}: Readonly<{
    metadata: Metadata | null;
    isDeleting: boolean;
    onCancel: () => void;
    onConfirm: () => void;
}>) {
    return (
        <Dialog open={metadata !== null} onOpenChange={open => !open && onCancel()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Delete Metadata</DialogTitle>
                    <DialogDescription>
                        Are you sure you want to delete <span className="font-medium text-foreground">{metadata?.name}</span>
                        {metadata?.key ? (
                            <>
                                {' '}
                                <span className="font-mono text-xs bg-muted px-1 py-0.5 rounded">{metadata.key}</span>
                            </>
                        ) : null}
                        ? APIs that inherit this key will lose its default value.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onCancel} disabled={isDeleting}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting || metadata === null}>
                        {isDeleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
