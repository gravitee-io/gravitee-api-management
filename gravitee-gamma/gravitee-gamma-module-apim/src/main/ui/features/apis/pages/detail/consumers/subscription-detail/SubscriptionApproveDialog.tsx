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

import { useCustomApiKeyCheck } from '../../../../hooks/useCustomApiKeyCheck';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import type { ApproveSubscriptionPayload, SubscriptionContext } from '../../../../types/subscription';
import { dateInputToOffsetDateTime } from '../../../../utils/dateInputToOffsetDateTime';
import { CustomApiKeyField } from '../CustomApiKeyField';

interface SubscriptionApproveDialogProps {
    open: boolean;
    ctx: SubscriptionContext;
    applicationId: string;
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
    ctx,
    applicationId,
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
    const customKeyCheck = useCustomApiKeyCheck(ctx, applicationId, showCustomApiKey ? form.customApiKey : '');

    const set = useCallback(<K extends keyof FormState>(key: K, value: FormState[K]) => {
        setForm(prev => ({ ...prev, [key]: value }));
    }, []);

    const handleClose = useCallback(() => {
        setForm(EMPTY_FORM);
        onClose();
    }, [onClose]);

    const handleConfirm = useCallback(() => {
        if (customKeyCheck.blocked) return;
        const payload: ApproveSubscriptionPayload = {};
        if (form.reason.trim()) payload.reason = form.reason.trim();
        if (showDates && form.startingAt) payload.startingAt = dateInputToOffsetDateTime(form.startingAt);
        if (showDates && form.endingAt) payload.endingAt = dateInputToOffsetDateTime(form.endingAt);
        if (showCustomApiKey && form.customApiKey !== '') payload.customApiKey = form.customApiKey;
        onConfirm(payload);
    }, [customKeyCheck.blocked, form, showDates, showCustomApiKey, onConfirm]);

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
                        <CustomApiKeyField
                            id="approve-key"
                            label="Custom API key"
                            value={form.customApiKey}
                            onChange={value => set('customApiKey', value)}
                            error={customKeyCheck.error}
                            checking={customKeyCheck.checking}
                            disabled={isPending}
                            placeholder="Leave blank to auto-generate"
                        />
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
                    <Button type="button" onClick={handleConfirm} disabled={isPending || customKeyCheck.blocked}>
                        <CircleCheckIcon className="size-4" aria-hidden />
                        {isPending ? 'Approving…' : 'Approve'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
