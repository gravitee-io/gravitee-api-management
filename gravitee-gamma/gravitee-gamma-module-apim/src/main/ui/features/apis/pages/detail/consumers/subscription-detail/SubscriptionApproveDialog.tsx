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
import { useCallback, useRef, useState } from 'react';

import { useCustomApiKeyCheck } from '../../../../hooks/useCustomApiKeyCheck';
import { useCustomApiKeyEnabled } from '../../../../hooks/usePlanSecuritySettings';
import type { ApproveSubscriptionPayload, SubscriptionContext } from '../../../../types/subscription';
import { CustomApiKeyField } from '../CustomApiKeyField';
import {
    currentDatetimeLocalMinute,
    datetimeLocalToIso,
    endDatetimeLocalMin,
    isBeforeCurrentMinute,
    isEndOnOrBeforeStart,
} from './subscriptionDateTime';

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
    const [startIncomplete, setStartIncomplete] = useState(false);
    const [endIncomplete, setEndIncomplete] = useState(false);
    const [checkedAt, setCheckedAt] = useState<Date | null>(null);
    const startInputRef = useRef<HTMLInputElement>(null);
    const endInputRef = useRef<HTMLInputElement>(null);
    const customApiKeyEnabled = useCustomApiKeyEnabled();
    const showDates = !isFederated;
    const showCustomApiKey = !isFederated && isApiKeyPlan && customApiKeyEnabled && !isSharedApiKeyMode;
    const customKeyCheck = useCustomApiKeyCheck(ctx, applicationId, showCustomApiKey ? form.customApiKey : '');

    const set = useCallback(<K extends keyof FormState>(key: K, value: FormState[K]) => {
        setForm(prev => ({ ...prev, [key]: value }));
    }, []);

    const handleClose = useCallback(() => {
        setForm(EMPTY_FORM);
        setStartIncomplete(false);
        setEndIncomplete(false);
        setCheckedAt(null);
        onClose();
    }, [onClose]);

    const now = checkedAt ?? undefined;
    const startInPast = showDates && isBeforeCurrentMinute(form.startingAt, now);
    const endInPast = showDates && isBeforeCurrentMinute(form.endingAt, now);
    const endNotAfterStart = showDates && isEndOnOrBeforeStart(form.startingAt, form.endingAt);
    const datesIncomplete = showDates && (startIncomplete || endIncomplete);
    const datesBlocked = datesIncomplete || startInPast || endInPast || endNotAfterStart;

    const handleConfirm = useCallback(() => {
        const checkedNow = new Date();
        const startBadInput = Boolean(startInputRef.current?.validity.badInput);
        const endBadInput = Boolean(endInputRef.current?.validity.badInput);
        if (startBadInput) setStartIncomplete(true);
        if (endBadInput) setEndIncomplete(true);
        const startPast = showDates && isBeforeCurrentMinute(form.startingAt, checkedNow);
        const endPast = showDates && isBeforeCurrentMinute(form.endingAt, checkedNow);
        const endOrder = showDates && isEndOnOrBeforeStart(form.startingAt, form.endingAt);
        if (customKeyCheck.blocked || datesIncomplete || startBadInput || endBadInput || startPast || endPast || endOrder) {
            if (startPast || endPast || endOrder) setCheckedAt(checkedNow);
            return;
        }
        const payload: ApproveSubscriptionPayload = {};
        if (form.reason.trim()) payload.reason = form.reason.trim();
        if (showDates && form.startingAt) payload.startingAt = datetimeLocalToIso(form.startingAt);
        if (showDates && form.endingAt) payload.endingAt = datetimeLocalToIso(form.endingAt);
        if (showCustomApiKey && form.customApiKey !== '') payload.customApiKey = form.customApiKey;
        onConfirm(payload);
    }, [customKeyCheck.blocked, datesIncomplete, form, showDates, showCustomApiKey, onConfirm]);

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
                                    ref={startInputRef}
                                    id="approve-start"
                                    type="datetime-local"
                                    value={form.startingAt}
                                    min={currentDatetimeLocalMinute(now)}
                                    onChange={e => {
                                        set('startingAt', e.target.value);
                                        setStartIncomplete(e.target.validity.badInput);
                                        setCheckedAt(null);
                                    }}
                                    onBlur={e => setStartIncomplete(e.currentTarget.validity.badInput)}
                                />
                                {startIncomplete ? (
                                    <p className="text-sm text-destructive">Enter both a date and a time.</p>
                                ) : startInPast ? (
                                    <p className="text-sm text-destructive">Date and time must not be earlier than the current minute.</p>
                                ) : null}
                            </div>
                            <div className="space-y-1.5">
                                <Label htmlFor="approve-end">Ending date</Label>
                                <Input
                                    ref={endInputRef}
                                    id="approve-end"
                                    type="datetime-local"
                                    value={form.endingAt}
                                    min={endDatetimeLocalMin(form.startingAt, now)}
                                    onChange={e => {
                                        set('endingAt', e.target.value);
                                        setEndIncomplete(e.target.validity.badInput);
                                        setCheckedAt(null);
                                    }}
                                    onBlur={e => setEndIncomplete(e.currentTarget.validity.badInput)}
                                />
                                {endIncomplete ? (
                                    <p className="text-sm text-destructive">Enter both a date and a time.</p>
                                ) : endInPast ? (
                                    <p className="text-sm text-destructive">Date and time must not be earlier than the current minute.</p>
                                ) : null}
                                {endNotAfterStart && !endInPast ? (
                                    <p className="text-sm text-destructive">End date must be after the start date.</p>
                                ) : null}
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
                    <Button type="button" onClick={handleConfirm} disabled={isPending || customKeyCheck.blocked || datesBlocked}>
                        <CircleCheckIcon className="size-4" aria-hidden />
                        {isPending ? 'Approving…' : 'Approve'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
