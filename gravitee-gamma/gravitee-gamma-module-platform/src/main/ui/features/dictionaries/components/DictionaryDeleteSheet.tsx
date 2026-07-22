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
import { useCallback } from 'react';

import type { DictionaryListItem } from '../types/dictionary';

export function DictionaryDeleteSheet({
    open,
    dictionary,
    onClose,
    onConfirm,
    isDeleting,
}: Readonly<{
    open: boolean;
    dictionary: DictionaryListItem | undefined;
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}>) {
    const handleOpenChange = useCallback(
        (isOpen: boolean) => {
            if (!isOpen) onClose();
        },
        [onClose],
    );

    return (
        <Sheet open={open} onOpenChange={handleOpenChange}>
            <SheetContent side="right" className="flex max-h-full flex-col" style={{ maxWidth: '480px' }}>
                <SheetHeader>
                    <SheetTitle>Delete Dictionary</SheetTitle>
                    <SheetDescription>This action cannot be undone.</SheetDescription>
                </SheetHeader>

                <div className="flex-1 px-1 py-4">
                    <p className="text-sm text-muted-foreground">
                        Are you sure you want to delete <span className="font-medium text-foreground">{dictionary?.name}</span>
                        {dictionary?.key ? (
                            <>
                                {' '}
                                <span className="font-mono text-xs bg-muted px-1 py-0.5 rounded">{dictionary.key}</span>
                            </>
                        ) : null}
                        ? Policies that reference this dictionary will no longer resolve its values.
                    </p>
                </div>

                <SheetFooter className="shrink-0 flex-row justify-end gap-2 border-t pt-4">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isDeleting}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting}>
                        {isDeleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </SheetFooter>
            </SheetContent>
        </Sheet>
    );
}
