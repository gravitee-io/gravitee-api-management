/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
} from '@gravitee/graphene-core';
import { useEffect, useId, useState } from 'react';

import { deriveTenantHrid } from '../utils/tenant-hrid';

interface CreatePortalTenantDialogProps {
    readonly open: boolean;
    readonly isPending: boolean;
    readonly onOpenChange: (open: boolean) => void;
    readonly onCreate: (input: { name: string; description?: string; hrid: string }) => void;
}

export function CreatePortalTenantDialog({
    open,
    isPending,
    onOpenChange,
    onCreate,
}: CreatePortalTenantDialogProps) {
    const nameId = useId();
    const descriptionId = useId();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');

    useEffect(() => {
        if (!open) {
            setName('');
            setDescription('');
        }
    }, [open]);

    const hrid = deriveTenantHrid(name);
    const canSubmit = name.trim().length > 0 && !isPending;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent style={{ width: 'min(92vw, 28rem)' }}>
                <DialogHeader>
                    <DialogTitle>Create tenant</DialogTitle>
                    <DialogDescription>
                        Give each customer their own isolated slice of this portal.
                    </DialogDescription>
                </DialogHeader>

                <div className="space-y-4 py-2">
                    <div className="space-y-2">
                        <Label htmlFor={nameId}>Name *</Label>
                        <Input
                            id={nameId}
                            value={name}
                            onChange={event => setName(event.target.value)}
                            placeholder="Acme Corp"
                            autoFocus
                        />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={descriptionId}>Description (optional)</Label>
                        <textarea
                            id={descriptionId}
                            value={description}
                            onChange={event => setDescription(event.target.value)}
                            placeholder="Payment integration partner"
                            rows={3}
                            className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                        />
                    </div>

                    <div className="space-y-1 text-sm">
                        <p className="text-muted-foreground">HRID (auto)</p>
                        <p className="font-mono">{hrid || '—'}</p>
                    </div>
                </div>

                <DialogFooter>
                    <Button variant="outline" onClick={() => onOpenChange(false)} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button
                        disabled={!canSubmit}
                        onClick={() =>
                            onCreate({
                                name: name.trim(),
                                description: description.trim() || undefined,
                                hrid,
                            })
                        }
                    >
                        {isPending ? 'Creating…' : 'Create'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
