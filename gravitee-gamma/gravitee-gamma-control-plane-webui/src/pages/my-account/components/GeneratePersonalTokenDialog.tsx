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
    toast,
} from '@gravitee/graphene-core';
import { CopyIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useMemo, useState } from 'react';

import { ApiError } from '../../../shared/api/api-client';
import { useBootstrapStore } from '../../../shared/config/bootstrap.store';
import { buildTokenUsageExample, isDuplicateTokenError, TOKEN_NAME_MAX_LENGTH, validateTokenName } from '../myAccount.mapping';
import type { PersonalAccessToken } from '../myAccount.types';
import { createCurrentUserToken } from '../services/currentUserTokens';

export function GeneratePersonalTokenDialog({
    open,
    environmentId,
    onOpenChange,
    onGenerated,
}: Readonly<{
    open: boolean;
    environmentId: string;
    onOpenChange: (open: boolean) => void;
    onGenerated: () => void;
}>) {
    const nameInputId = useId();
    const [name, setName] = useState('');
    const [nameError, setNameError] = useState<string | null>(null);
    const [generated, setGenerated] = useState<PersonalAccessToken | null>(null);
    const [pending, setPending] = useState(false);
    const config = useBootstrapStore(s => s.config);

    useEffect(() => {
        if (!open) {
            setName('');
            setNameError(null);
            setGenerated(null);
            setPending(false);
        }
    }, [open]);

    const usageExample = useMemo(() => {
        if (!generated?.token || !config) {
            return '';
        }
        return buildTokenUsageExample(generated.token, config.managementBaseURL, config.organizationId, environmentId);
    }, [config, environmentId, generated]);

    const canGenerate = validateTokenName(name) === null;

    function handleOpenChange(nextOpen: boolean) {
        if (!nextOpen && pending) {
            return;
        }
        if (!nextOpen && generated) {
            onGenerated();
        }
        onOpenChange(nextOpen);
    }

    async function handleGenerate() {
        const validationError = validateTokenName(name);
        if (validationError) {
            setNameError(validationError);
            return;
        }
        setNameError(null);
        setPending(true);
        try {
            const token = await createCurrentUserToken(name.trim());
            toast.success(`Token "${name.trim()}" has been successfully generated.`);
            setGenerated(token);
        } catch (error) {
            const message = error instanceof Error ? error.message : '';
            const technicalCode = error instanceof ApiError ? error.technicalCode : undefined;
            if (isDuplicateTokenError(technicalCode, message)) {
                setNameError(message || 'A token with this name already exists.');
            } else {
                toast.error(message || 'Failed to generate token.');
            }
        } finally {
            setPending(false);
        }
    }

    return (
        <Dialog open={open} onOpenChange={handleOpenChange}>
            <DialogContent className="w-full max-w-xl overflow-hidden" showCloseButton={false}>
                <DialogHeader>
                    <DialogTitle>Generate a token</DialogTitle>
                    {!generated ? <DialogDescription>What&apos;s this token for?</DialogDescription> : null}
                </DialogHeader>
                {!generated ? (
                    <div className="space-y-2 py-2">
                        <Label htmlFor={nameInputId}>Name *</Label>
                        <Input
                            id={nameInputId}
                            value={name}
                            maxLength={TOKEN_NAME_MAX_LENGTH}
                            autoComplete="off"
                            aria-invalid={Boolean(nameError)}
                            onChange={event => {
                                setName(event.target.value);
                                if (nameError) {
                                    setNameError(null);
                                }
                            }}
                        />
                        <p className="text-xs text-muted-foreground">
                            {name.trim().length}/{TOKEN_NAME_MAX_LENGTH}
                        </p>
                        {nameError ? <p className="text-sm text-destructive">{nameError}</p> : null}
                    </div>
                ) : (
                    <div className="min-w-0 space-y-4 py-2">
                        <Alert>
                            <AlertDescription>
                                Make sure to copy your new personal access token now. You won&apos;t be able to see it again.
                            </AlertDescription>
                        </Alert>
                        {generated.token ? <CopyField label="Token" value={generated.token} /> : null}
                        {usageExample ? <CopyField label="Usage" value={usageExample} /> : null}
                    </div>
                )}
                <DialogFooter className="sm:justify-end">
                    <DialogClose asChild>
                        <Button type="button" variant="outline" disabled={pending}>
                            {generated ? 'Close' : 'Cancel'}
                        </Button>
                    </DialogClose>
                    {!generated ? (
                        <Button type="button" disabled={!canGenerate || pending} onClick={() => void handleGenerate()}>
                            {pending ? 'Generating…' : 'Generate'}
                        </Button>
                    ) : null}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

function CopyField({ label, value }: Readonly<{ label: string; value: string }>) {
    return (
        <div className="min-w-0 space-y-2">
            <p className="text-sm font-medium">{label}</p>
            <div className="flex min-w-0 items-start gap-2 rounded-md border bg-muted/40 p-3">
                <code className="min-w-0 flex-1 break-all font-mono text-sm">{value}</code>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-8 shrink-0"
                    aria-label={`Copy ${label.toLowerCase()}`}
                    onClick={() => {
                        void navigator.clipboard.writeText(value).then(
                            () => toast.info('Copied to clipboard'),
                            () => toast.error('Unable to copy to clipboard'),
                        );
                    }}
                >
                    <CopyIcon className="size-4" aria-hidden />
                </Button>
            </div>
        </div>
    );
}
