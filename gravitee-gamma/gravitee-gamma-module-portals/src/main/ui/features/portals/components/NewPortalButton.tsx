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
import { useModuleRouting } from '@gravitee/gamma-modules-sdk/routing';
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
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useId, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';

import { PORTALS_ROUTE_CONFIG } from '../../../config/routes';
import { useCreatePortal } from '../hooks/useCreatePortal';
import type { PortalQuota } from '../types/portal';
import { classifyCreatePortalError, validatePortalName } from '../utils/createPortal';
import { portalKeys } from '../utils/queryKeys';

const LIMIT_MESSAGE = "You've reached your portal limit. Contact support to add more portals.";

export function NewPortalButton({
    quota,
    quotaLoading,
    quotaError,
}: Readonly<{ quota?: PortalQuota; quotaLoading: boolean; quotaError: boolean }>) {
    const environmentId = useEnvironment()?.id ?? '';
    const canCreate = useHasPermission({ anyOf: ['environment-portal-c'] });
    const routing = useModuleRouting(PORTALS_ROUTE_CONFIG);
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const create = useCreatePortal();
    const [open, setOpen] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [name, setName] = useState('');
    const [fieldError, setFieldError] = useState<string>();
    const [error, setError] = useState<string>();
    const [serverLimit, setServerLimit] = useState(false);
    const currentEnvironment = useRef(environmentId);
    const submitting = useRef(false);
    const trigger = useRef<HTMLButtonElement>(null);
    const input = useRef<HTMLInputElement>(null);
    const inputId = useId();
    const errorId = useId();
    const descriptionId = useId();
    currentEnvironment.current = environmentId;
    const atLimit = Boolean(quota && quota.used >= quota.limit);
    const disabled = !environmentId || quotaLoading || quotaError || !quota || atLimit || serverLimit;
    const reason =
        atLimit || serverLimit ? LIMIT_MESSAGE : quotaLoading ? 'Checking portal quota…' : 'Portal quota is unavailable. Please try again.';

    useEffect(() => {
        setOpen(false);
        setName('');
        setFieldError(undefined);
        setError(undefined);
        setServerLimit(false);
    }, [environmentId]);

    useEffect(
        () => () => {
            currentEnvironment.current = '';
        },
        [],
    );

    // A successful quota refresh replaces the limit reported by a previous create attempt.
    useEffect(() => {
        if (quota && quota.used < quota.limit && !quotaLoading && !quotaError) {
            setServerLimit(false);
            setError(previous => (previous === LIMIT_MESSAGE ? undefined : previous));
        }
    }, [quota, quotaLoading, quotaError]);

    if (!canCreate) return null;

    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (submitting.current || disabled) return;
        const validation = validatePortalName(name);
        setFieldError(validation);
        setError(undefined);
        if (validation) return;
        submitting.current = true;
        setIsSubmitting(true);
        const submittedEnvironment = environmentId;
        try {
            const portal = await create.mutateAsync({ environmentId: submittedEnvironment, name });
            const refreshKey = portalKeys.creationRefresh(submittedEnvironment, portal.id);
            queryClient.setQueryData(refreshKey, 'pending');
            void queryClient.invalidateQueries({ queryKey: portalKeys.environment(submittedEnvironment) }, { throwOnError: true }).then(
                () => queryClient.setQueryData(refreshKey, 'success'),
                () => queryClient.setQueryData(refreshKey, 'error'),
            );
            if (currentEnvironment.current !== submittedEnvironment) return;
            setOpen(false);
            const root = routing.pathForKey('dashboard').replace(/\/$/, '');
            navigate(`${root}/${encodeURIComponent(portal.id)}/overview`);
        } catch (failure) {
            if (currentEnvironment.current !== submittedEnvironment) return;
            const kind = classifyCreatePortalError(failure);
            if (kind === 'duplicate') setFieldError('A portal with this name already exists.');
            else if (kind === 'limit') {
                setServerLimit(true);
                setError(LIMIT_MESSAGE);
                void queryClient.invalidateQueries({ queryKey: portalKeys.stats(submittedEnvironment) });
            } else if (kind === 'permission') setError('You do not have permission to create a portal.');
            else if (kind === 'invalid')
                setError(
                    'Unable to create the portal. The environment may need a default portal with a valid URL, or its portals may be managed through the Automation API.',
                );
            else setError('Unable to create the portal. Check the portal list before trying again.');
        } finally {
            submitting.current = false;
            setIsSubmitting(false);
        }
    }

    return (
        <>
            <Button
                ref={trigger}
                type="button"
                aria-disabled={disabled}
                aria-describedby={disabled ? descriptionId : undefined}
                className="aria-disabled:cursor-not-allowed aria-disabled:opacity-50"
                onClick={() => {
                    if (!disabled) setOpen(true);
                }}
            >
                <PlusIcon aria-hidden="true" /> New Portal
            </Button>
            {disabled ? (
                <span id={descriptionId} className="text-sm text-muted-foreground">
                    {reason}
                </span>
            ) : null}
            <Dialog
                open={open}
                onOpenChange={value => {
                    if (!submitting.current) {
                        setOpen(value);
                        if (!value) {
                            setName('');
                            setFieldError(undefined);
                            setError(undefined);
                        }
                    }
                }}
            >
                <DialogContent
                    showCloseButton={false}
                    onOpenAutoFocus={event => {
                        event.preventDefault();
                        input.current?.focus();
                    }}
                    onCloseAutoFocus={event => {
                        event.preventDefault();
                        trigger.current?.focus();
                    }}
                >
                    <DialogHeader>
                        <DialogTitle>New Portal</DialogTitle>
                        <DialogDescription>Enter a name for your portal.</DialogDescription>
                    </DialogHeader>
                    <form onSubmit={event => void submit(event)} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor={inputId}>Name</Label>
                            <Input
                                id={inputId}
                                ref={input}
                                value={name}
                                disabled={isSubmitting}
                                aria-invalid={Boolean(fieldError)}
                                aria-describedby={fieldError ? errorId : undefined}
                                onChange={event => {
                                    setName(event.target.value);
                                    setFieldError(undefined);
                                }}
                            />
                            {fieldError ? (
                                <p id={errorId} role="alert">
                                    {fieldError}
                                </p>
                            ) : null}
                        </div>
                        {error || atLimit ? <p role="alert">{error ?? LIMIT_MESSAGE}</p> : null}
                        {!error && !atLimit && disabled ? <p role={quotaLoading ? 'status' : 'alert'}>{reason}</p> : null}
                        <DialogFooter>
                            <Button
                                type="button"
                                variant="outline"
                                disabled={isSubmitting}
                                onClick={() => {
                                    setOpen(false);
                                    setName('');
                                    setFieldError(undefined);
                                    setError(undefined);
                                }}
                            >
                                Cancel
                            </Button>
                            <Button type="submit" disabled={isSubmitting || disabled}>
                                {isSubmitting ? 'Creating…' : 'Create portal'}
                            </Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        </>
    );
}
