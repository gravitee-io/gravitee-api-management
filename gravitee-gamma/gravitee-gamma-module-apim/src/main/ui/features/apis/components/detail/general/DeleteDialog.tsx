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
    Input,
    Label,
} from '@gravitee/graphene-core';
import { Trash2Icon } from '@gravitee/graphene-core/icons';
import { useEffect, useState } from 'react';

export function DeleteDialog({
    open,
    onOpenChange,
    apiName,
    onDelete,
    isLoading,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (v: boolean) => void;
    apiName: string;
    onDelete: () => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    const [confirm, setConfirm] = useState('');

    useEffect(() => {
        if (!open) setConfirm('');
    }, [open]);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Delete API permanently?</DialogTitle>
                    <DialogDescription>
                        This will permanently delete <strong>{apiName}</strong> along with all plans, subscriptions, and analytics data.
                        This action cannot be undone.
                    </DialogDescription>
                </DialogHeader>
                <div className="py-2 space-y-2">
                    <Label htmlFor="delete-confirm">
                        Type <span className="font-mono font-semibold">{apiName}</span> to confirm
                    </Label>
                    <Input
                        id="delete-confirm"
                        value={confirm}
                        onChange={e => setConfirm(e.target.value)}
                        placeholder={apiName}
                        autoComplete="off"
                    />
                    {error && <p className="text-sm text-destructive">{error}</p>}
                </div>
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline">
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" variant="destructive" disabled={confirm !== apiName || isLoading} onClick={onDelete}>
                        <Trash2Icon className="size-4" /> {isLoading ? 'Deleting…' : 'Delete permanently'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
