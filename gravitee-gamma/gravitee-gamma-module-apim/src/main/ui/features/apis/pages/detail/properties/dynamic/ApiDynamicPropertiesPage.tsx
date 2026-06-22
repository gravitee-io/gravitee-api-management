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
import { Alert, AlertDescription, Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

import { DynamicPropertiesForm } from './DynamicPropertiesForm';
import type { DynamicPropertiesFormState } from './types';
import { DEFAULT_FORM, fromDtoToFormState, fromFormStateToDto } from './types';
import { useApiDetailContext } from '../../../../context/ApiDetailContext';
import { updateDynamicProperties } from '../../../../services/apis';
import { apiDetailKeys } from '../../../../utils/queryKeys';

export function ApiDynamicPropertiesPage() {
    const { apiId } = useParams<{ apiId: string }>();
    const navigate = useNavigate();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const { api, isLoading, permissionsReady } = useApiDetailContext();

    const canEdit = useHasPermission({ allOf: ['api-definition-u'] });
    const isKubernetesManaged = api?.definitionContext?.origin === 'KUBERNETES';
    const isReadOnly = !permissionsReady || !canEdit || isKubernetesManaged;

    const mutation = useMutation({
        mutationFn: (state: DynamicPropertiesFormState) => updateDynamicProperties(env!.id, apiId!, fromFormStateToDto(state)),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
        },
    });

    // Stable reference: only re-derived when the DTO object reference changes (i.e. after a
    // successful save triggers invalidation + refetch). Intermediate parent re-renders that
    // don't change the DTO will not reset the form.
    const dynamicPropertyDto = api?.services?.dynamicProperty;
    const initialState = useMemo(() => (dynamicPropertyDto ? fromDtoToFormState(dynamicPropertyDto) : DEFAULT_FORM), [dynamicPropertyDto]);

    // ─── Loading skeleton ──────────────────────────────────────────────────────
    if (isLoading) {
        return (
            <div className="space-y-6">
                <div className="flex items-center gap-3">
                    <Skeleton className="h-8 w-8 rounded" />
                    <div className="space-y-1.5">
                        <Skeleton className="h-6 w-48 rounded" />
                        <Skeleton className="h-4 w-72 rounded" />
                    </div>
                </div>
                <Skeleton className="h-20 w-full rounded-lg" />
                <Skeleton className="h-24 w-full rounded-lg" />
                <Skeleton className="h-48 w-full rounded-lg" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* ─── Header ───────────────────────────────────────────────────────── */}
            <div className="flex items-start gap-3">
                <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="mt-0.5 shrink-0"
                    onClick={() => navigate('..')}
                    aria-label="Back to properties"
                >
                    <ArrowLeftIcon className="size-4" aria-hidden />
                </Button>
                <div className="space-y-1">
                    <h1 className="text-2xl font-semibold">Dynamic properties</h1>
                    <p className="text-sm text-muted-foreground">
                        Automatically sync properties from an external HTTP endpoint on a cron schedule.
                    </p>
                </div>
            </div>

            {/* ─── Kubernetes read-only banner ───────────────────────────────────── */}
            {isKubernetesManaged && (
                <Alert>
                    <AlertDescription>
                        This API is managed by the Kubernetes operator. Dynamic property settings are read-only.
                    </AlertDescription>
                </Alert>
            )}

            {/* ─── Form ─────────────────────────────────────────────────────────── */}
            <DynamicPropertiesForm
                initialState={initialState}
                onSave={state => mutation.mutate(state)}
                isSaving={mutation.isPending}
                saveError={mutation.isError ? (mutation.error?.message ?? 'Failed to save dynamic properties.') : undefined}
                isReadOnly={isReadOnly}
            />
        </div>
    );
}
