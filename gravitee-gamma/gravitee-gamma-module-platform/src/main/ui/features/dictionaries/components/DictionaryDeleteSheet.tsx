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
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="w-full max-w-md sm:max-w-md" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <TriangleAlertIcon className="size-5 shrink-0 text-destructive" aria-hidden />
                        Delete Dictionary
                    </DialogTitle>
                    <DialogDescription>
                        Are you sure you want to delete <strong>{dictionary?.name}</strong>? This will permanently remove all of its
                        properties. This action cannot be undone.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="sm:justify-end gap-2">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isDeleting}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting || !dictionary}>
                        {isDeleting ? 'Deleting…' : 'Delete Dictionary'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
