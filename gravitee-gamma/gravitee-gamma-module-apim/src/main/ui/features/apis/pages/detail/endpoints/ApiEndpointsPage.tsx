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
import { Alert, AlertDescription, Button, Skeleton, TooltipProvider } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';

import { EndpointForm } from './endpoint-form/EndpointForm';
import { EndpointGroupList } from './EndpointGroupList';
import { EndpointsLanding } from './EndpointsLanding';
import { EndpointGroupForm } from './group-form/EndpointGroupForm';
import type { EndpointFormState, EndpointGroupFormState } from './types';
import { buildDefaultEndpointForGroup, DEFAULT_GROUP_FORM, DEFAULT_SHARED_CONFIG, newEndpointRow, parseSharedConfigDto } from './types';
import { notify } from '../../../../../shared/notify';
import { useApiDetailContext } from '../../../context/ApiDetailContext';
import { updateApiEndpointGroups } from '../../../services/apis';
import type { EndpointGroupDto, EndpointGroupSharedConfiguration } from '../../../types';
import { isHttpProxyApi } from '../../../utils/apiHttpProxy';
import { serializeSharedConfiguration, serializeSharedConfigurationOverride } from '../../../utils/endpointSharedConfiguration';
import {
    buildEndpointHealthCheckService,
    buildGroupHealthCheckService,
    healthCheckFromEndpoint,
    healthCheckFromGroup,
} from '../../../utils/healthCheckForm';
import { apiDetailKeys } from '../../../utils/queryKeys';

// ─── DTO ↔ form conversion ────────────────────────────────────────────────────

function dtoToFormState(group: EndpointGroupDto): EndpointGroupFormState {
    const groupHealth = group.services?.healthCheck;
    return {
        name: group.name,
        loadBalancerType: group.loadBalancer?.type ?? 'ROUND_ROBIN',
        sharedConfig: parseSharedConfigDto(group.sharedConfiguration ?? {}),
        healthCheck: healthCheckFromGroup(groupHealth),
        endpoints: (group.endpoints ?? []).map(ep => ({
            _id: Math.random().toString(36).slice(2, 10),
            name: ep.name,
            target: ep.configuration?.target ?? '',
            weight: ep.weight ?? 1,
            backup: ep.backup ?? false,
            inheritConfiguration: ep.inheritConfiguration ?? true,
            tenants: ep.tenants ?? [],
            _originalDto: ep,
            _configOverride: ep.sharedConfigurationOverride
                ? parseSharedConfigDto(ep.sharedConfigurationOverride as EndpointGroupSharedConfiguration)
                : undefined,
            healthCheck: healthCheckFromEndpoint(ep.services?.healthCheck, groupHealth),
        })),
    };
}

function formStateToDto(form: EndpointGroupFormState, existingGroup?: EndpointGroupDto, includeHealthCheck = false): EndpointGroupDto {
    const healthCheckService = includeHealthCheck ? buildGroupHealthCheckService(form.healthCheck) : existingGroup?.services?.healthCheck;
    return {
        ...(existingGroup ?? {}),
        name: form.name,
        type: existingGroup?.type ?? 'http-proxy',
        loadBalancer: { type: form.loadBalancerType },
        services: includeHealthCheck
            ? {
                  ...(existingGroup?.services ?? {}),
                  healthCheck: healthCheckService,
              }
            : existingGroup?.services,
        sharedConfiguration: serializeSharedConfiguration(form.sharedConfig, existingGroup?.sharedConfiguration),
        endpoints: form.endpoints.map(ep => {
            const orig = ep._originalDto;
            return {
                ...(orig ?? {}),
                name: ep.name.trim(),
                type: orig?.type ?? 'http-proxy',
                weight: ep.weight,
                backup: ep.backup || undefined,
                inheritConfiguration: ep.inheritConfiguration,
                configuration: { ...(orig?.configuration ?? {}), target: ep.target },
                tenants: ep.tenants.length > 0 ? ep.tenants : undefined,
                sharedConfigurationOverride: ep.inheritConfiguration ? {} : (orig?.sharedConfigurationOverride ?? {}),
            };
        }),
    };
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Group + endpoint names across the API (classic `isEndpointNameUnique` parity). */
function getExistingEndpointResourceNames(groups: EndpointGroupDto[], excludeGroupIndex: number | null): string[] {
    return groups.flatMap((g, i) => {
        if (i === excludeGroupIndex) return [];
        return [g.name, ...(g.endpoints ?? []).map(e => e.name)];
    });
}

function getExistingEndpointNames(groups: EndpointGroupDto[], groupIdx: number, excludeEpIdx: number | null): string[] {
    return (groups[groupIdx]?.endpoints ?? []).filter((_, i) => i !== excludeEpIdx).map(e => e.name);
}

function endpointDtoToFormState(groups: EndpointGroupDto[], groupIdx: number, epIdx: number): EndpointFormState {
    const group = groups[groupIdx];
    const groupHealth = group?.services?.healthCheck;
    const ep = group?.endpoints?.[epIdx];
    if (!ep) return newEndpointRow(healthCheckFromGroup(groupHealth));
    return {
        _id: Math.random().toString(36).slice(2, 10),
        name: ep.name,
        target: ep.configuration?.target ?? '',
        weight: ep.weight ?? 1,
        backup: ep.backup ?? false,
        inheritConfiguration: ep.inheritConfiguration ?? true,
        tenants: ep.tenants ?? [],
        _originalDto: ep,
        _configOverride: ep.sharedConfigurationOverride
            ? parseSharedConfigDto(ep.sharedConfigurationOverride as EndpointGroupSharedConfiguration)
            : undefined,
        healthCheck: healthCheckFromEndpoint(ep.services?.healthCheck, groupHealth),
    };
}

// ─── Form modes ───────────────────────────────────────────────────────────────

type GroupFormMode = 'hidden' | 'add' | 'edit';
type EndpointFormMode = 'hidden' | 'add' | 'edit';
type GroupFormStep = 'general' | 'configuration' | 'health-check';

// ─── Page ─────────────────────────────────────────────────────────────────────

export function ApiEndpointsPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const [searchParams, setSearchParams] = useSearchParams();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const { api, isLoading } = useApiDetailContext();

    const canEdit = useHasPermission({ anyOf: ['api-definition-u'] });
    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const isReadOnly = !canEdit || isKubernetesManaged;

    // Group form state
    const [groupFormMode, setGroupFormMode] = useState<GroupFormMode>('hidden');
    const [editingGroupIndex, setEditingGroupIndex] = useState<number | null>(null);
    const [groupFormInitialStep, setGroupFormInitialStep] = useState<GroupFormStep>('general');
    const [consumedDeepLink, setConsumedDeepLink] = useState<string | null>(null);

    // Endpoint form state
    const [endpointFormMode, setEndpointFormMode] = useState<EndpointFormMode>('hidden');
    const [endpointGroupIndex, setEndpointGroupIndex] = useState<number | null>(null);
    const [endpointIndex, setEndpointIndex] = useState<number | null>(null);
    const [endpointInitialForm, setEndpointInitialForm] = useState<EndpointFormState | undefined>(undefined);

    const [saveError, setSaveError] = useState<string | null>(null);

    const groups: EndpointGroupDto[] = api?.endpointGroups ?? [];
    const hasGroups = groups.length > 0;
    const httpProxyApi = isHttpProxyApi(api);

    const editGroupParam = searchParams.get('editGroup');
    const stepParam = searchParams.get('step');
    const deepLinkKey = editGroupParam !== null || stepParam !== null ? `${editGroupParam ?? ''}|${stepParam ?? ''}` : null;

    // Apply checklist/overview deep links during render once API data is ready (avoids setState in useEffect).
    if (!isLoading && deepLinkKey !== null && consumedDeepLink !== deepLinkKey) {
        setConsumedDeepLink(deepLinkKey);

        const groupIndex = editGroupParam === '0' || editGroupParam === 'default' ? 0 : Number.parseInt(editGroupParam ?? '', 10);

        if (Number.isFinite(groupIndex) && groupIndex >= 0 && groupIndex < groups.length) {
            setEditingGroupIndex(groupIndex);
            setGroupFormMode('edit');
            setGroupFormInitialStep(stepParam === 'configuration' ? 'configuration' : 'general');
        }
    }

    useEffect(() => {
        if (consumedDeepLink !== null && deepLinkKey !== null) {
            setSearchParams({}, { replace: true });
        }
    }, [consumedDeepLink, deepLinkKey, setSearchParams]);

    const mutation = useMutation({
        mutationFn: (updated: EndpointGroupDto[]) => updateApiEndpointGroups(env!.id, apiId!, updated),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            setSaveError(null);
            setGroupFormMode('hidden');
            setEditingGroupIndex(null);
            setEndpointFormMode('hidden');
            setEndpointGroupIndex(null);
            setEndpointIndex(null);
            notify.success('Endpoints updated');
        },
        onError: (err: unknown) => {
            // Group/endpoint form views surface this inline via `saveError`.
            // List-view actions (delete/reorder) additionally pass their own toast `onError`.
            setSaveError(err instanceof Error ? err.message : 'Failed to save.');
        },
    });

    // ── Group form handlers ──
    function openAddGroup() {
        mutation.reset();
        setSaveError(null);
        setEditingGroupIndex(null);
        setGroupFormInitialStep('general');
        setGroupFormMode('add');
    }

    function buildAddGroupInitialForm(): EndpointGroupFormState {
        return {
            ...DEFAULT_GROUP_FORM,
            sharedConfig: { ...DEFAULT_SHARED_CONFIG },
        };
    }

    function openEditGroup(idx: number) {
        mutation.reset();
        setSaveError(null);
        setEditingGroupIndex(idx);
        setGroupFormInitialStep('general');
        setGroupFormMode('edit');
    }

    function handleGroupCancel() {
        setGroupFormMode('hidden');
        setEditingGroupIndex(null);
        setGroupFormInitialStep('general');
        setSaveError(null);
    }

    function handleGroupSave(form: EndpointGroupFormState) {
        const trimmedForm = { ...form, name: form.name.trim() };
        const updated = [...groups];
        if (groupFormMode === 'add') {
            const groupType = 'http-proxy';
            const target = trimmedForm.defaultEndpointTarget?.trim() ?? '';
            updated.push({
                ...formStateToDto(trimmedForm, undefined, httpProxyApi),
                endpoints: [buildDefaultEndpointForGroup(trimmedForm.name, target, groupType)],
            });
        } else if (groupFormMode === 'edit' && editingGroupIndex !== null) {
            // Preserve existing endpoints — group form no longer manages them
            const existing = groups[editingGroupIndex];
            updated[editingGroupIndex] = {
                ...formStateToDto(trimmedForm, existing, httpProxyApi),
                endpoints: existing.endpoints ?? [],
            };
        }
        mutation.mutate(updated);
    }

    // ── Endpoint form handlers ──
    function openAddEndpoint(groupIdx: number) {
        mutation.reset();
        setSaveError(null);
        setEndpointGroupIndex(groupIdx);
        setEndpointIndex(null);
        setEndpointInitialForm(undefined);
        setEndpointFormMode('add');
    }

    function openEditEndpoint(groupIdx: number, epIdx: number) {
        mutation.reset();
        setSaveError(null);
        setEndpointGroupIndex(groupIdx);
        setEndpointIndex(epIdx);
        setEndpointInitialForm(endpointDtoToFormState(groups, groupIdx, epIdx));
        setEndpointFormMode('edit');
    }

    function handleEndpointCancel() {
        setEndpointFormMode('hidden');
        setEndpointGroupIndex(null);
        setEndpointIndex(null);
        setSaveError(null);
    }

    function handleEndpointSave(ep: EndpointFormState) {
        if (endpointGroupIndex === null || mutation.isPending) return;
        const updated = groups.map((g, gIdx) => {
            if (gIdx !== endpointGroupIndex) return g;
            const endpoints = [...(g.endpoints ?? [])];
            const orig = ep._originalDto;
            const newEp = {
                ...(orig ?? {}),
                name: ep.name.trim(),
                type: orig?.type ?? 'http-proxy',
                weight: ep.weight,
                backup: ep.backup || undefined,
                inheritConfiguration: ep.inheritConfiguration,
                configuration: { ...(orig?.configuration ?? {}), target: ep.target },
                tenants: ep.tenants.length > 0 ? ep.tenants : undefined,
                sharedConfigurationOverride: ep.inheritConfiguration ? {} : serializeSharedConfigurationOverride(ep._configOverride),
                ...(httpProxyApi
                    ? {
                          services: {
                              ...(orig?.services ?? {}),
                              healthCheck: buildEndpointHealthCheckService(ep.healthCheck),
                          },
                      }
                    : {}),
            };
            if (endpointFormMode === 'add') {
                endpoints.push(newEp);
            } else if (endpointFormMode === 'edit' && endpointIndex !== null) {
                endpoints[endpointIndex] = newEp;
            }
            return { ...g, endpoints };
        });
        mutation.mutate(updated);
    }

    function handleDeleteGroup(idx: number) {
        mutation.mutate(
            groups.filter((_, i) => i !== idx),
            { onError: error => notify.error(error, 'Failed to delete endpoint group.') },
        );
    }

    function handleDeleteEndpoint(groupIdx: number, epIdx: number) {
        const updated = groups.map((g, i) => (i === groupIdx ? { ...g, endpoints: (g.endpoints ?? []).filter((_, j) => j !== epIdx) } : g));
        mutation.mutate(updated, { onError: error => notify.error(error, 'Failed to delete endpoint.') });
    }

    function handleReorderEndpoints(groupIdx: number, fromIdx: number, toIdx: number) {
        const updated = groups.map((g, i) => {
            if (i !== groupIdx) return g;
            const endpoints = [...(g.endpoints ?? [])];
            const [moved] = endpoints.splice(fromIdx, 1);
            endpoints.splice(toIdx, 0, moved);
            return { ...g, endpoints };
        });
        mutation.mutate(updated, { onError: error => notify.error(error, 'Failed to reorder endpoints.') });
    }

    // ── Loading ──
    if (isLoading) {
        return (
            <div className="space-y-6 p-6">
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-2">
                        <Skeleton className="h-7 w-36 rounded" />
                        <Skeleton className="h-4 w-72 rounded" />
                    </div>
                    <Skeleton className="h-9 w-40 rounded-md" />
                </div>
                <Skeleton className="h-48 w-full rounded-xl" />
            </div>
        );
    }

    const showGroupForm = groupFormMode !== 'hidden';
    const showEndpointForm = endpointFormMode !== 'hidden';
    const showList = !showGroupForm && !showEndpointForm;

    // Derived name sets for validation
    const existingGroupNames = getExistingEndpointResourceNames(groups, groupFormMode === 'edit' ? editingGroupIndex : null);

    const groupInitialForm: EndpointGroupFormState =
        groupFormMode === 'edit' && editingGroupIndex !== null ? dtoToFormState(groups[editingGroupIndex]) : buildAddGroupInitialForm();

    // Endpoint form data
    const endpointExistingNames =
        endpointGroupIndex !== null
            ? getExistingEndpointNames(groups, endpointGroupIndex, endpointFormMode === 'edit' ? endpointIndex : null)
            : [];

    return (
        <TooltipProvider>
            <div className="space-y-6 p-6">
                {/* ── Page header ── */}
                <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1">
                        <h1 className="text-2xl font-semibold tracking-tight">
                            {showGroupForm
                                ? groupFormMode === 'add'
                                    ? 'Add endpoint group'
                                    : editingGroupIndex === 0 && groupFormInitialStep === 'configuration'
                                      ? 'Default endpoint group configuration'
                                      : editingGroupIndex === 0
                                        ? 'Edit default endpoint group'
                                        : 'Edit endpoint group'
                                : showEndpointForm
                                  ? endpointFormMode === 'add'
                                      ? 'Add endpoint'
                                      : 'Edit endpoint'
                                  : 'Endpoints'}
                        </h1>
                        <p className="text-sm text-muted-foreground">
                            {showGroupForm
                                ? 'Configure the endpoint group and its upstream connections.'
                                : showEndpointForm
                                  ? 'Configure the endpoint and its upstream connection settings.'
                                  : 'Define the protocol and configuration settings by which the Gateway API will fetch data from, or post data to, the backend API'}
                        </p>
                    </div>

                    {showList && !isReadOnly && (
                        <Button size="sm" className="gap-1.5 shrink-0" onClick={openAddGroup}>
                            <PlusIcon className="size-3.5" aria-hidden />
                            Add endpoint group
                        </Button>
                    )}
                </div>

                {/* ── Kubernetes read-only banner ── */}
                {isKubernetesManaged && (
                    <Alert>
                        <AlertDescription>
                            This API is managed by the Kubernetes operator. Endpoint configuration is read-only.
                        </AlertDescription>
                    </Alert>
                )}

                {/* ── Group form view ── */}
                {showGroupForm && (
                    <EndpointGroupForm
                        key={`${groupFormMode}-${editingGroupIndex ?? 'new'}-${groupFormInitialStep}`}
                        initialForm={groupInitialForm}
                        existingGroupNames={existingGroupNames}
                        initialStep={groupFormInitialStep}
                        showHealthCheck={httpProxyApi}
                        isCreateMode={groupFormMode === 'add' && httpProxyApi}
                        isReadOnly={isReadOnly}
                        isSaving={mutation.isPending}
                        saveError={saveError}
                        onSave={handleGroupSave}
                        onCancel={handleGroupCancel}
                    />
                )}

                {/* ── Endpoint form view (inline) ── */}
                {showEndpointForm && (
                    <>
                        {mutation.isError && (
                            <Alert variant="destructive">
                                <AlertDescription>{saveError ?? 'An error occurred.'}</AlertDescription>
                            </Alert>
                        )}
                        <EndpointForm
                            key={`ep-${endpointFormMode}-g${endpointGroupIndex ?? 0}-e${endpointIndex ?? 'new'}`}
                            isEdit={endpointFormMode === 'edit'}
                            initial={endpointInitialForm}
                            existingNames={endpointExistingNames}
                            showHealthCheck={httpProxyApi}
                            groupHealthCheck={endpointGroupIndex !== null ? groupInitialForm.healthCheck : undefined}
                            isReadOnly={isReadOnly}
                            isSaving={mutation.isPending}
                            onSave={handleEndpointSave}
                            onCancel={handleEndpointCancel}
                        />
                    </>
                )}

                {/* ── List / landing view ── */}
                {showList && (
                    <>
                        {!hasGroups && <EndpointsLanding />}
                        {hasGroups && (
                            <EndpointGroupList
                                groups={groups}
                                isReadOnly={isReadOnly}
                                onEditGroup={openEditGroup}
                                onDeleteGroup={handleDeleteGroup}
                                onAddEndpoint={openAddEndpoint}
                                onEditEndpoint={openEditEndpoint}
                                onDeleteEndpoint={handleDeleteEndpoint}
                                onReorderEndpoints={handleReorderEndpoints}
                            />
                        )}
                    </>
                )}
            </div>
        </TooltipProvider>
    );
}
