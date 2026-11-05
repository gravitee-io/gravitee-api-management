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
import { useEnvironment } from '@gravitee/gamma-modules-sdk';
import {
    Badge,
    Button,
    Card,
    CardContent,
    cn,
    extractDefaults,
    Input,
    type JsonSchema,
    JsonSchemaForm,
    jsonSchemaResolver,
    Label,
    Skeleton,
} from '@gravitee/graphene-core';
import { ArrowLeftIcon, SearchIcon } from '@gravitee/graphene-core/icons';
import { useQueryClient } from '@tanstack/react-query';
import { type ReactNode, useEffect, useId, useMemo, useState } from 'react';
import { type FieldErrors, type FieldValues, type Resolver, useForm } from 'react-hook-form';
import { Navigate, useNavigate, useParams } from 'react-router-dom';

import { ApimApiError } from '../../../../../shared/api/apimClient';
import { notify } from '../../../../../shared/notify';
import { PluginIcon } from '../../../../apis/components/PluginIcon';
import { WizardStepIndicator, type WizardStep as WizardStepDescriptor } from '../../../../apis/components/WizardStepIndicator';
import { useResourcePlugins, useResourceSchema } from '../../../../apis/hooks/useApiResources';
import type { ApiResource, ResourcePlugin } from '../../../../apis/types/resource';
import { useApiProductDetailContext } from '../../../context/ApiProductDetailContext';
import { useApiProductResourcePermissions } from '../../../hooks/useApiProductPermissions';
import { useUpdateApiProductResources } from '../../../hooks/useUpdateApiProductResources';
import { upsertProductResourceApply } from '../../../utils/apiProductResourceListMutations';
import { isApiProductResourcePlugin } from '../../../utils/apiProductResourcePlugins';
import { apiProductKeys } from '../../../utils/queryKeys';

type WizardStep = 'select-type' | 'configure';

// ─── Footer ─────────────────────────────────────────────────────────────────

function WizardFooter({
    onBack,
    backLabel,
    stepText,
    action,
}: {
    onBack: () => void;
    backLabel: string;
    stepText?: string;
    action: ReactNode;
}) {
    return (
        <div className="flex items-center justify-between border-t pt-4">
            <Button variant="ghost" size="sm" onClick={onBack}>
                <ArrowLeftIcon className="size-4" />
                {backLabel}
            </Button>
            <div className="flex items-center gap-4">
                {stepText ? <span className="text-xs text-muted-foreground">{stepText}</span> : null}
                {action}
            </div>
        </div>
    );
}

// ─── Step 1: select type ──────────────────────────────────────────────────

function CategoryChip({ active, children, onClick }: { active: boolean; children: ReactNode; onClick: () => void }) {
    return (
        <button
            type="button"
            onClick={onClick}
            aria-pressed={active}
            className={cn(
                'rounded-full border px-3 py-1 text-xs font-medium transition-colors',
                active ? 'border-primary bg-primary/10 text-primary' : 'border-border text-muted-foreground hover:bg-muted',
            )}
        >
            {children}
        </button>
    );
}

function SelectTypeStep({
    plugins,
    pluginsLoading,
    selectedId,
    onSelect,
    onBack,
    onNext,
    stepText,
}: {
    plugins: ResourcePlugin[];
    pluginsLoading: boolean;
    selectedId: string | undefined;
    onSelect: (plugin: ResourcePlugin) => void;
    onBack: () => void;
    onNext: () => void;
    stepText: string;
}) {
    const [search, setSearch] = useState('');
    const [category, setCategory] = useState('all');

    const categories = useMemo(() => Array.from(new Set(plugins.map(p => p.category).filter((c): c is string => !!c))).sort(), [plugins]);

    const filtered = useMemo(() => {
        const q = search.trim().toLowerCase();
        return plugins.filter(p => {
            if (category !== 'all' && p.category !== category) return false;
            if (!q) return true;
            return p.name.toLowerCase().includes(q) || p.id.toLowerCase().includes(q) || (p.description ?? '').toLowerCase().includes(q);
        });
    }, [plugins, search, category]);

    return (
        <Card>
            <CardContent className="space-y-4 pt-6">
                <div>
                    <h2 className="text-base font-semibold">Select Resource Type</h2>
                    <p className="text-xs text-muted-foreground">Choose the type of resource you want to configure for this API Product.</p>
                </div>

                <div className="relative">
                    <SearchIcon
                        className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground pointer-events-none"
                        aria-hidden
                    />
                    <Input placeholder="Search resource types…" value={search} onChange={e => setSearch(e.target.value)} className="pl-9" />
                </div>

                {categories.length > 0 ? (
                    <div className="flex flex-wrap gap-2">
                        <CategoryChip active={category === 'all'} onClick={() => setCategory('all')}>
                            All categories
                        </CategoryChip>
                        {categories.map(c => (
                            <CategoryChip key={c} active={category === c} onClick={() => setCategory(c)}>
                                {c}
                            </CategoryChip>
                        ))}
                    </div>
                ) : null}

                {pluginsLoading ? (
                    <div className="grid auto-rows-fr grid-cols-3 gap-4">
                        {Array.from({ length: 6 }).map((_, i) => (
                            <Skeleton key={i} className="h-32 rounded-xl" />
                        ))}
                    </div>
                ) : filtered.length === 0 ? (
                    <p className="py-10 text-center text-sm text-muted-foreground">No resource types match your search.</p>
                ) : (
                    <div className="grid auto-rows-fr grid-cols-3 gap-4">
                        {filtered.map(plugin => {
                            const selected = plugin.id === selectedId;
                            return (
                                <button
                                    key={plugin.id}
                                    type="button"
                                    onClick={() => onSelect(plugin)}
                                    aria-pressed={selected}
                                    className={cn(
                                        'flex h-full flex-col gap-2 rounded-xl border p-4 text-left transition-colors',
                                        selected
                                            ? 'border-primary bg-primary/5 ring-1 ring-primary'
                                            : 'border-border hover:border-primary/60',
                                    )}
                                >
                                    <div className="flex items-center justify-between gap-2">
                                        <div className="flex size-9 shrink-0 items-center justify-center rounded-lg bg-muted">
                                            <PluginIcon icon={plugin.icon} />
                                        </div>
                                        {plugin.category ? (
                                            <Badge variant="secondary" className="text-xs font-normal">
                                                {plugin.category}
                                            </Badge>
                                        ) : null}
                                    </div>
                                    <p className="text-sm font-semibold">{plugin.name}</p>
                                    {plugin.description ? (
                                        <p className="line-clamp-2 text-xs text-muted-foreground">{plugin.description}</p>
                                    ) : null}
                                </button>
                            );
                        })}
                    </div>
                )}

                <WizardFooter
                    onBack={onBack}
                    backLabel="Back to Resources"
                    stepText={stepText}
                    action={
                        <Button size="sm" onClick={onNext} disabled={!selectedId}>
                            Next
                        </Button>
                    }
                />
            </CardContent>
        </Card>
    );
}

// ─── Configure step: name + schema-driven form ───────────────────────────────

interface ConfigureResult {
    name: string;
    configuration: Record<string, unknown>;
}

function ResourceForm({
    schema,
    initial,
    existingNames,
    isSaving,
    backLabel,
    stepText,
    onBack,
    onSubmit,
}: {
    schema: JsonSchema;
    initial?: ApiResource;
    existingNames: string[];
    isSaving: boolean;
    backLabel: string;
    stepText?: string;
    onBack: () => void;
    onSubmit: (result: ConfigureResult) => void;
}) {
    const nameId = useId();

    // Schema fields live under `configuration` so they can never collide with the `name` field;
    // the wrapper adds the name validation the schema resolver doesn't know about.
    const resolver = useMemo<Resolver<FieldValues>>(() => {
        const base = jsonSchemaResolver(schema, { basePath: 'configuration' });
        return async (values, context, options) => {
            const result = await base(values, context, options);
            const name = typeof values.name === 'string' ? values.name.trim() : '';
            let nameError: { type: string; message: string } | null = null;
            if (!name) {
                nameError = { type: 'required', message: 'Name is required.' };
            } else if (existingNames.includes(name)) {
                nameError = { type: 'duplicate', message: 'A resource with this name already exists.' };
            }
            if (!nameError) return result;
            return { values: {}, errors: { ...result.errors, name: nameError } as FieldErrors<FieldValues> };
        };
    }, [schema, existingNames]);

    const defaultValues = useMemo(
        () => ({
            name: initial?.name ?? '',
            configuration: { ...((extractDefaults(schema) ?? {}) as Record<string, unknown>), ...(initial?.configuration ?? {}) },
        }),
        [schema, initial],
    );
    const form = useForm<FieldValues>({ resolver, mode: 'onTouched', defaultValues });
    const nameError = form.formState.errors.name?.message as string | undefined;

    const submit = form.handleSubmit(values =>
        onSubmit({ name: (values.name as string).trim(), configuration: values.configuration as Record<string, unknown> }),
    );

    return (
        <Card>
            <CardContent className="space-y-5 pt-6">
                <div className="space-y-2">
                    <Label htmlFor={nameId}>Resource name</Label>
                    <Input
                        id={nameId}
                        placeholder="e.g. my-cache"
                        autoComplete="off"
                        aria-invalid={nameError ? true : undefined}
                        aria-describedby={nameError ? `${nameId}-hint ${nameId}-error` : `${nameId}-hint`}
                        {...form.register('name')}
                    />
                    <p id={`${nameId}-hint`} className="text-xs text-muted-foreground">
                        Policies reference this resource by name at runtime — keep it stable once in use.
                    </p>
                    {nameError ? (
                        <p id={`${nameId}-error`} className="text-xs text-destructive">
                            {nameError}
                        </p>
                    ) : null}
                </div>

                <div className="space-y-3">
                    <span className="text-sm font-medium">Configuration</span>
                    <JsonSchemaForm schema={schema} control={form.control} name="configuration" />
                </div>

                <WizardFooter
                    onBack={onBack}
                    backLabel={backLabel}
                    stepText={stepText}
                    action={
                        <Button size="sm" onClick={submit} disabled={isSaving}>
                            {isSaving ? 'Saving…' : initial ? 'Save changes' : 'Create resource'}
                        </Button>
                    }
                />
            </CardContent>
        </Card>
    );
}

/** Loads the plugin schema, then mounts the form. */
function ConfigureStep(props: {
    plugin: ResourcePlugin;
    initial?: ApiResource;
    existingNames: string[];
    isSaving: boolean;
    backLabel: string;
    stepText?: string;
    onBack: () => void;
    onSubmit: (result: ConfigureResult) => void;
}) {
    const { data: schema, isLoading, isError } = useResourceSchema(props.plugin.id);

    if (isLoading) {
        return (
            <Card>
                <CardContent className="space-y-4 pt-6">
                    <Skeleton className="h-10 w-full rounded" />
                    <Skeleton className="h-32 w-full rounded" />
                    <WizardFooter onBack={props.onBack} backLabel={props.backLabel} stepText={props.stepText} action={null} />
                </CardContent>
            </Card>
        );
    }

    if (isError || !schema) {
        return (
            <Card className="border-destructive/30">
                <CardContent className="space-y-4 pt-6">
                    <p className="text-sm text-destructive">Failed to load the configuration schema for this resource type.</p>
                    <WizardFooter onBack={props.onBack} backLabel={props.backLabel} stepText={props.stepText} action={null} />
                </CardContent>
            </Card>
        );
    }

    return (
        <ResourceForm
            key={props.plugin.id}
            schema={schema as JsonSchema}
            initial={props.initial}
            existingNames={props.existingNames}
            isSaving={props.isSaving}
            backLabel={props.backLabel}
            stepText={props.stepText}
            onBack={props.onBack}
            onSubmit={props.onSubmit}
        />
    );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiProductResourceWizardPage() {
    const { productId, resourceName } = useParams<{ productId: string; resourceName?: string }>();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const editMode = Boolean(resourceName);

    const { product, isLoading } = useApiProductDetailContext();
    const { canRead, canUpdate, isLoading: permsLoading } = useApiProductResourcePermissions(productId, 'DEFINITION');
    const { data: allPlugins = [], isLoading: pluginsLoading } = useResourcePlugins();
    const plugins = useMemo(() => allPlugins.filter(p => isApiProductResourcePlugin(p.id)), [allPlugins]);
    const mutation = useUpdateApiProductResources(productId);

    const resources = useMemo<ApiResource[]>(() => product?.resources ?? [], [product?.resources]);
    const existing = useMemo(() => (resourceName ? resources.find(r => r.name === resourceName) : undefined), [resources, resourceName]);

    const [selectedPlugin, setSelectedPlugin] = useState<ResourcePlugin | null>(null);
    const [step, setStep] = useState<WizardStep>(editMode ? 'configure' : 'select-type');

    useEffect(() => {
        if (editMode && existing && plugins.length > 0) {
            setSelectedPlugin(prev => prev ?? plugins.find(p => p.id === existing.type) ?? null);
        }
    }, [editMode, existing, plugins]);

    // Edit mode has exactly one screen, so it carries no step indicator or step count of its own to show.
    const wizardSteps = useMemo<WizardStepDescriptor[]>(
        () =>
            editMode
                ? []
                : [
                      { id: 'select-type', label: 'Select Type' },
                      { id: 'configure', label: 'Configure' },
                  ],
        [editMode],
    );
    const stepLabel = editMode
        ? undefined
        : (stepId: WizardStep) => `Step ${wizardSteps.findIndex(s => s.id === stepId) + 1} of ${wizardSteps.length}`;

    const existingNames = useMemo(
        () => resources.filter(r => !editMode || r.name !== existing?.name).map(r => r.name),
        [resources, editMode, existing],
    );

    const goToList = () => navigate('..');

    const handleSubmit = (result: ConfigureResult) => {
        if (!canUpdate || !selectedPlugin) return;
        const resource: ApiResource = {
            name: result.name,
            type: selectedPlugin.id,
            enabled: existing?.enabled ?? true,
            configuration: result.configuration,
        };
        const apply = upsertProductResourceApply(resource, editMode ? existing?.name : undefined);
        mutation.mutate(apply, {
            onSuccess: () => {
                notify.success(editMode ? 'Resource updated.' : 'Resource created.');
                goToList();
            },
            onError: error => {
                notify.error(error, 'Failed to save resource.');
                if (error instanceof ApimApiError && error.status === 409 && env?.id && productId) {
                    void queryClient.invalidateQueries({ queryKey: apiProductKeys.detail(env.id, productId) });
                }
            },
        });
    };

    if (isLoading || permsLoading) {
        return (
            <div className="space-y-6">
                <Skeleton className="h-8 w-56 rounded" />
                <Skeleton className="h-10 w-full rounded" />
                <Skeleton className="h-64 w-full rounded-xl" />
            </div>
        );
    }

    if (!canRead) {
        return (
            <Card>
                <CardContent className="pt-4 pb-4">
                    <p className="text-sm text-muted-foreground">You do not have permission to view API Product resources.</p>
                </CardContent>
            </Card>
        );
    }

    if (!canUpdate) {
        return <Navigate to=".." replace />;
    }

    if (editMode && !isLoading && !existing) {
        return (
            <div className="space-y-4">
                <Card className="border-destructive/30">
                    <CardContent className="pt-4 pb-4">
                        <p className="text-sm text-destructive">Resource not found.</p>
                    </CardContent>
                </Card>
                <Button variant="outline" size="sm" onClick={goToList}>
                    <ArrowLeftIcon className="size-4" />
                    Back to Resources
                </Button>
            </div>
        );
    }

    if (editMode && existing) {
        if (pluginsLoading) {
            return (
                <div className="space-y-6">
                    <Skeleton className="h-8 w-56 rounded" />
                    <Skeleton className="h-64 w-full rounded-xl" />
                </div>
            );
        }
        const matchingPlugin = plugins.find(p => p.id === existing.type);
        if (!matchingPlugin) {
            return (
                <div className="space-y-4">
                    <Card className="border-destructive/30">
                        <CardContent className="pt-4 pb-4">
                            <p className="text-sm text-destructive">The resource type {existing.type} is not available.</p>
                        </CardContent>
                    </Card>
                    <Button variant="outline" size="sm" onClick={goToList}>
                        <ArrowLeftIcon className="size-4" />
                        Back to Resources
                    </Button>
                </div>
            );
        }
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">{editMode ? 'Edit Resource' : 'Add Resource'}</h1>
                <p className="text-sm text-muted-foreground">
                    {editMode
                        ? 'Update the configuration of this API Product resource.'
                        : 'Create a resource for this API Product with its own configuration.'}
                </p>
            </div>

            {wizardSteps.length > 0 ? (
                <WizardStepIndicator
                    steps={wizardSteps}
                    currentStepId={step}
                    onStepClick={id => setStep(id as WizardStep)}
                    ariaLabel="Resource creation steps"
                />
            ) : null}

            {step === 'select-type' ? (
                <SelectTypeStep
                    plugins={plugins}
                    pluginsLoading={pluginsLoading}
                    selectedId={selectedPlugin?.id}
                    onSelect={setSelectedPlugin}
                    onBack={goToList}
                    onNext={() => setStep('configure')}
                    stepText={stepLabel?.('select-type') ?? ''}
                />
            ) : selectedPlugin ? (
                <ConfigureStep
                    plugin={selectedPlugin}
                    initial={existing}
                    existingNames={existingNames}
                    isSaving={mutation.isPending}
                    backLabel={editMode ? 'Back to Resources' : 'Back'}
                    stepText={stepLabel?.('configure')}
                    onBack={() => (editMode ? goToList() : setStep('select-type'))}
                    onSubmit={handleSubmit}
                />
            ) : (
                <Skeleton className="h-64 w-full rounded-xl" />
            )}
        </div>
    );
}
