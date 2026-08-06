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
    Alert,
    AlertDescription,
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
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useMemo, useState } from 'react';

import { copyTextToClipboardWithNotifyHandler } from '../../../shared/copyToClipboard';
import { notify } from '../../../shared/notify';
import { useCreateOrganizationUserToken } from '../hooks/useUserMutations';
import type { OrganizationUserToken } from '../types/user';
import { buildTokenUsageExample, isDuplicateTokenError, TOKEN_NAME_MAX_LENGTH, validateTokenName } from '../utils/userTokenDisplay';

interface GenerateUserTokenDialogProps {
    readonly open: boolean;
    readonly userId: string;
    readonly environmentId: string;
    readonly onOpenChange: (open: boolean) => void;
}

function TokenCopyField({ label, value }: Readonly<{ label: string; value: string }>) {
    return (
        <div className="min-w-0 space-y-2">
            <p className="text-sm font-medium text-foreground">{label}</p>
            <div className="flex min-w-0 items-start gap-2 rounded-md border bg-muted/40 p-3">
                <code className="min-w-0 flex-1 break-all [overflow-wrap:anywhere] font-mono text-sm text-foreground">{value}</code>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-8 shrink-0"
                    aria-label={`Copy ${label.toLowerCase()}`}
                    onClick={() => copyTextToClipboardWithNotifyHandler(value, 'Copied to clipboard')}
                >
                    <CopyIcon className="size-4" aria-hidden />
                </Button>
            </div>
        </div>
    );
}

export function GenerateUserTokenDialog({ open, userId, environmentId, onOpenChange }: GenerateUserTokenDialogProps) {
    const nameInputId = useId();
    const [name, setName] = useState('');
    const [nameError, setNameError] = useState<string | null>(null);
    const [generatedToken, setGeneratedToken] = useState<OrganizationUserToken | null>(null);
    const [usageExample, setUsageExample] = useState('');
    const createToken = useCreateOrganizationUserToken(userId);

    useEffect(() => {
        if (!open) {
            setName('');
            setNameError(null);
            setGeneratedToken(null);
            setUsageExample('');
        }
    }, [open]);

    useEffect(() => {
        if (!generatedToken?.token) {
            setUsageExample('');
            return;
        }
        let cancelled = false;
        void buildTokenUsageExample(generatedToken.token, environmentId).then(example => {
            if (!cancelled) {
                setUsageExample(example);
            }
        });
        return () => {
            cancelled = true;
        };
    }, [environmentId, generatedToken]);

    const trimmedName = name.trim();
    const nameLengthHint = `${trimmedName.length}/${TOKEN_NAME_MAX_LENGTH}`;
    const canGenerate = useMemo(() => validateTokenName(name) === null, [name]);

    function handleOpenChange(nextOpen: boolean) {
        if (!nextOpen && createToken.isPending) {
            return;
        }
        onOpenChange(nextOpen);
    }

    function handleGenerate() {
        const validationError = validateTokenName(name);
        if (validationError) {
            setNameError(validationError);
            return;
        }

        setNameError(null);
        createToken.mutate(
            { name: trimmedName },
            {
                onSuccess: token => {
                    notify.success('Token successfully created!');
                    setGeneratedToken(token);
                },
                onError: error => {
                    const message = error instanceof Error ? error.message : '';
                    if (isDuplicateTokenError(message)) {
                        setNameError(message);
                        return;
                    }
                    notify.error(error, 'Failed to generate token.');
                },
            },
        );
    }

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="w-full max-w-xl overflow-hidden" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle>Generate a token</DialogTitle>
                    {!generatedToken ? <DialogDescription>What&apos;s this token for?</DialogDescription> : null}
                </DialogHeader>

                {!generatedToken ? (
                    <div className="space-y-2 py-2">
                        <Label htmlFor={nameInputId}>Name *</Label>
                        <Input
                            id={nameInputId}
                            value={name}
                            maxLength={TOKEN_NAME_MAX_LENGTH}
                            autoComplete="off"
                            aria-invalid={Boolean(nameError)}
                            aria-describedby={nameError ? `${nameInputId}-error` : `${nameInputId}-hint`}
                            onChange={event => {
                                setName(event.target.value);
                                if (nameError) {
                                    setNameError(null);
                                }
                            }}
                        />
                        <div className="flex items-start justify-between gap-3 text-xs text-muted-foreground">
                            <span id={`${nameInputId}-hint`}>What&apos;s this token for?</span>
                            <span>{nameLengthHint}</span>
                        </div>
                        {nameError ? (
                            <p id={`${nameInputId}-error`} className="text-sm text-destructive">
                                {nameError}
                            </p>
                        ) : null}
                    </div>
                ) : (
                    <div className="min-w-0 space-y-4 py-2">
                        <Alert>
                            <AlertDescription>
                                Make sure to copy your new personal access token now. You won&apos;t be able to see it again.
                            </AlertDescription>
                        </Alert>
                        {generatedToken.token ? <TokenCopyField label="Token" value={generatedToken.token} /> : null}
                        {usageExample ? <TokenCopyField label="Usage" value={usageExample} /> : null}
                    </div>
                )}

                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={createToken.isPending}>
                            {generatedToken ? 'Close' : 'Cancel'}
                        </Button>
                    </DialogClose>
                    {!generatedToken ? (
                        <Button type="button" disabled={!canGenerate || createToken.isPending} onClick={handleGenerate}>
                            {createToken.isPending ? 'Generating…' : 'Generate'}
                        </Button>
                    ) : null}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
