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
import { useHasPermission } from '@gravitee/gamma-modules-sdk';
import {
    Alert,
    AlertDescription,
    Badge,
    Button,
    Card,
    CardContent,
    Field,
    FieldDescription,
    FieldError,
    FieldLabel,
    Input,
    type JsonSchema,
    JsonSchemaForm,
    RadioGroup,
    RadioGroupItem,
    Skeleton,
    cn,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, CheckIcon, CircleCheckIcon } from '@gravitee/graphene-core/icons';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { type FieldValues, useForm } from 'react-hook-form';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';

import { DocumentationContentEditor } from './DocumentationContentEditor';
import { FETCHER_ICONS, NAME_MAX, SOURCE_OPTIONS, TypeIcon, fileAccept, typeLabel } from './documentation-shared';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import {
    useApiDocumentationPage,
    useApiDocumentationPages,
    useCreateDocumentationPage,
    useDocumentationFetchers,
    useUpdateDocumentationPage,
} from '../../../hooks/useApiDocumentation';
import type { FetcherListItem, PageSourceType, SupportedEditPageType } from '../../../types/documentation';
import { parseFetcherSchema, toApiParentId } from '../../../utils/documentationFormatters';

function parsePageType(value: string | null): SupportedEditPageType {
    if (value === 'MARKDOWN' || value === 'SWAGGER' || value === 'ASYNCAPI') return value;
    return 'MARKDOWN';
}

function DocumentationStepper({ steps, current }: { steps: string[]; current: number }) {
    return (
        <nav aria-label="Create documentation page" className="flex items-center">
            {steps.map((label, index) => {
                const state = index < current ? 'completed' : index === current ? 'active' : 'upcoming';
                return (
                    <div key={label} className="flex flex-1 items-center">
                        <div className="flex w-full items-center justify-center gap-2.5 rounded-lg px-3 py-2 text-sm">
                            <span
                                className={cn(
                                    'flex size-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold',
                                    state === 'active' && 'bg-primary text-primary-foreground',
                                    state === 'upcoming' && 'bg-muted text-muted-foreground',
                                )}
                                style={
                                    state === 'completed'
                                        ? {
                                              backgroundColor: 'color-mix(in oklab, var(--color-success) 10%, transparent)',
                                              color: 'var(--color-success)',
                                          }
                                        : undefined
                                }
                                aria-current={state === 'active' ? 'step' : undefined}
                            >
                                {state === 'completed' ? <CircleCheckIcon className="size-3.5" aria-hidden /> : index + 1}
                            </span>
                            <span className={cn('whitespace-nowrap text-sm font-medium', state === 'upcoming' && 'text-muted-foreground')}>
                                {label}
                            </span>
                        </div>
                        {index < steps.length - 1 ? (
                            <div
                                className="h-px w-8 shrink-0"
                                style={{ backgroundColor: state === 'completed' ? 'var(--color-success)' : 'var(--color-border)' }}
                            />
                        ) : null}
                    </div>
                );
            })}
        </nav>
    );
}

function SourcePicker({
    value,
    disabled,
    onChange,
}: {
    value: PageSourceType;
    disabled: boolean;
    onChange: (value: PageSourceType) => void;
}) {
    return (
        <RadioGroup
            value={value}
            disabled={disabled}
            onValueChange={next => {
                if (next === 'FILL' || next === 'IMPORT' || next === 'EXTERNAL') onChange(next);
            }}
            className="grid gap-2 md:grid-cols-3"
        >
            {SOURCE_OPTIONS.map(option => {
                const selected = value === option.value;
                return (
                    <label
                        key={option.value}
                        className={cn(
                            'flex items-start gap-3 rounded-xl border p-4',
                            disabled ? 'cursor-default' : 'cursor-pointer',
                            selected ? 'border-primary bg-primary/5' : 'hover:bg-muted/60',
                        )}
                    >
                        <RadioGroupItem value={option.value} className="mt-0.5" />
                        <option.Icon className="size-6 shrink-0 text-muted-foreground" aria-hidden />
                        <div>
                            <p className="text-sm font-medium">{option.title}</p>
                            <p className="mt-1 text-xs text-muted-foreground">{option.description}</p>
                        </div>
                    </label>
                );
            })}
        </RadioGroup>
    );
}

function FetcherPicker({
    fetchers,
    value,
    disabled,
    error,
    onChange,
}: {
    fetchers: FetcherListItem[];
    value: string;
    disabled: boolean;
    error?: string | null;
    onChange: (value: string) => void;
}) {
    return (
        <div className="space-y-2">
            <p className="text-sm font-medium">Page source</p>
            <RadioGroup value={value} disabled={disabled} onValueChange={onChange} className="grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
                {fetchers.map(fetcher => {
                    const selected = value === fetcher.id;
                    const Icon = FETCHER_ICONS[fetcher.id];
                    return (
                        <label
                            key={fetcher.id}
                            className={cn(
                                'flex items-center gap-3 rounded-xl border p-3',
                                disabled ? 'cursor-default' : 'cursor-pointer',
                                selected ? 'border-primary bg-primary/5' : 'hover:bg-muted/60',
                            )}
                        >
                            <RadioGroupItem value={fetcher.id} />
                            {Icon ? <Icon className="size-6 shrink-0 text-muted-foreground" aria-hidden /> : null}
                            <p className="text-sm font-medium">{fetcher.name ?? fetcher.id}</p>
                        </label>
                    );
                })}
            </RadioGroup>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </div>
    );
}

function ImportFileDropzone({
    type,
    fileName,
    disabled,
    error,
    onFile,
}: {
    type: SupportedEditPageType;
    fileName: string;
    disabled: boolean;
    error?: string | null;
    onFile: (file: File) => void;
}) {
    const fileId = useId();
    const fileRef = useRef<HTMLInputElement>(null);
    const [isDragOver, setIsDragOver] = useState(false);
    const accept = fileAccept(type);

    return (
        <div className="space-y-2">
            <h2 className="text-base font-semibold">Upload a file</h2>
            <input
                ref={fileRef}
                id={fileId}
                type="file"
                accept={accept}
                className="hidden"
                disabled={disabled}
                onChange={event => {
                    const file = event.target.files?.[0];
                    event.target.value = '';
                    if (file) onFile(file);
                }}
            />
            <button
                type="button"
                disabled={disabled}
                onClick={() => fileRef.current?.click()}
                onDragOver={event => {
                    event.preventDefault();
                    setIsDragOver(true);
                }}
                onDragLeave={() => setIsDragOver(false)}
                onDrop={event => {
                    event.preventDefault();
                    setIsDragOver(false);
                    const file = event.dataTransfer.files[0];
                    if (file) onFile(file);
                }}
                className={cn(
                    'flex w-full flex-col items-center justify-center rounded-xl border border-dashed px-6 py-10 text-center',
                    isDragOver ? 'border-primary bg-primary/5' : 'hover:bg-muted/40',
                )}
            >
                <p className="text-sm font-medium">Drag and drop a file to upload it.</p>
                <p className="mt-1 text-xs text-muted-foreground">Alternatively, click here to choose a file.</p>
                <p className="mt-2 text-xs text-muted-foreground">Supported file formats: {accept.replace(/,/g, ', ')}</p>
                {fileName ? <p className="mt-3 text-sm font-medium">{fileName}</p> : null}
            </button>
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
        </div>
    );
}

export function ApiDocumentationEditPage() {
    const { apiId, pageId } = useParams<{ apiId: string; pageId: string }>();
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { api } = useApiDetailContext();
    const isNew = !pageId;
    const parentFromQuery = searchParams.get('parentId') || 'ROOT';
    const typeFromQuery = parsePageType(searchParams.get('pageType'));

    const canCreate = useHasPermission({ anyOf: ['api-documentation-c'] });
    const canUpdate = useHasPermission({ anyOf: ['api-documentation-u'] });
    const isKubernetes = api?.definitionContext?.origin === 'KUBERNETES';
    const canSave = (isNew ? canCreate : canUpdate) && !isKubernetes;
    const readOnly = !canSave;
    const nameId = useId();

    const pageQuery = useApiDocumentationPage(apiId, pageId);
    const siblingsQuery = useApiDocumentationPages(apiId, isNew ? parentFromQuery : toApiParentId(pageQuery.data?.parentId));
    const fetchersQuery = useDocumentationFetchers();
    const createMutation = useCreateDocumentationPage(apiId ?? '');
    const updateMutation = useUpdateDocumentationPage(apiId ?? '');
    const fetchers = fetchersQuery.data ?? [];

    const [showErrors, setShowErrors] = useState(false);
    const [stepIndex, setStepIndex] = useState(0);
    const [initialized, setInitialized] = useState(false);
    const [name, setName] = useState('');
    const [type, setType] = useState<SupportedEditPageType>(typeFromQuery);
    const [sourceType, setSourceType] = useState<PageSourceType>('FILL');
    const [content, setContent] = useState('');
    const [fileName, setFileName] = useState('');
    const [fetcherType, setFetcherType] = useState('');
    const [savedSnapshot, setSavedSnapshot] = useState('');

    const fetcherForm = useForm<FieldValues>({ defaultValues: { configuration: {} } });
    const selectedFetcher = fetchers.find(fetcher => fetcher.id === fetcherType);
    const fetcherSchema: JsonSchema | undefined = selectedFetcher ? parseFetcherSchema(selectedFetcher) : undefined;

    useEffect(() => {
        if (isNew) {
            setType(typeFromQuery);
            return;
        }
        if (!pageQuery.data || initialized) return;
        const page = pageQuery.data;
        if (page.type === 'FOLDER') {
            navigate({ pathname: '..' }, { replace: true });
            return;
        }
        setName(page.name ?? '');
        if (page.type === 'MARKDOWN' || page.type === 'SWAGGER' || page.type === 'ASYNCAPI') setType(page.type);
        setContent(page.content ?? '');
        if (page.source?.type) {
            setSourceType('EXTERNAL');
            setFetcherType(page.source.type);
            fetcherForm.reset({ configuration: page.source.configuration ?? {} });
        }
        setSavedSnapshot(
            JSON.stringify({
                name: page.name ?? '',
                content: page.content ?? '',
                sourceType: page.source?.type ? 'EXTERNAL' : 'FILL',
                fetcherType: page.source?.type ?? '',
            }),
        );
        setInitialized(true);
    }, [fetcherForm, initialized, isNew, navigate, pageQuery.data, typeFromQuery]);

    const existingNames = (siblingsQuery.pages ?? [])
        .filter(item => item.id !== pageId)
        .map(item => (item.name ?? '').toLowerCase().trim());
    const breadcrumbs = siblingsQuery.breadcrumbs ?? [];
    const parentId = isNew ? parentFromQuery : toApiParentId(pageQuery.data?.parentId);

    const goBack = () => navigate({ pathname: '..' });

    const trimmed = name.trim();
    const nameError =
        trimmed === ''
            ? 'Page name is required.'
            : existingNames.includes(trimmed.toLowerCase())
              ? 'A page or folder with this name already exists here.'
              : null;
    const fetcherError = sourceType === 'EXTERNAL' && !fetcherType ? 'Select a page source.' : null;
    const contentError = sourceType !== 'EXTERNAL' && content.trim() === '' ? 'Page content cannot be empty.' : null;
    const currentSnapshot = JSON.stringify({ name, content, sourceType, fetcherType });
    const isDirty = !isNew && savedSnapshot !== '' && currentSnapshot !== savedSnapshot;
    const isSaving = createMutation.isPending || updateMutation.isPending;

    const steps = useMemo(
        () => [
            'Configure page',
            'Determine source',
            sourceType === 'IMPORT' ? 'Upload a file' : sourceType === 'EXTERNAL' ? 'Configure External Source' : 'Add content',
        ],
        [sourceType],
    );

    function handleDiscard() {
        if (!savedSnapshot) return;
        const saved = JSON.parse(savedSnapshot) as {
            name: string;
            content: string;
            sourceType: PageSourceType;
            fetcherType: string;
        };
        setName(saved.name);
        setContent(saved.content);
        setSourceType(saved.sourceType);
        setFetcherType(saved.fetcherType);
        setFileName('');
        setShowErrors(false);
    }

    function handleSourceType(next: PageSourceType) {
        setSourceType(next);
        if (next !== 'EXTERNAL') {
            setFetcherType('');
            fetcherForm.reset({ configuration: {} });
        }
    }

    async function handleFile(file: File) {
        const text = await file.text();
        setFileName(file.name);
        setContent(text);
        if (!trimmed) setName(file.name.replace(/\.[^.]+$/, ''));
    }

    function validateForSave(): boolean {
        if (nameError || fetcherError || contentError) {
            setShowErrors(true);
            if (isNew) {
                if (nameError) setStepIndex(0);
                else if (fetcherError) setStepIndex(1);
                else setStepIndex(2);
            }
            return false;
        }
        return true;
    }

    function handleSave() {
        if (!apiId || !validateForSave()) return;
        const sourceConfiguration = (fetcherForm.getValues('configuration') ?? {}) as Record<string, unknown>;
        const payload = {
            type,
            name: trimmed,
            parentId,
            content,
            ...(sourceType === 'EXTERNAL' && fetcherType
                ? { source: { type: fetcherType, configuration: sourceConfiguration } }
                : {}),
        };
        if (isNew) {
            createMutation.mutate(payload, {
                onSuccess: () => {
                    notify.success('Page created');
                    goBack();
                },
                onError: error => notify.error(error, 'Could not create page.'),
            });
            return;
        }
        if (!pageId || !pageQuery.data) return;
        updateMutation.mutate(
            { pageId, payload: { ...pageQuery.data, ...payload } },
            {
                onSuccess: () => {
                    notify.success('Page updated');
                    setSavedSnapshot(JSON.stringify({ name: trimmed, content, sourceType, fetcherType }));
                    setShowErrors(false);
                },
                onError: error => notify.error(error, 'Could not update page.'),
            },
        );
    }

    function handleNext() {
        if (stepIndex === 0 && nameError) {
            setShowErrors(true);
            return;
        }
        if (stepIndex === 1 && fetcherError) {
            setShowErrors(true);
            return;
        }
        setShowErrors(false);
        setStepIndex(current => Math.min(current + 1, steps.length - 1));
    }

    const sourcePickers = (
        <>
            <Field>
                <FieldLabel>Determine source</FieldLabel>
                <SourcePicker value={sourceType} disabled={isSaving || readOnly} onChange={handleSourceType} />
            </Field>
            {sourceType === 'EXTERNAL' ? (
                <FetcherPicker
                    fetchers={fetchers}
                    value={fetcherType}
                    disabled={isSaving || readOnly}
                    error={showErrors ? fetcherError : null}
                    onChange={setFetcherType}
                />
            ) : null}
        </>
    );

    if (!isNew && pageQuery.isLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-72 rounded" />
                <Skeleton className="h-64 w-full rounded-xl" />
            </div>
        );
    }

    if (!isNew && pageQuery.isError) {
        return (
            <div className="space-y-4">
                <Alert variant="destructive">
                    <AlertDescription>Failed to load this page. It may have been deleted.</AlertDescription>
                </Alert>
                <Button variant="outline" size="sm" onClick={goBack}>
                    Back to Documentation
                </Button>
            </div>
        );
    }

    const displayName = trimmed || (isNew ? 'Add new page' : 'Untitled page');

    return (
        <div className="space-y-6">
            <div className="flex items-start justify-between gap-4">
                <div className="space-y-1">
                    <Button type="button" variant="ghost" size="sm" className="-ml-2 gap-1.5" onClick={goBack} disabled={isSaving}>
                        <ArrowLeftIcon className="size-4" aria-hidden />
                        Documentation
                    </Button>
                    <div className="flex items-center gap-2">
                        <TypeIcon type={type} className="size-6" />
                        <h1 className="text-2xl font-semibold tracking-tight">{displayName}</h1>
                        <Badge variant="secondary">{typeLabel(type)}</Badge>
                    </div>
                    {breadcrumbs.length > 0 ? (
                        <p className="flex flex-wrap items-center gap-1 text-sm text-muted-foreground">
                            <span>In</span>
                            {breadcrumbs.map((crumb, index) => (
                                <span key={crumb.id} className="flex items-center gap-1">
                                    {index > 0 ? <span>/</span> : null}
                                    <span className="font-medium text-foreground">{crumb.name}</span>
                                </span>
                            ))}
                        </p>
                    ) : null}
                </div>
                {isNew ? (
                    <Button type="button" variant="outline" size="sm" onClick={goBack} disabled={isSaving}>
                        Exit without saving
                    </Button>
                ) : isDirty && !readOnly ? (
                    <div className="flex shrink-0 items-center gap-2">
                        <Button type="button" variant="outline" size="sm" onClick={handleDiscard} disabled={isSaving}>
                            Discard
                        </Button>
                        <Button type="button" size="sm" onClick={handleSave} disabled={isSaving}>
                            <CheckIcon className="size-4" />
                            {isSaving ? 'Saving…' : 'Save changes'}
                        </Button>
                    </div>
                ) : null}
            </div>

            {isKubernetes ? (
                <Alert>
                    <AlertDescription>This API is managed by the Kubernetes operator. Documentation is read-only.</AlertDescription>
                </Alert>
            ) : null}

            {isNew ? (
                <>
                    <Card>
                        <CardContent className="py-3">
                            <DocumentationStepper steps={steps} current={stepIndex} />
                        </CardContent>
                    </Card>

                    {stepIndex === 0 ? (
                        <Card>
                            <CardContent className="space-y-4 pt-6">
                                <Field>
                                    <FieldLabel htmlFor={nameId} required>
                                        Name
                                    </FieldLabel>
                                    <Input
                                        id={nameId}
                                        value={name}
                                        maxLength={NAME_MAX}
                                        disabled={isSaving || readOnly}
                                        aria-invalid={showErrors && nameError ? true : undefined}
                                        onChange={event => setName(event.target.value)}
                                    />
                                    <FieldDescription>
                                        {name.length}/{NAME_MAX}
                                    </FieldDescription>
                                    {showErrors && nameError ? <FieldError>{nameError}</FieldError> : null}
                                </Field>
                                <Field>
                                    <FieldLabel>Type</FieldLabel>
                                    <div className="flex items-center gap-2">
                                        <TypeIcon type={type} className="size-6" />
                                        <p className="text-sm">{typeLabel(type)}</p>
                                    </div>
                                    <FieldDescription>Chosen when you added the page. Type cannot change after create.</FieldDescription>
                                </Field>
                            </CardContent>
                        </Card>
                    ) : null}

                    {stepIndex === 1 ? (
                        <Card>
                            <CardContent className="space-y-4 pt-6">{sourcePickers}</CardContent>
                        </Card>
                    ) : null}

                    {stepIndex === 2 ? (
                        <Card>
                            <CardContent className="space-y-4 pt-6">
                                {sourceType === 'FILL' ? (
                                    <DocumentationContentEditor
                                        type={type}
                                        content={content}
                                        readOnly={readOnly}
                                        isExternal={false}
                                        error={showErrors ? contentError : null}
                                        onChange={setContent}
                                    />
                                ) : null}
                                {sourceType === 'IMPORT' ? (
                                    <>
                                        <ImportFileDropzone
                                            type={type}
                                            fileName={fileName}
                                            disabled={isSaving || readOnly}
                                            error={showErrors ? contentError : null}
                                            onFile={file => void handleFile(file)}
                                        />
                                        {content.trim() ? (
                                            <DocumentationContentEditor
                                                type={type}
                                                content={content}
                                                readOnly={readOnly}
                                                isExternal={false}
                                                onChange={setContent}
                                            />
                                        ) : null}
                                    </>
                                ) : null}
                                {sourceType === 'EXTERNAL' ? (
                                    <div className="space-y-4">
                                        <div>
                                            <h2 className="text-base font-semibold">Configure External Source</h2>
                                            <p className="mt-1 text-sm text-muted-foreground">
                                                The gateway will fetch this file. Saving stores the configuration; it does not publish the
                                                page.
                                            </p>
                                        </div>
                                        {fetcherSchema ? (
                                            <JsonSchemaForm schema={fetcherSchema} control={fetcherForm.control} name="configuration" />
                                        ) : (
                                            <p className="text-sm text-muted-foreground">Select a page source to configure it.</p>
                                        )}
                                    </div>
                                ) : null}
                            </CardContent>
                        </Card>
                    ) : null}

                    <div className="flex items-center justify-between">
                        <Button
                            type="button"
                            variant="outline"
                            onClick={stepIndex === 0 ? goBack : () => setStepIndex(current => current - 1)}
                            disabled={isSaving}
                        >
                            {stepIndex === 0 ? 'Cancel' : 'Previous'}
                        </Button>
                        {readOnly ? (
                            <Button type="button" variant="outline" onClick={goBack}>
                                Close
                            </Button>
                        ) : stepIndex < steps.length - 1 ? (
                            <Button type="button" onClick={handleNext}>
                                Next
                            </Button>
                        ) : (
                            <Button type="button" onClick={handleSave} disabled={isSaving}>
                                {isSaving ? 'Saving…' : 'Save'}
                            </Button>
                        )}
                    </div>
                </>
            ) : (
                <Card>
                    <CardContent className="pt-6">
                        {sourceType === 'IMPORT' && !content.trim() ? (
                            <ImportFileDropzone
                                type={type}
                                fileName={fileName}
                                disabled={isSaving || readOnly}
                                error={showErrors ? contentError : null}
                                onFile={file => void handleFile(file)}
                            />
                        ) : (
                            <DocumentationContentEditor
                                type={type}
                                content={content}
                                readOnly={readOnly}
                                isExternal={sourceType === 'EXTERNAL'}
                                error={showErrors ? contentError : null}
                                onChange={setContent}
                            />
                        )}
                    </CardContent>
                </Card>
            )}
        </div>
    );
}
