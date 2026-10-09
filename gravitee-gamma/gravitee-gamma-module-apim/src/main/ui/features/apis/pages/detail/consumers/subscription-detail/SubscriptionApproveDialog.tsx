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
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, Input, Label, Textarea } from '@gravitee/graphene-core';
import { CircleCheckIcon } from '@gravitee/graphene-core/icons';
import { useCallback, useState } from 'react';

import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import type { ApproveSubscriptionPayload } from '../../../../types/subscription';

interface SubscriptionApproveDialogProps {
    open: boolean;
    isApiKeyPlan: boolean;
    isSharedApiKeyMode?: boolean;
    isFederated?: boolean;
    isPending: boolean;
    onConfirm: (payload: ApproveSubscriptionPayload) => void;
    onClose: () => void;
}

interface FormState {
    startingAt: string;
    endingAt: string;
    customApiKey: string;
    reason: string;
}

const EMPTY_FORM: FormState = { startingAt: '', endingAt: '', customApiKey: '', reason: '' };

export function SubscriptionApproveDialog({
    open,
    isApiKeyPlan,
    isSharedApiKeyMode = false,
    isFederated = false,
    isPending,
    onConfirm,
    onClose,
}: Readonly<SubscriptionApproveDialogProps>) {
    const [form, setForm] = useState<FormState>(EMPTY_FORM);
    const customApiKeyEnabled = useCustomApiKeyEnabled();
    const showDates = !isFederated;
    const showCustomApiKey = !isFederated && isApiKeyPlan && customApiKeyEnabled && !isSharedApiKeyMode;

    const set = useCallback(<K extends keyof FormState>(key: K, value: FormState[K]) => {
        setForm(prev => ({ ...prev, [key]: value }));
    }, []);

    const handleClose = useCallback(() => {
        setForm(EMPTY_FORM);
        onClose();
    }, [onClose]);

    const handleConfirm = useCallback(() => {
        const payload: ApproveSubscriptionPayload = {};
        if (form.reason.trim()) payload.reason = form.reason.trim();
        if (showDates && form.startingAt) payload.startingAt = new Date(form.startingAt).toISOString();
        if (showDates && form.endingAt) payload.endingAt = new Date(form.endingAt).toISOString();
        if (showCustomApiKey && form.customApiKey.trim()) payload.customApiKey = form.customApiKey.trim();
        onConfirm(payload);
    }, [form, showDates, showCustomApiKey, onConfirm]);

    return (
        <Dialog open={open} onOpenChange={open ? handleClose : undefined}>
            <DialogContent style={{ maxWidth: '440px', width: '90vw' }}>
                <DialogHeader>
                    <DialogTitle>Approve Subscription</DialogTitle>
                    <p className="text-sm text-muted-foreground">All fields are optional. Leave blank to use defaults.</p>
                </DialogHeader>

                <div className="space-y-4 py-2">
                    {showDates && (
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1.5">
                                <Label htmlFor="approve-start">Starting date</Label>
                                <Input
                                    id="approve-start"
                                    type="date"
                                    value={form.startingAt}
                                    onChange={e => set('startingAt', e.target.value)}
                                />
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="approve-end">Ending date</Label>
                                <Input
                                    id="approve-end"
                                    type="date"
                                    value={form.endingAt}
                                    min={form.startingAt || undefined}
                                    onChange={e => set('endingAt', e.target.value)}
                                />
                            </div>
                        </div>
                    )}

                    {showCustomApiKey && (
                        <div className="space-y-1.5">
                            <Label htmlFor="approve-key">Custom API key</Label>
                            <Input
                                id="approve-key"
                                value={form.customApiKey}
                                onChange={e => set('customApiKey', e.target.value)}
                                placeholder="Leave blank to auto-generate"
                                autoComplete="off"
                            />
                            <p className="text-xs text-muted-foreground">Must be 8–64 alphanumeric characters if provided.</p>
                        </div>
                    )}

                    <div className="space-y-1.5">
                        <Label htmlFor="approve-reason">Message to subscriber</Label>
                        <Textarea
                            id="approve-reason"
                            value={form.reason}
                            onChange={e => set('reason', e.target.value)}
                            placeholder="Optional message visible to the subscriber…"
                            rows={3}
                        />
                    </div>
                </div>

                <DialogFooter>
                    <Button type="button" variant="outline" onClick={handleClose} disabled={isPending}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={handleConfirm} disabled={isPending}>
                        <CircleCheckIcon className="size-4" aria-hidden />
                        {isPending ? 'Approving…' : 'Approve'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
