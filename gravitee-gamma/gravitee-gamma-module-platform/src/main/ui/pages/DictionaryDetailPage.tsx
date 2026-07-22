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
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { AddDictionaryPropertySheet } from '../features/dictionaries/components/AddDictionaryPropertySheet';
import { DictionaryStateBadge } from '../features/dictionaries/components/DictionaryStateBadge';
import { DictionaryTypeBadge } from '../features/dictionaries/components/DictionaryTypeBadge';
import { TRIGGER_UNIT_OPTIONS } from '../features/dictionaries/components/DictionaryTriggerFields';
import {
    useDeployDictionary,
    useStartDictionary,
    useStopDictionary,
    useUpdateDictionary,
} from '../features/dictionaries/hooks/useDictionaryMutations';
import { useDictionaryPermissions } from '../features/dictionaries/hooks/useDictionaryPermissions';
import { useEnvironmentDictionary } from '../features/dictionaries/hooks/useEnvironmentDictionary';
import type { DictionaryHttpProviderConfiguration } from '../features/dictionaries/types/dictionary';
import { formatDictionaryDate } from '../features/dictionaries/utils/formatDictionaryDate';
import { dictionaryKeys } from '../features/dictionaries/utils/queryKeys';
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

export function DictionaryDetailPage() {
    const { dictionaryId } = useParams<{ dictionaryId: string }>();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const { canUpdate } = useDictionaryPermissions();
    const { data: dictionary, isLoading, isError } = useEnvironmentDictionary(dictionaryId);
    const updateMutation = useUpdateDictionary();
    const deployMutation = useDeployDictionary();
    const startMutation = useStartDictionary();
    const stopMutation = useStopDictionary();
    const [addPropertyOpen, setAddPropertyOpen] = useState(false);

    const properties = dictionary?.properties ?? {};
    const propertyEntries = Object.entries(properties);
    const propertyCount = propertyEntries.length;
    const isDynamic = dictionary?.type === 'DYNAMIC';
    const isStarted = dictionary?.state === 'STARTED';
    const lifecyclePending = startMutation.isPending || stopMutation.isPending;
    const providerConfig = asHttpProviderConfig(dictionary?.provider?.configuration);

    async function invalidateDetail() {
        if (!env?.id || !dictionaryId) return;
        await queryClient.invalidateQueries({ queryKey: dictionaryKeys.detail(env.id, dictionaryId) });
        await queryClient.invalidateQueries({ queryKey: dictionaryKeys.list(env.id) });
    }

    async function handleAddProperty(property: { key: string; value: string }) {
        if (!dictionary) return;
        try {
            await updateMutation.mutateAsync({
                dictionaryId: dictionary.id,
                data: {
                    name: dictionary.name,
                    description: dictionary.description,
                    type: dictionary.type,
                    properties: { ...properties, [property.key]: property.value },
                    provider: dictionary.provider ?? undefined,
                    trigger: dictionary.trigger ?? undefined,
                },
            });
            await invalidateDetail();
            notify.success('Property added successfully');
            setAddPropertyOpen(false);
        } catch (error) {
            notify.error(error, 'Failed to add property');
            throw error;
        }
    }

    async function handleDeploy() {
        if (!dictionary) return;
        try {
            await deployMutation.mutateAsync(dictionary.id);
            await invalidateDetail();
            notify.success('Dictionary deployed successfully');
        } catch (error) {
            notify.error(error, 'Failed to deploy dictionary');
        }
    }

    async function handleStart() {
        if (!dictionary) return;
        try {
            await startMutation.mutateAsync(dictionary.id);
            await invalidateDetail();
            notify.success('Dictionary started successfully');
        } catch (error) {
            notify.error(error, 'Failed to start dictionary');
        }
    }

    async function handleStop() {
        if (!dictionary) return;
        try {
            await stopMutation.mutateAsync(dictionary.id);
            await invalidateDetail();
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
                            <div className="grid gap-4 sm:grid-cols-2">
                                <div className="space-y-1">
                                    <div className="text-xs text-muted-foreground">Dictionary Key</div>
                                    <div className="inline-flex rounded-md bg-muted px-2 py-1 font-mono text-xs">
                                        {dictionary.key || dictionary.id}
                                    </div>
                                </div>
                                <div className="space-y-1">
                                    <div className="text-xs text-muted-foreground">Properties</div>
                                    <div className="text-sm font-medium">
                                        {propertyCount} {propertyCount === 1 ? 'entry' : 'entries'}
                                    </div>
                                </div>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <ClockIcon className="size-4 shrink-0" aria-hidden />
                                    <span>
                                        <span className="text-xs">Last Updated</span>
                                        <div className="text-foreground">{formatDictionaryDate(dictionary.updated_at)}</div>
                                    </span>
                                </div>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <CalendarIcon className="size-4 shrink-0" aria-hidden />
                                    <span>
                                        <span className="text-xs">Last Deployed</span>
                                        <div className="text-foreground">{formatDictionaryDate(dictionary.deployed_at)}</div>
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
                    </div>
                    <div className="space-y-1">
                        <div className="text-xs text-muted-foreground">HTTP Service URL</div>
                        <div className="flex items-center gap-2 rounded-md bg-muted px-3 py-2 font-mono text-xs">
                            {providerConfig.method ? (
                                <span className="shrink-0 font-semibold text-foreground">{providerConfig.method}</span>
                            ) : null}
                            <span className="truncate">{providerConfig.url?.trim() || '—'}</span>
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
                    {canUpdate && !isDynamic ? (
                        <Button type="button" className="shrink-0 gap-1.5" onClick={() => setAddPropertyOpen(true)}>
                            <PlusIcon className="size-4" aria-hidden />
                            Add Property
                        </Button>
                    ) : null}
                </div>

                {propertyCount === 0 ? (
                    <div className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed px-6 py-16 text-center">
                        <BookOpenIcon className="size-10 text-muted-foreground/40" aria-hidden />
                        <p className="text-sm text-muted-foreground">
                            {isDynamic
                                ? 'Properties will be populated once the dictionary is started.'
                                : 'No properties yet. Add key/value entries to this dictionary.'}
                        </p>
                    </div>
                ) : (
                    <div className="overflow-hidden rounded-lg border">
                        <table className="w-full text-sm">
                            <thead className="bg-muted/50 text-left text-muted-foreground">
                                <tr>
                                    <th className="px-4 py-2 font-medium">Key</th>
                                    <th className="px-4 py-2 font-medium">Value</th>
                                </tr>
                            </thead>
                            <tbody>
                                {propertyEntries.map(([key, value]) => (
                                    <tr key={key} className="border-t">
                                        <td className="px-4 py-2 font-mono text-xs">{key}</td>
                                        <td className="px-4 py-2">{value}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <AddDictionaryPropertySheet
                open={addPropertyOpen}
                onClose={() => setAddPropertyOpen(false)}
                onSubmit={handleAddProperty}
                isSaving={updateMutation.isPending}
                existingKeys={Object.keys(properties)}
            />
        </div>
    );
}
