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
import { type ReactNode, useEffect, useId, useMemo, useState } from 'react';
import { type FieldErrors, type FieldValues, type Resolver, useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';

import { notify } from '../../../../../shared/notify';
import { PluginIcon } from '../../../components/PluginIcon';
import { ReviewRow } from '../../../components/ReviewRow';
import { WizardStepIndicator, type WizardStep as WizardStepDescriptor } from '../../../components/WizardStepIndicator';
import { useApiDetail } from '../../../hooks/useApiDetail';
import { useResourcePlugins, useResourceSchema, useUpdateApiResources } from '../../../hooks/useApiResources';
import type { ApiResource, ResourcePlugin } from '../../../types/resource';

type WizardStep = 'select-type' | 'configure' | 'review';

// ─── Footer ─────────────────────────────────────────────────────────────────

function WizardFooter({
    onBack,
    backLabel,
    stepText,
    action,
}: {
    onBack: () => void;
    backLabel: string;
    stepText: string;
    action: ReactNode;
}) {
    return (
        <div className="flex items-center justify-between border-t pt-4">
            <Button variant="ghost" size="sm" onClick={onBack}>
                <ArrowLeftIcon className="size-4" />
                {backLabel}
            </Button>
            <div className="flex items-center gap-4">
                <span className="text-xs text-muted-foreground">{stepText}</span>
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
                    <p className="text-xs text-muted-foreground">Choose the type of resource you want to configure for this API.</p>
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

// ─── Steps 2 & 3: configure + review (one mounted form) ──────────────────────

interface ConfigureReviewResult {
    name: string;
    configuration: Record<string, unknown>;
}

function ResourceForm({
    plugin,
    schema,
    step,
    initial,
    existingNames,
    isSaving,
    backLabel,
    stepLabel,
    onBack,
    onStep,
    onSubmit,
}: {
    plugin: ResourcePlugin;
    schema: JsonSchema;
    step: 'configure' | 'review';
    initial?: ApiResource;
    existingNames: string[];
    isSaving: boolean;
    backLabel: string;
    stepLabel: (step: WizardStep) => string;
    onBack: () => void;
    onStep: (step: WizardStep) => void;
    onSubmit: (result: ConfigureReviewResult) => void;
}) {
    const nameId = useId();

    // Schema fields live under `configuration` so they can never collide with the `name` field;
    // the wrapper adds the name validation the schema resolver doesn't know about.
    const resolver = useMemo<Resolver<FieldValues>>(() => {
        const base = jsonSchemaResolver(schema, { basePath: 'configuration' });
        return async (values, context, options) => {
            const result = await base(values, context, options);
            const name = typeof values.name === 'string' ? values.name.trim() : '';
            const nameError = !name
                ? { type: 'required', message: 'Name is required.' }
                : existingNames.includes(name)
                  ? { type: 'duplicate', message: 'A resource with this name already exists.' }
                  : null;
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

    const goToReview = async () => {
        if (await form.trigger()) onStep('review');
    };

    const submit = form.handleSubmit(values =>
        onSubmit({ name: (values.name as string).trim(), configuration: values.configuration as Record<string, unknown> }),
    );

    if (step === 'configure') {
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
                        stepText={stepLabel('configure')}
                        action={
                            <Button size="sm" onClick={goToReview}>
                                Next
                            </Button>
                        }
                    />
                </CardContent>
            </Card>
        );
    }

    const values = form.getValues();
    return (
        <Card>
            <CardContent className="space-y-5 pt-6">
                <div>
                    <h2 className="text-base font-semibold">{initial ? 'Review & Save' : 'Review & Create'}</h2>
                    <p className="text-xs text-muted-foreground">
                        {initial
                            ? 'Confirm the resource details before saving your changes.'
                            : 'Confirm the resource details before creating it.'}
                    </p>
                </div>

                <div className="rounded-lg border border-border">
                    <ReviewRow label="Name" value={String(values.name ?? '').trim()} />
                    <ReviewRow label="Type" value={plugin.name} />
                    <ReviewRow label="Status" value={initial?.enabled === false ? 'Disabled' : 'Enabled'} />
                </div>

                <div className="space-y-2">
                    <span className="text-sm font-medium">Configuration</span>
                    <pre className="max-h-72 overflow-auto rounded-lg bg-muted p-3 text-xs">
                        {JSON.stringify(values.configuration, null, 2)}
                    </pre>
                </div>

                <WizardFooter
                    onBack={() => onStep('configure')}
                    backLabel="Back"
                    stepText={stepLabel('review')}
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

/** Loads the plugin schema, then mounts the form (kept mounted across configure/review). */
function ConfigureReviewStep(props: {
    plugin: ResourcePlugin;
    step: 'configure' | 'review';
    initial?: ApiResource;
    existingNames: string[];
    isSaving: boolean;
    backLabel: string;
    stepLabel: (step: WizardStep) => string;
    onBack: () => void;
    onStep: (step: WizardStep) => void;
    onSubmit: (result: ConfigureReviewResult) => void;
}) {
    const { data: schema, isLoading, isError } = useResourceSchema(props.plugin.id);

    if (isLoading) {
        return (
            <Card>
                <CardContent className="space-y-4 pt-6">
                    <Skeleton className="h-10 w-full rounded" />
                    <Skeleton className="h-32 w-full rounded" />
                    <WizardFooter onBack={props.onBack} backLabel={props.backLabel} stepText={props.stepLabel('configure')} action={null} />
                </CardContent>
            </Card>
        );
    }

    if (isError || !schema) {
        return (
            <Card className="border-destructive/30">
                <CardContent className="space-y-4 pt-6">
                    <p className="text-sm text-destructive">Failed to load the configuration schema for this resource type.</p>
                    <WizardFooter onBack={props.onBack} backLabel={props.backLabel} stepText={props.stepLabel('configure')} action={null} />
                </CardContent>
            </Card>
        );
    }

    return <ResourceForm key={props.plugin.id} schema={schema as JsonSchema} {...props} />;
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiResourceWizardPage() {
    const { apiId, resourceName } = useParams<{ apiId: string; resourceName?: string }>();
    const navigate = useNavigate();
    const editMode = Boolean(resourceName);
    const decodedName = resourceName ? decodeURIComponent(resourceName) : undefined;

    const { data: api, isLoading } = useApiDetail(apiId);
    const { data: plugins = [], isLoading: pluginsLoading } = useResourcePlugins();
    const mutation = useUpdateApiResources(apiId);

    const resources = useMemo<ApiResource[]>(() => api?.resources ?? [], [api?.resources]);
    const existing = useMemo(() => (decodedName ? resources.find(r => r.name === decodedName) : undefined), [resources, decodedName]);

    const [selectedPlugin, setSelectedPlugin] = useState<ResourcePlugin | null>(null);
    const [step, setStep] = useState<WizardStep>(editMode ? 'configure' : 'select-type');

    // Edit mode: derive the plugin from the existing resource's type once plugins load.
    useEffect(() => {
        if (editMode && existing && plugins.length > 0) {
            setSelectedPlugin(prev => prev ?? plugins.find(p => p.id === existing.type) ?? null);
        }
    }, [editMode, existing, plugins]);

    const wizardSteps = useMemo<WizardStepDescriptor[]>(
        () =>
            editMode
                ? [
                      { id: 'configure', label: 'Configure' },
                      { id: 'review', label: 'Review & Save' },
                  ]
                : [
                      { id: 'select-type', label: 'Select Type' },
                      { id: 'configure', label: 'Configure' },
                      { id: 'review', label: 'Review & Create' },
                  ],
        [editMode],
    );
    const stepLabel = (stepId: WizardStep) => `Step ${wizardSteps.findIndex(s => s.id === stepId) + 1} of ${wizardSteps.length}`;

    const existingNames = useMemo(
        () => resources.filter(r => !editMode || r.name !== existing?.name).map(r => r.name),
        [resources, editMode, existing],
    );

    const goToList = () => navigate('..');

    const handleSubmit = (result: ConfigureReviewResult) => {
        if (!selectedPlugin) return;
        const resource: ApiResource = {
            name: result.name,
            type: selectedPlugin.id,
            enabled: existing?.enabled ?? true,
            configuration: result.configuration,
        };
        const next = editMode ? resources.map(r => (r.name === existing?.name ? resource : r)) : [...resources, resource];
        mutation.mutate(next, {
            onSuccess: () => {
                notify.success(editMode ? 'Resource updated.' : 'Resource created.');
                goToList();
            },
            onError: error => notify.error(error, 'Failed to save resource.'),
        });
    };

    if (isLoading) {
        return (
            <div className="space-y-6 p-6">
                <Skeleton className="h-8 w-56 rounded" />
                <Skeleton className="h-10 w-full rounded" />
                <Skeleton className="h-64 w-full rounded-xl" />
            </div>
        );
    }

    if (editMode && !existing) {
        return (
            <div className="space-y-4 p-6">
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

    return (
        <div className="space-y-6 p-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">{editMode ? 'Edit Resource' : 'Add Resource'}</h1>
                <p className="text-sm text-muted-foreground">
                    {editMode
                        ? 'Update the configuration of this API-level resource.'
                        : 'Create a resource specific to this API with its own configuration.'}
                </p>
            </div>

            <WizardStepIndicator
                steps={wizardSteps}
                currentStepId={step}
                onStepClick={id => setStep(id as WizardStep)}
                ariaLabel="Resource creation steps"
            />

            {step === 'select-type' ? (
                <SelectTypeStep
                    plugins={plugins}
                    pluginsLoading={pluginsLoading}
                    selectedId={selectedPlugin?.id}
                    onSelect={setSelectedPlugin}
                    onBack={goToList}
                    onNext={() => setStep('configure')}
                    stepText={stepLabel('select-type')}
                />
            ) : selectedPlugin ? (
                <ConfigureReviewStep
                    plugin={selectedPlugin}
                    step={step}
                    initial={existing}
                    existingNames={existingNames}
                    isSaving={mutation.isPending}
                    backLabel={editMode ? 'Back to Resources' : 'Back'}
                    stepLabel={stepLabel}
                    onBack={() => (editMode ? goToList() : setStep('select-type'))}
                    onStep={setStep}
                    onSubmit={handleSubmit}
                />
            ) : (
                <Skeleton className="h-64 w-full rounded-xl" />
            )}
        </div>
    );
}
