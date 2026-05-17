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

export function ApplicationDeleteDialog({
    open,
    onOpenChange,
    applicationName,
    onDelete,
    isLoading,
    error,
}: Readonly<{
    open: boolean;
    onOpenChange: (open: boolean) => void;
    applicationName: string;
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
            <DialogContent
                className="sm:max-w-md"
                style={{ width: 'min(90vw, 28rem)', maxWidth: 'min(90vw, 28rem)' }}
                showCloseButton={false}
            >
                <DialogHeader>
                    <DialogTitle>Archive this application?</DialogTitle>
                    <DialogDescription>
                        The application <strong>{applicationName}</strong> will be archived. Active subscriptions will be closed.
                    </DialogDescription>
                </DialogHeader>
                <div className="space-y-2 py-2">
                    <Label htmlFor="application-delete-confirm" className="text-sm">
                        Type <span className="font-mono font-semibold">{applicationName}</span> to confirm
                    </Label>
                    <Input
                        id="application-delete-confirm"
                        value={confirm}
                        onChange={e => setConfirm(e.target.value)}
                        placeholder={applicationName}
                        autoComplete="off"
                    />
                    {error ? <p className="text-sm text-destructive">{error}</p> : null}
                </div>
                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" onClick={() => setConfirm('')}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button type="button" variant="destructive" disabled={confirm !== applicationName || isLoading} onClick={onDelete}>
                        <Trash2Icon className="size-4" aria-hidden />
                        {isLoading ? 'Archiving…' : 'Archive application'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
