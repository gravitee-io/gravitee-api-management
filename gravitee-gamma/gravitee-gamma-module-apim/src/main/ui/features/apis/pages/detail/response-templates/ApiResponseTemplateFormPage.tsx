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

import { useEnvironment, useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Button,
    Card,
    CardContent,
    Command,
    CommandEmpty,
    CommandGroup,
    CommandInput,
    CommandItem,
    CommandList,
    Input,
    Label,
    Popover,
    PopoverContent,
    PopoverTrigger,
    ScrollArea,
    Skeleton,
    Switch,
    Textarea,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, ChevronsUpDownIcon, PlusIcon, Trash2Icon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useId, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { FeatureUnavailableNotice } from './FeatureUnavailableNotice';
import { KubernetesManagedReadOnlyAlert } from './KubernetesManagedReadOnlyAlert';
import { TcpProxyUnavailableNotice } from './TcpProxyUnavailableNotice';
import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { updateApiResponseTemplates } from '../../../services/apis';
import type { ResponseTemplateRow } from '../../../types/responseTemplate';
import { hasTcpListeners, supportsResponseTemplates } from '../../../utils/apiHttpProxy';
import { findHttpStatusCode, HTTP_STATUS_CODES, isValidHttpStatusCode } from '../../../utils/httpStatusCodes';
import { apiDetailKeys } from '../../../utils/queryKeys';
import { RESPONSE_TEMPLATE_KEYS } from '../../../utils/responseTemplateKeys';
import {
    isDuplicateKeyAccept,
    parseResponseTemplatePath,
    toResponseTemplatePath,
    toResponseTemplates,
    upsertResponseTemplate,
} from '../../../utils/responseTemplates';
import { headersFromRecord, headersToRecord, newHeaderRow, type HeaderEntry } from '../endpoints/types';

export function ApiResponseTemplateFormPage() {
    const { templateKey, contentType } = useParams<{ templateKey?: string; contentType?: string }>();
    const formKey = templateKey !== undefined && contentType !== undefined ? `${templateKey}/${contentType}` : 'new';
    return <ApiResponseTemplateForm key={formKey} />;
}

function ApiResponseTemplateForm() {
    const {
        apiId,
        templateKey,
        contentType: contentTypeParam,
    } = useParams<{
        apiId: string;
        templateKey?: string;
        contentType?: string;
    }>();
    const routeIdentity = parseResponseTemplatePath(templateKey, contentTypeParam);
    const env = useEnvironment();
    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const canEdit = useHasPermission({ anyOf: ['api-response_templates-u'] });
    const canRead = useHasPermission({ anyOf: ['api-response_templates-r'] });

    const { data: api, isLoading, isError } = useApiDetail(apiId);
    const { permissionsReady } = useApiDetailContext();

    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const templates = useMemo(() => toResponseTemplates(api?.responseTemplates), [api?.responseTemplates]);
    const editing = useMemo(
        () =>
            routeIdentity ? templates.find(rt => rt.key === routeIdentity.key && rt.contentType === routeIdentity.contentType) : undefined,
        [routeIdentity, templates],
    );
    const mode: 'new' | 'edit' = routeIdentity ? 'edit' : 'new';
    const readOnly = isKubernetesManaged || !canEdit;

    const [key, setKey] = useState('');
    const [acceptHeader, setAcceptHeader] = useState('*/*');
    const [statusCode, setStatusCode] = useState('400');
    const [headers, setHeaders] = useState<HeaderEntry[]>(() => [newHeaderRow()]);
    const [body, setBody] = useState('');
    const [propagateErrorKeyToLogs, setPropagateErrorKeyToLogs] = useState(false);
    const [keyOpen, setKeyOpen] = useState(false);
    const [statusOpen, setStatusOpen] = useState(false);
    const [hydrated, setHydrated] = useState(false);
    const [attemptedSubmit, setAttemptedSubmit] = useState(false);

    const keyId = useId();
    const acceptId = useId();
    const statusId = useId();
    const bodyId = useId();
    const propagateId = useId();

    useEffect(() => {
        if (!api) {
            if (isError) setHydrated(true);
            return;
        }
        if (mode === 'edit' && editing) {
            setKey(editing.key);
            setAcceptHeader(editing.contentType);
            setStatusCode(String(editing.statusCode ?? 400));
            const existingHeaders = headersFromRecord(editing.headers);
            setHeaders(existingHeaders.length > 0 ? existingHeaders : [newHeaderRow()]);
            setBody(editing.body ?? '');
            setPropagateErrorKeyToLogs(editing.propagateErrorKeyToLogs ?? false);
        }
        setHydrated(true);
    }, [api, editing, isError, mode]);

    const selectedStatus = findHttpStatusCode(statusCode);
    const editingIdentity = editing ? { key: editing.key, contentType: editing.contentType } : undefined;

    const keyError = !key.trim() ? 'Template key is required.' : undefined;
    let acceptError: string | undefined;
    if (!acceptHeader.trim()) {
        acceptError = 'Accept header is required.';
    } else if (isDuplicateKeyAccept(templates, key.trim(), acceptHeader.trim(), editingIdentity)) {
        acceptError = `Response template with key '${key.trim()}' and accept header '${acceptHeader.trim()}' already exists.`;
    }
    let statusError: string | undefined;
    if (!statusCode.trim()) {
        statusError = 'Status code is required.';
    } else if (!isValidHttpStatusCode(statusCode)) {
        statusError = `Invalid status code: ${statusCode}.`;
    }

    const canSubmit = !readOnly && !keyError && !acceptError && !statusError;

    const mutation = useMutation({
        mutationFn: (toSave: ResponseTemplateRow) =>
            updateApiResponseTemplates(env!.id, apiId!, current => upsertResponseTemplate(current, toSave, editingIdentity)),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            notify.success('Configuration successfully saved!');
            navigate('..');
        },
        onError: (error: unknown) => {
            if (error instanceof ApimApiError && error.status === 412) {
                queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            }
            notify.error(error, 'Failed to save response template');
        },
    });

    function handleCancel() {
        navigate('..');
    }

    function handleSubmit() {
        setAttemptedSubmit(true);
        if (!canSubmit || !env || !apiId) return;

        const nextKey = key.trim();
        const nextContentType = acceptHeader.trim();
        mutation.mutate({
            id: toResponseTemplatePath(nextKey, nextContentType),
            key: nextKey,
            contentType: nextContentType,
            statusCode: Number.parseInt(statusCode, 10),
            headers: headersToRecord(headers),
            body,
            propagateErrorKeyToLogs,
        });
    }

    if (!env || !apiId) return null;

    if (!permissionsReady) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-64 rounded" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    if (hasTcpListeners(api)) {
        return <TcpProxyUnavailableNotice feature="Response Templates" />;
    }

    if (!supportsResponseTemplates(api)) {
        return (
            <FeatureUnavailableNotice
                heading="Response Templates are not available for MCP and LLM Proxy APIs"
                detail="MCP and LLM Proxy APIs do not support HTTP response template overrides."
            />
        );
    }

    if (!canRead) {
        return (
            <div className="space-y-6">
                <p className="text-sm text-muted-foreground">You don&apos;t have permission to view response templates.</p>
            </div>
        );
    }

    if (isLoading || !hydrated) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-64 rounded" />
                <Skeleton className="h-64 w-full rounded-lg" />
            </div>
        );
    }

    if (isError) {
        return (
            <Alert variant="destructive">
                <AlertDescription>Failed to load response template. Please refresh the page.</AlertDescription>
            </Alert>
        );
    }

    if (routeIdentity && !editing) {
        return (
            <div className="space-y-4">
                <Button variant="ghost" size="sm" className="-ml-2 text-muted-foreground" onClick={handleCancel}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Response Templates
                </Button>
                <Alert variant="destructive">
                    <AlertDescription>Response template not found.</AlertDescription>
                </Alert>
            </div>
        );
    }

    let title = 'Create a new Response Template';
    if (mode === 'edit') {
        title = readOnly ? 'View Response Template' : 'Edit Response Template';
    }

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div>
                    <Button variant="ghost" size="sm" className="-ml-2 mb-3 text-muted-foreground" onClick={handleCancel}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Response Templates
                    </Button>
                    <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
                    <p className="mt-1 text-sm text-muted-foreground">
                        Override the gateway response for a template key and Accept header.
                    </p>
                </div>
                {!readOnly ? (
                    <div className="flex shrink-0 items-center gap-2">
                        <Button type="button" variant="outline" onClick={handleCancel} disabled={mutation.isPending}>
                            Cancel
                        </Button>
                        <Button type="button" onClick={handleSubmit} disabled={mutation.isPending || (attemptedSubmit && !canSubmit)}>
                            {mutation.isPending ? 'Saving…' : mode === 'edit' ? 'Save' : 'Create'}
                        </Button>
                    </div>
                ) : null}
            </div>

            {isKubernetesManaged ? <KubernetesManagedReadOnlyAlert resource="Response templates" /> : null}

            <Card>
                <CardContent className="space-y-6 pt-6">
                    <div className="space-y-2">
                        <Label htmlFor={keyId}>
                            Template key <span className="text-destructive">*</span>
                        </Label>
                        <p className="text-xs text-muted-foreground">The template key for which template responses will be applied.</p>
                        <Popover open={keyOpen} onOpenChange={setKeyOpen}>
                            <PopoverTrigger asChild>
                                <Button
                                    id={keyId}
                                    type="button"
                                    variant="outline"
                                    role="combobox"
                                    aria-expanded={keyOpen}
                                    disabled={readOnly}
                                    className="w-full justify-between font-normal"
                                    aria-invalid={attemptedSubmit && Boolean(keyError)}
                                    aria-describedby={attemptedSubmit && keyError ? `${keyId}-error` : undefined}
                                >
                                    <span className={key ? undefined : 'text-muted-foreground'}>{key || 'Select or type a key…'}</span>
                                    <ChevronsUpDownIcon className="size-4 shrink-0 opacity-50" aria-hidden />
                                </Button>
                            </PopoverTrigger>
                            <PopoverContent className="w-[var(--radix-popover-trigger-width)] p-0" align="start">
                                <Command>
                                    <CommandInput placeholder="Search or enter a key…" value={key} onValueChange={setKey} />
                                    <CommandList>
                                        <CommandEmpty>No matching keys — your custom value will be used.</CommandEmpty>
                                        <CommandGroup>
                                            <ScrollArea className="max-h-60">
                                                {RESPONSE_TEMPLATE_KEYS.map(suggestion => (
                                                    <CommandItem
                                                        key={suggestion}
                                                        value={suggestion}
                                                        onSelect={() => {
                                                            setKey(suggestion);
                                                            setKeyOpen(false);
                                                        }}
                                                    >
                                                        {suggestion}
                                                    </CommandItem>
                                                ))}
                                            </ScrollArea>
                                        </CommandGroup>
                                    </CommandList>
                                </Command>
                            </PopoverContent>
                        </Popover>
                        {attemptedSubmit && keyError ? (
                            <p className="text-sm text-destructive" id={`${keyId}-error`}>
                                {keyError}
                            </p>
                        ) : null}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={acceptId}>
                            Accept header to match <span className="text-destructive">*</span>
                        </Label>
                        <p className="text-xs text-muted-foreground">
                            The Accept header of requests for which this template response should be used.
                        </p>
                        <Input
                            id={acceptId}
                            value={acceptHeader}
                            onChange={e => setAcceptHeader(e.target.value)}
                            disabled={readOnly}
                            aria-invalid={attemptedSubmit && Boolean(acceptError)}
                            aria-describedby={attemptedSubmit && acceptError ? `${acceptId}-error` : undefined}
                        />
                        {attemptedSubmit && acceptError ? (
                            <p className="text-sm text-destructive" id={`${acceptId}-error`}>
                                {acceptError}
                            </p>
                        ) : null}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={statusId}>
                            Status code <span className="text-destructive">*</span>
                        </Label>
                        <p className="text-xs text-muted-foreground">The status code sent to the API consumer.</p>
                        <div className="flex items-center gap-2">
                            <Input
                                id={statusId}
                                value={statusCode}
                                onChange={e => setStatusCode(e.target.value)}
                                disabled={readOnly}
                                className="w-28"
                                aria-invalid={attemptedSubmit && Boolean(statusError)}
                                aria-describedby={attemptedSubmit && statusError ? `${statusId}-error` : undefined}
                            />
                            <Popover open={statusOpen} onOpenChange={setStatusOpen}>
                                <PopoverTrigger asChild>
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="icon"
                                        className="size-9 shrink-0"
                                        disabled={readOnly}
                                        aria-label="Browse status codes"
                                        aria-expanded={statusOpen}
                                    >
                                        <ChevronsUpDownIcon className="size-4 opacity-50" aria-hidden />
                                    </Button>
                                </PopoverTrigger>
                                <PopoverContent className="w-72 p-0" align="start">
                                    <Command>
                                        <CommandInput placeholder="Search status codes…" />
                                        <CommandList>
                                            <CommandEmpty>No matching status codes.</CommandEmpty>
                                            <CommandGroup>
                                                <ScrollArea className="max-h-60">
                                                    {HTTP_STATUS_CODES.map(status => (
                                                        <CommandItem
                                                            key={`${status.code}-${status.label}`}
                                                            value={`${status.code} ${status.label}`}
                                                            onSelect={() => {
                                                                setStatusCode(String(status.code));
                                                                setStatusOpen(false);
                                                            }}
                                                        >
                                                            {status.code} — {status.label}
                                                        </CommandItem>
                                                    ))}
                                                </ScrollArea>
                                            </CommandGroup>
                                        </CommandList>
                                    </Command>
                                </PopoverContent>
                            </Popover>
                            {selectedStatus ? <span className="text-sm italic text-muted-foreground">{selectedStatus.label}</span> : null}
                        </div>
                        {attemptedSubmit && statusError ? (
                            <p className="text-sm text-destructive" id={`${statusId}-error`}>
                                {statusError}
                            </p>
                        ) : null}
                    </div>

                    <div className="space-y-2">
                        <Label>HTTP Headers</Label>
                        <div className="space-y-2">
                            <div className="flex items-center gap-2 px-0.5">
                                <span className="flex-1 text-xs font-medium text-muted-foreground">Header name</span>
                                <span className="flex-1 text-xs font-medium text-muted-foreground">Value</span>
                                <span className="size-8 shrink-0" aria-hidden />
                            </div>
                            {headers.map(h => (
                                <div key={h._id} className="flex items-center gap-2">
                                    <Input
                                        value={h.name}
                                        placeholder="Header name"
                                        onChange={e =>
                                            setHeaders(prev =>
                                                prev.map(row => (row._id === h._id ? { ...row, name: e.target.value } : row)),
                                            )
                                        }
                                        className="flex-1"
                                        disabled={readOnly}
                                        aria-label="Header name"
                                    />
                                    <Input
                                        value={h.value}
                                        placeholder="Value"
                                        onChange={e =>
                                            setHeaders(prev =>
                                                prev.map(row => (row._id === h._id ? { ...row, value: e.target.value } : row)),
                                            )
                                        }
                                        className="flex-1"
                                        disabled={readOnly}
                                        aria-label="Header value"
                                    />
                                    {!readOnly ? (
                                        <Button
                                            type="button"
                                            size="sm"
                                            variant="ghost"
                                            className="size-8 shrink-0 p-0 text-destructive hover:text-destructive"
                                            aria-label="Remove header"
                                            onClick={() =>
                                                setHeaders(prev =>
                                                    prev.length <= 1 ? [newHeaderRow()] : prev.filter(row => row._id !== h._id),
                                                )
                                            }
                                        >
                                            <Trash2Icon className="size-3.5" aria-hidden />
                                        </Button>
                                    ) : (
                                        <span className="size-8 shrink-0" aria-hidden />
                                    )}
                                </div>
                            ))}
                        </div>
                        {!readOnly ? (
                            <Button
                                type="button"
                                size="sm"
                                variant="outline"
                                className="gap-1.5"
                                onClick={() => setHeaders(prev => [...prev, newHeaderRow()])}
                            >
                                <PlusIcon className="size-3.5" aria-hidden />
                                Add header
                            </Button>
                        ) : null}
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor={bodyId}>Body</Label>
                        <p className="text-xs text-muted-foreground">Response template body.</p>
                        <Textarea
                            id={bodyId}
                            value={body}
                            onChange={e => setBody(e.target.value)}
                            disabled={readOnly}
                            className="min-h-32 font-mono text-sm"
                            placeholder="Body"
                        />
                    </div>

                    <div className="flex items-center justify-between gap-4">
                        <Label htmlFor={propagateId} className="text-sm font-medium">
                            Add template key to logs
                        </Label>
                        <Switch
                            id={propagateId}
                            checked={propagateErrorKeyToLogs}
                            onCheckedChange={setPropagateErrorKeyToLogs}
                            disabled={readOnly}
                        />
                    </div>
                </CardContent>
            </Card>
        </div>
    );
}
