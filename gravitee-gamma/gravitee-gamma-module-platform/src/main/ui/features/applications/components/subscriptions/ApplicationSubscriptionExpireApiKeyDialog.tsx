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
import { useEffect, useMemo, useState } from 'react';

import type { ApplicationSubscriptionApiKeyRow } from '../../types/applicationSubscription';
import {
    canSubmitApiKeyExpirationChange,
    isAfterMinCandidate,
    parseDatetimeLocalValue,
} from '../../utils/applicationSubscriptionApiKeyExpireUtils';

function toDatetimeLocalValue(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function defaultExpirationDraft(apiKey: ApplicationSubscriptionApiKeyRow): Date {
    if (apiKey.expireAt !== undefined) {
        return new Date(apiKey.expireAt);
    }
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setMinutes(0, 0, 0);
    return d;
}

export function ApplicationSubscriptionExpireApiKeyDialog({
    apiKey,
    onClose,
    onConfirm,
    isLoading,
    error,
}: Readonly<{
    apiKey: ApplicationSubscriptionApiKeyRow | null;
    onClose: () => void;
    onConfirm: (expirationDate: Date) => void;
    isLoading: boolean;
    error?: string | null;
}>) {
    const initialLocal = useMemo(() => (apiKey ? toDatetimeLocalValue(defaultExpirationDraft(apiKey)) : ''), [apiKey]);
    const [value, setValue] = useState(initialLocal);
    const [dirty, setDirty] = useState(false);

    useEffect(() => {
        setValue(initialLocal);
        setDirty(false);
    }, [initialLocal]);

    const minMs = Date.now() - 60_000;
    const parsedExpirationDate = useMemo(() => parseDatetimeLocalValue(value), [value]);

    const hasInvalidFormat = dirty && !parsedExpirationDate;
    const hasInvalidRange = dirty && Boolean(parsedExpirationDate) && !isAfterMinCandidate(value, minMs);
    const canSubmit = canSubmitApiKeyExpirationChange(dirty, value, minMs);

    return (
        <Dialog open={Boolean(apiKey)} onOpenChange={open => !open && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>Change your API Key&apos;s expiration date</DialogTitle>
                    <DialogDescription>
                        Set a new expiration for key <span className="font-mono text-foreground">{apiKey?.maskedKey}</span>.
                    </DialogDescription>
                </DialogHeader>
                {apiKey ? (
                    <div className="space-y-2">
                        <Label htmlFor="api-key-expire-at">Expire date</Label>
                        <Input
                            id="api-key-expire-at"
                            type="datetime-local"
                            value={value}
                            min={toDatetimeLocalValue(new Date())}
                            onChange={e => {
                                setValue(e.target.value);
                                setDirty(true);
                            }}
                        />
                        {hasInvalidFormat ? <p className="text-sm text-destructive">Enter a valid date and time.</p> : null}
                        {hasInvalidRange ? <p className="text-sm text-destructive">Date and time must be in the future.</p> : null}
                    </div>
                ) : null}
                {error ? <p className="text-sm text-destructive">{error}</p> : null}
                <DialogFooter>
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={isLoading}>
                            Cancel
                        </Button>
                    </DialogClose>
                    <Button
                        type="button"
                        disabled={isLoading || !canSubmit}
                        onClick={() => apiKey && canSubmit && parsedExpirationDate && onConfirm(parsedExpirationDate)}
                    >
                        {isLoading ? 'Saving…' : 'Change expiration date'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
