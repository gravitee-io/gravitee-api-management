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

import { Button, Skeleton } from '@gravitee/graphene-core';
import {
    ArrowLeftIcon,
    BookOpenIcon,
    CalendarIcon,
    CircleStopIcon,
    ClockIcon,
    CloudUploadIcon,
    PlayIcon,
    PlusIcon,
} from '@gravitee/graphene-core/icons';
import { useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { AddDictionaryPropertySheet } from '../features/dictionaries/components/AddDictionaryPropertySheet';
import { DeleteDictionaryPropertySheet } from '../features/dictionaries/components/DeleteDictionaryPropertySheet';
import { DictionaryPropertiesTable } from '../features/dictionaries/components/DictionaryPropertiesTable';
import { DictionaryStateBadge } from '../features/dictionaries/components/DictionaryStateBadge';
import { TRIGGER_UNIT_OPTIONS } from '../features/dictionaries/components/DictionaryTriggerFields';
import { DictionaryTypeBadge } from '../features/dictionaries/components/DictionaryTypeBadge';
import { EditDictionaryPropertySheet } from '../features/dictionaries/components/EditDictionaryPropertySheet';
import {
    useDeployDictionary,
    useStartDictionary,
    useStopDictionary,
    useUpdateDictionary,
} from '../features/dictionaries/hooks/useDictionaryMutations';
import { useDictionaryPermissions } from '../features/dictionaries/hooks/useDictionaryPermissions';
import { useEnvironmentDictionary } from '../features/dictionaries/hooks/useEnvironmentDictionary';
import type { Dictionary, DictionaryHttpProviderConfiguration, UpdateDictionaryPayload } from '../features/dictionaries/types/dictionary';
import { formatDictionaryDate } from '../features/dictionaries/utils/formatDictionaryDate';
import { notify } from '../shared/notify';

function triggerUnitLabel(unit: string | undefined): string {
    if (!unit) return '';
    return TRIGGER_UNIT_OPTIONS.find(option => option.value === unit)?.label.toLowerCase() ?? unit.toLowerCase();
}

function asHttpProviderConfig(configuration: unknown): DictionaryHttpProviderConfiguration {
    if (configuration && typeof configuration === 'object') {
        return configuration as DictionaryHttpProviderConfiguration;
    }
    return {};
}

function toUpdatePayload(dictionary: Dictionary, nextProperties: Record<string, string>): UpdateDictionaryPayload {
    return {
        name: dictionary.name,
        description: dictionary.description,
        type: dictionary.type,
        properties: nextProperties,
        provider: dictionary.provider ?? undefined,
        trigger: dictionary.trigger ?? undefined,
    };
}

export function DictionaryDetailPage() {
    const { dictionaryId } = useParams<{ dictionaryId: string }>();
    const navigate = useNavigate();
    const { canUpdate } = useDictionaryPermissions();
    const { data: dictionary, isLoading, isError } = useEnvironmentDictionary(dictionaryId);
    const updateMutation = useUpdateDictionary();
    const deployMutation = useDeployDictionary();
    const startMutation = useStartDictionary();
    const stopMutation = useStopDictionary();
    const [addPropertyOpen, setAddPropertyOpen] = useState(false);
    const [propertyToEdit, setPropertyToEdit] = useState<{ key: string; value: string } | undefined>();
    const [propertyToDelete, setPropertyToDelete] = useState<string | undefined>();

    const properties = dictionary?.properties;
    const propertyRows = useMemo(() => Object.entries(properties ?? {}).map(([key, value]) => ({ key, value })), [properties]);
    const propertyCount = propertyRows.length;
    const isDynamic = dictionary?.type === 'DYNAMIC';
    // Classic Console gates property add/edit/delete with environment-dictionary-u.
    const canEditManualProperties = canUpdate && !isDynamic;
    const isStarted = dictionary?.state === 'STARTED';
    const lifecyclePending = startMutation.isPending || stopMutation.isPending;
    const providerConfig = asHttpProviderConfig(dictionary?.provider?.configuration);
    const providerHeaders = (providerConfig.headers ?? []).filter(header => header.name?.trim() || header.value?.trim());
    const providerBody = providerConfig.body?.trim() ?? '';
    const providerSpecification = providerConfig.specification?.trim() ?? '';

    async function saveProperties(nextProperties: Record<string, string>, successMessage: string) {
        if (!dictionary || dictionary.type !== 'MANUAL') return;
        await updateMutation.mutateAsync({
            dictionaryId: dictionary.id,
            data: toUpdatePayload(dictionary, nextProperties),
        });
        notify.success(successMessage);
    }

    async function handleAddProperty(property: { key: string; value: string }) {
        try {
            await saveProperties({ ...(properties ?? {}), [property.key]: property.value }, 'Property added successfully');
            setAddPropertyOpen(false);
        } catch (error) {
            notify.error(error, 'Failed to add property');
            throw error;
        }
    }

    async function handleEditProperty(next: { originalKey: string; key: string; value: string }) {
        const updated = { ...(properties ?? {}) };
        if (next.key !== next.originalKey) {
            delete updated[next.originalKey];
        }
        updated[next.key] = next.value;
        try {
            await saveProperties(updated, 'Property updated successfully');
            setPropertyToEdit(undefined);
        } catch (error) {
            notify.error(error, 'Failed to update property');
            throw error;
        }
    }

    async function handleDeleteProperty() {
        if (!propertyToDelete) return;
        const updated = { ...(properties ?? {}) };
        delete updated[propertyToDelete];
        try {
            await saveProperties(updated, 'Property deleted successfully');
            setPropertyToDelete(undefined);
        } catch (error) {
            notify.error(error, 'Failed to delete property');
        }
    }

    async function handleDeploy() {
        if (!dictionary) return;
        try {
            await deployMutation.mutateAsync(dictionary.id);
            notify.success('Dictionary deployed successfully');
        } catch (error) {
            notify.error(error, 'Failed to deploy dictionary');
        }
    }

    async function handleStart() {
        if (!dictionary) return;
        try {
            await startMutation.mutateAsync(dictionary.id);
            notify.success('Dictionary started successfully');
        } catch (error) {
            notify.error(error, 'Failed to start dictionary');
        }
    }

    async function handleStop() {
        if (!dictionary) return;
        try {
            await stopMutation.mutateAsync(dictionary.id);
            notify.success('Dictionary stopped successfully');
        } catch (error) {
            notify.error(error, 'Failed to stop dictionary');
        }
    }

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-8 w-48" />
                <Skeleton className="h-40 w-full rounded-xl" />
                <Skeleton className="h-56 w-full rounded-xl" />
            </div>
        );
    }

    if (isError || !dictionary) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Dictionaries
                </Button>
                <p className="text-sm text-muted-foreground">Dictionary not found or failed to load.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                <Link to="..">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to Dictionaries
                </Link>
            </Button>

            <section className="rounded-xl border bg-card p-5">
                <div className="flex items-start justify-between gap-4">
                    <div className="flex min-w-0 items-start gap-3">
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-orange-100 text-orange-700">
                            <BookOpenIcon className="size-5" aria-hidden />
                        </div>
                        <div className="min-w-0 space-y-3">
                            <div className="space-y-1">
                                <div className="flex flex-wrap items-center gap-2">
                                    <h1 className="text-xl font-semibold tracking-tight">{dictionary.name}</h1>
                                    <DictionaryTypeBadge type={dictionary.type} />
                                    {isDynamic ? <DictionaryStateBadge state={dictionary.state} /> : null}
                                </div>
                                {dictionary.description?.trim() ? (
                                    <p className="text-sm text-muted-foreground">{dictionary.description}</p>
                                ) : null}
                            </div>
                            <div className="inline-flex items-start gap-6">
                                <div className="inline-flex shrink-0 flex-col gap-1">
                                    <span className="text-xs text-muted-foreground">Properties</span>
                                    <span className="text-sm font-medium text-foreground">
                                        {propertyCount} {propertyCount === 1 ? 'entry' : 'entries'}
                                    </span>
                                </div>
                                <div className="inline-flex shrink-0 flex-col gap-1">
                                    <span className="text-xs text-muted-foreground">Last Updated</span>
                                    <span className="inline-flex items-center gap-1.5 text-sm text-foreground">
                                        <ClockIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                        {formatDictionaryDate(dictionary.updated_at)}
                                    </span>
                                </div>
                                <div className="inline-flex shrink-0 flex-col gap-1">
                                    <span className="text-xs text-muted-foreground">Last Deployed</span>
                                    <span className="inline-flex items-center gap-1.5 text-sm text-foreground">
                                        <CalendarIcon className="size-4 shrink-0 text-muted-foreground" aria-hidden />
                                        {formatDictionaryDate(dictionary.deployed_at)}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                    {canUpdate ? (
                        <div className="flex shrink-0 items-center gap-2">
                            {isDynamic ? (
                                isStarted ? (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        className="gap-1.5"
                                        onClick={handleStop}
                                        disabled={lifecyclePending}
                                    >
                                        <CircleStopIcon className="size-4" aria-hidden />
                                        {stopMutation.isPending ? 'Stopping…' : 'Stop'}
                                    </Button>
                                ) : (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        className="gap-1.5"
                                        onClick={handleStart}
                                        disabled={lifecyclePending}
                                    >
                                        <PlayIcon className="size-4" aria-hidden />
                                        {startMutation.isPending ? 'Starting…' : 'Start'}
                                    </Button>
                                )
                            ) : (
                                <Button
                                    type="button"
                                    variant="outline"
                                    className="gap-1.5"
                                    onClick={handleDeploy}
                                    disabled={deployMutation.isPending}
                                >
                                    <CloudUploadIcon className="size-4" aria-hidden />
                                    {deployMutation.isPending ? 'Deploying…' : 'Deploy'}
                                </Button>
                            )}
                        </div>
                    ) : null}
                </div>
            </section>

            {isDynamic ? (
                <section className="space-y-4 rounded-xl border bg-card p-5">
                    <div>
                        <h2 className="text-base font-semibold">Trigger & Provider</h2>
                        <p className="text-sm text-muted-foreground">Polling schedule and HTTP source used to refresh properties.</p>
                    </div>
                    <div className="grid gap-4 sm:grid-cols-2">
                        <div className="space-y-1">
                            <div className="text-xs text-muted-foreground">Polling Interval</div>
                            <div className="text-sm font-medium">
                                {dictionary.trigger ? `Every ${dictionary.trigger.rate} ${triggerUnitLabel(dictionary.trigger.unit)}` : '—'}
                            </div>
                        </div>
                        <div className="space-y-1">
                            <div className="text-xs text-muted-foreground">Provider Type</div>
                            <div className="text-sm font-medium">
                                {dictionary.provider?.type ? `Custom (${dictionary.provider.type})` : '—'}
                            </div>
                        </div>
                        <div className="space-y-1 sm:col-span-2">
                            <div className="text-xs text-muted-foreground">HTTP Service URL</div>
                            <div className="flex items-center gap-2 rounded-md bg-muted px-3 py-2 font-mono text-xs">
                                {providerConfig.method ? (
                                    <span className="shrink-0 font-semibold text-foreground">{providerConfig.method}</span>
                                ) : null}
                                <span className="truncate">{providerConfig.url?.trim() || '—'}</span>
                            </div>
                        </div>
                        <div className="space-y-1">
                            <div className="text-xs text-muted-foreground">Use system proxy</div>
                            <div className="text-sm font-medium">{providerConfig.useSystemProxy ? 'Yes' : 'No'}</div>
                        </div>
                        <div className="space-y-1">
                            <div className="text-xs text-muted-foreground">Headers</div>
                            {providerHeaders.length > 0 ? (
                                <ul className="space-y-1 text-sm">
                                    {providerHeaders.map((header, index) => (
                                        <li key={`${header.name ?? ''}-${index}`} className="font-mono text-xs">
                                            <span className="font-semibold text-foreground">{header.name?.trim() || '—'}</span>
                                            <span className="text-muted-foreground">: {header.value?.trim() || '—'}</span>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <div className="text-sm text-muted-foreground">None</div>
                            )}
                        </div>
                        <div className="space-y-1 sm:col-span-2">
                            <div className="text-xs text-muted-foreground">Request body</div>
                            {providerBody ? (
                                <pre className="max-h-40 overflow-auto whitespace-pre-wrap rounded-md bg-muted px-3 py-2 font-mono text-xs">
                                    {providerBody}
                                </pre>
                            ) : (
                                <div className="text-sm text-muted-foreground">None</div>
                            )}
                        </div>
                        <div className="space-y-1 sm:col-span-2">
                            <div className="text-xs text-muted-foreground">JOLT specification</div>
                            {providerSpecification ? (
                                <pre className="max-h-48 overflow-auto whitespace-pre-wrap rounded-md bg-muted px-3 py-2 font-mono text-xs">
                                    {providerSpecification}
                                </pre>
                            ) : (
                                <div className="text-sm text-muted-foreground">—</div>
                            )}
                        </div>
                    </div>
                </section>
            ) : null}

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <div className="flex items-start justify-between gap-4">
                    <div>
                        <h2 className="text-base font-semibold">Properties</h2>
                        <p className="text-sm text-muted-foreground">
                            {isDynamic
                                ? 'Properties are populated automatically by the provider.'
                                : 'Key/value entries that make up this dictionary.'}
                        </p>
                    </div>
                    {canEditManualProperties ? (
                        <Button type="button" className="shrink-0 gap-1.5" onClick={() => setAddPropertyOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden />
                            Add Property
                        </Button>
                    ) : null}
                </div>

                <DictionaryPropertiesTable
                    properties={propertyRows}
                    canEdit={canEditManualProperties}
                    isMutating={updateMutation.isPending}
                    onEdit={setPropertyToEdit}
                    onDelete={setPropertyToDelete}
                    emptyMessage={
                        isDynamic
                            ? 'Properties will be populated once the dictionary is started.'
                            : 'No properties yet. Add key/value entries to this dictionary.'
                    }
                />
            </section>

            <AddDictionaryPropertySheet
                open={addPropertyOpen}
                onClose={() => setAddPropertyOpen(false)}
                onSubmit={handleAddProperty}
                isSaving={updateMutation.isPending}
                existingKeys={Object.keys(properties ?? {})}
            />

            <EditDictionaryPropertySheet
                open={propertyToEdit !== undefined}
                property={propertyToEdit}
                onClose={() => setPropertyToEdit(undefined)}
                onSubmit={handleEditProperty}
                isSaving={updateMutation.isPending}
                existingKeys={Object.keys(properties ?? {})}
            />

            <DeleteDictionaryPropertySheet
                open={propertyToDelete !== undefined}
                propertyKey={propertyToDelete}
                onClose={() => setPropertyToDelete(undefined)}
                onConfirm={() => void handleDeleteProperty()}
                isDeleting={updateMutation.isPending}
            />
        </div>
    );
}
