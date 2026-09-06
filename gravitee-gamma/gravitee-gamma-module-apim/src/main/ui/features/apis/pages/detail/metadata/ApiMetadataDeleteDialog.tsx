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

import type { ApiMetadata } from '../../../types/metadata';
import { isResettableMetadata } from '../../../utils/apiMetadata';

export function ApiMetadataDeleteDialog({
    open,
    metadata,
    onClose,
    onConfirm,
    isDeleting,
}: Readonly<{
    open: boolean;
    metadata: ApiMetadata | undefined;
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}>) {
    const isReset = metadata ? isResettableMetadata(metadata) : false;
    const title = isReset ? 'Reset global metadata' : 'Delete API metadata';
    const confirmLabel = isReset ? 'Reset' : 'Delete';
    const pendingLabel = isReset ? 'Resetting…' : 'Deleting…';

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>
                        {isReset ? (
                            <>
                                Are you sure you want to reset <span className="font-medium text-foreground">{metadata?.name}</span> to its
                                original value <span className="font-medium text-foreground">{metadata?.defaultValue}</span>?
                            </>
                        ) : (
                            <>
                                Are you sure you want to delete API metadata{' '}
                                <span className="font-medium text-foreground">{metadata?.name}</span>
                                {metadata?.key ? (
                                    <>
                                        {' '}
                                        <span className="font-mono text-xs bg-muted px-1 py-0.5 rounded">{metadata.key}</span>
                                    </>
                                ) : null}
                                ?
                            </>
                        )}
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isDeleting}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting || !metadata}>
                        {isDeleting ? pendingLabel : confirmLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
