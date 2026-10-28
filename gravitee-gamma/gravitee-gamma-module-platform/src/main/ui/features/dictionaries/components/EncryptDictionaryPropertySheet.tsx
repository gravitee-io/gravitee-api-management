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
import { LockIcon } from '@gravitee/graphene-core/icons';

export function EncryptDictionaryPropertySheet({
    open,
    propertyKey,
    onClose,
    onConfirm,
    isEncrypting,
}: Readonly<{
    open: boolean;
    propertyKey: string | undefined;
    onClose: () => void;
    onConfirm: () => void;
    isEncrypting: boolean;
}>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="w-full max-w-md sm:max-w-md" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle className="flex items-center gap-2">
                        <LockIcon className="size-5 shrink-0" aria-hidden />
                        Encrypt Property
                    </DialogTitle>
                    <DialogDescription>
                        Encrypt the value of property <strong className="font-mono text-xs">{propertyKey}</strong>? Encrypting a value
                        cannot be undone: it can no longer be read or turned back into plaintext.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="sm:justify-end gap-2">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isEncrypting}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" onClick={onConfirm} disabled={isEncrypting || !propertyKey}>
                        {isEncrypting ? 'Encrypting…' : 'Encrypt'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
