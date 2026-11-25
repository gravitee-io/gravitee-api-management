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
import { Button, Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle, Input, Label } from '@gravitee/graphene-core';
import { useEffect, useState } from 'react';

import { validateSharedCustomApiKey } from '../../utils/sharedCustomApiKey';

export function ApplicationSharedApiKeyRenewDialog({
    open,
    canUseCustomApiKey,
    pending,
    onClose,
    onConfirm,
}: Readonly<{
    open: boolean;
    canUseCustomApiKey: boolean;
    pending: boolean;
    onClose: () => void;
    onConfirm: (customApiKey?: string) => void;
}>) {
    const [value, setValue] = useState('');

    useEffect(() => {
        if (open) {
            setValue('');
        }
    }, [open]);

    const error = canUseCustomApiKey ? validateSharedCustomApiKey(value) : null;

    const submit = () => {
        if (error) {
            return;
        }
        const trimmed = canUseCustomApiKey ? value.trim() : '';
        onConfirm(trimmed.length > 0 ? trimmed : undefined);
    };

    return (
        <Dialog open={open} onOpenChange={next => !next && !pending && onClose()}>
            <DialogContent aria-describedby={undefined}>
                <DialogHeader>
                    <DialogTitle>Renew API Key</DialogTitle>
                </DialogHeader>
                <p className="text-sm" data-testid="shared-api-key-renew-notice">
                    The previous key stays valid for two hours. You can revoke it from the list.
                </p>
                {canUseCustomApiKey ? (
                    <div className="space-y-2">
                        <Label htmlFor="shared-api-key-renew-value">Custom API Key</Label>
                        <Input
                            id="shared-api-key-renew-value"
                            value={value}
                            onChange={event => setValue(event.target.value)}
                            disabled={pending}
                            autoComplete="off"
                            aria-invalid={error ? true : undefined}
                            aria-describedby={error ? 'shared-api-key-renew-error' : undefined}
                            data-testid="shared-api-key-renew-value"
                        />
                        {error ? (
                            <p
                                id="shared-api-key-renew-error"
                                className="text-xs text-destructive"
                                data-testid="shared-api-key-renew-error"
                            >
                                {error}
                            </p>
                        ) : null}
                    </div>
                ) : null}
                <DialogFooter className="flex-row justify-end gap-2">
                    <Button type="button" variant="outline" onClick={onClose} disabled={pending}>
                        Cancel
                    </Button>
                    <Button type="button" onClick={submit} disabled={pending || Boolean(error)} data-testid="shared-api-key-renew-submit">
                        {pending ? 'Renewing…' : 'Renew'}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
