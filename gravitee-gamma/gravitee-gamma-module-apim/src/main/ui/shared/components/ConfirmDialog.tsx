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
import { useEffect, useId, useState, type ReactNode } from 'react';

export interface ConfirmDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    title: ReactNode;
    description?: ReactNode;
    confirmLabel: string;
    /** Shown on the confirm button while the action is running (e.g. "Deleting…"). Defaults to `${confirmLabel}…`. */
    pendingLabel?: string;
    cancelLabel?: string;
    destructive?: boolean;
    isPending?: boolean;
    /**
     * When provided, the user must type this exact value to enable the confirm button (type-to-confirm).
     * Use for irreversible actions (e.g. the API or product name).
     */
    confirmKeyword?: string;
    /** Optional leading icon for the confirm button (e.g. a trash icon for destructive actions). */
    icon?: ReactNode;
    onConfirm: () => void;
}

/**
 * Reusable confirmation dialog for APIM pages.
 *
 * Errors are intentionally NOT rendered here: API failures from confirm/action dialogs are
 * surfaced via `notify.error(...)` (toast-only), keeping this component purely presentational.
 */
export function ConfirmDialog({
    open,
    onOpenChange,
    title,
    description,
    confirmLabel,
    pendingLabel,
    cancelLabel = 'Cancel',
    destructive = false,
    isPending = false,
    confirmKeyword,
    icon,
    onConfirm,
}: Readonly<ConfirmDialogProps>) {
    const inputId = useId();
    const [typed, setTyped] = useState('');

    useEffect(() => {
        if (!open) setTyped('');
    }, [open]);

    const keywordSatisfied = !confirmKeyword || typed === confirmKeyword;
    const disabled = isPending || !keywordSatisfied;

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    {description ? <DialogDescription>{description}</DialogDescription> : null}
                </DialogHeader>
                {confirmKeyword ? (
                    <div className="space-y-2 py-2">
                        <Label htmlFor={inputId} className="text-sm">
                            Type <span className="font-mono font-semibold">{confirmKeyword}</span> to confirm
                        </Label>
                        <Input
                            id={inputId}
                            value={typed}
                            onChange={e => setTyped(e.target.value)}
                            placeholder={confirmKeyword}
                            autoComplete="off"
                        />
                    </div>
                ) : null}
                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isPending}>
                            {cancelLabel}
                        </Button>
                    </DialogClose>
                    <Button type="button" variant={destructive ? 'destructive' : 'default'} disabled={disabled} onClick={onConfirm}>
                        {icon}
                        {isPending ? (pendingLabel ?? `${confirmLabel}…`) : confirmLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
