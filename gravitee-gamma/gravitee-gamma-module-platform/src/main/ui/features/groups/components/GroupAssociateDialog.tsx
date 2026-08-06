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

/** Mirrors classic group.component.ts's "Add Group To Existing APIs/API Products/Applications" confirm
 *  dialogs — same title/body/button copy, one component parametrized by the target type label. */
export function GroupAssociateDialog({
    open,
    typeLabel,
    onClose,
    onConfirm,
    isAssociating,
}: Readonly<{
    open: boolean;
    typeLabel: string;
    onClose: () => void;
    onConfirm: () => void;
    isAssociating: boolean;
}>) {
    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Add group to existing {typeLabel}</DialogTitle>
                    <DialogDescription>
                        You are trying to add the group to all the existing {typeLabel.toLowerCase()}. Do you want to continue?
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isAssociating}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={onConfirm} disabled={isAssociating}>
                        {isAssociating ? 'Adding…' : 'Continue'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
