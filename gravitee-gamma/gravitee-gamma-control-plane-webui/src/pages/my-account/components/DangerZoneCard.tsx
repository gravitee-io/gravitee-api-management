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
    Card,
    CardContent,
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
import { useEffect, useId, useState } from 'react';

import { displayDangerZone } from '../myAccount.mapping';
import type { ConsoleAuthenticationSettings } from '../myAccount.types';

export function DangerZoneCard({
    consoleAuth,
    primaryOwner,
    displayName,
    deleting,
    onDelete,
}: Readonly<{
    consoleAuth: ConsoleAuthenticationSettings | undefined;
    primaryOwner: boolean;
    displayName: string;
    deleting: boolean;
    onDelete: () => void;
}>) {
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [typed, setTyped] = useState('');
    const inputId = useId();

    useEffect(() => {
        if (!confirmOpen) {
            setTyped('');
        }
    }, [confirmOpen]);

    if (!displayDangerZone(consoleAuth)) {
        return null;
    }

    return (
        <>
            <Card className="border-destructive/40">
                <CardContent className="flex flex-col gap-4 pt-6 sm:flex-row sm:items-center sm:justify-between">
                    <div className="space-y-1">
                        <h2 className="text-lg font-semibold text-destructive">Danger Zone</h2>
                        <p className="text-sm text-muted-foreground">
                            Please transfer ownership of your APIs and applications or delete them before deleting your account
                        </p>
                    </div>
                    <Button type="button" variant="destructive" disabled={primaryOwner || deleting} onClick={() => setConfirmOpen(true)}>
                        Delete my account
                    </Button>
                </CardContent>
            </Card>
            <Dialog open={confirmOpen} onOpenChange={open => !deleting && setConfirmOpen(open)}>
                <DialogContent key={confirmOpen ? `open:${displayName}` : 'closed'} showCloseButton={false}>
                    <DialogHeader>
                        <DialogTitle>Are you sure you want to delete your account?</DialogTitle>
                        <DialogDescription>
                            This operation is irreversible. After removing your account, you will be automatically logged out.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-2 py-2">
                        <Label htmlFor={inputId}>
                            Please type your username <span className="font-mono font-semibold">{displayName}</span> to confirm.
                        </Label>
                        <Input
                            id={inputId}
                            value={typed}
                            autoComplete="off"
                            onChange={event => setTyped(event.target.value)}
                            placeholder={displayName}
                        />
                    </div>
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button type="button" variant="outline" disabled={deleting}>
                                Cancel
                            </Button>
                        </DialogClose>
                        <Button
                            type="button"
                            variant="destructive"
                            disabled={deleting || !displayName.trim() || typed !== displayName}
                            onClick={() => {
                                onDelete();
                            }}
                        >
                            {deleting ? 'Deleting…' : 'Yes, delete my account'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
}
