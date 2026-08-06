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
import { TriangleAlertIcon } from '@gravitee/graphene-core/icons';

import type { Group } from '../types/group';
import { isPrimaryOwnerGroup } from '../utils/groupPermissions';

export function GroupDeleteSheet({
    open,
    group,
    onClose,
    onConfirm,
    isDeleting,
}: Readonly<{
    open: boolean;
    group: Group | undefined;
    onClose: () => void;
    onConfirm: () => void;
    isDeleting: boolean;
}>) {
    // Delete is always offered (classic doesn't hide it either) — a primary-owner group is rejected by the
    // backend with StillPrimaryOwnerException regardless, so surface that as soon as the dialog opens
    // instead of letting the user hit an API error after confirming.
    const blocked = group ? isPrimaryOwnerGroup(group) : false;

    if (blocked) {
        return (
            <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
                <DialogContent className="max-w-sm">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            <TriangleAlertIcon className="size-5 text-destructive" aria-hidden />
                            Delete group?
                        </DialogTitle>
                        <DialogDescription>
                            <span className="font-medium text-foreground">{group?.name}</span> cannot be deleted while it still has a
                            primary owner membership. Reassign or remove the primary owner first.
                        </DialogDescription>
                    </DialogHeader>
                    <DialogFooter className="border-t px-6 py-4 gap-2">
                        <Button type="button" variant="outline" onClick={onClose}>
                            Cancel
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        );
    }

    return (
        <Dialog open={open} onOpenChange={isOpen => !isOpen && onClose()}>
            <DialogContent className="max-w-sm">
                <DialogHeader>
                    <DialogTitle>Delete Group</DialogTitle>
                    <DialogDescription>
                        Are you sure you want to delete <span className="font-medium text-foreground">{group?.name}</span>? Members will
                        lose any access granted through this group.
                    </DialogDescription>
                </DialogHeader>
                <DialogFooter className="border-t px-6 py-4 gap-2">
                    <Button type="button" variant="outline" onClick={onClose} disabled={isDeleting}>
                        Cancel
                    </Button>
                    <Button type="button" variant="destructive" onClick={onConfirm} disabled={isDeleting || !group}>
                        {isDeleting ? 'Deleting…' : 'Delete'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
