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
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import { useParams } from 'react-router-dom';

import {
    deleteApi,
    deleteApiBackground,
    deleteApiPicture,
    duplicateApi,
    startApi,
    stopApi,
    updateApiBackground,
    updateApiFromDefinition,
    updateApiGeneral,
    updateApiPicture,
} from '../services/apis';
import type { ApiDetailDto } from '../types';
import { apiDetailKeys } from '../utils/queryKeys';

interface ApiGeneralSideEffects {
    onDeleteSuccess?: () => void;
    onDuplicateSuccess?: (newApi: ApiDetailDto) => void;
    onImportSuccess?: (updatedApi: ApiDetailDto) => void;
}

export function useApiGeneralMutations(api: ApiDetailDto | null, sideEffects: ApiGeneralSideEffects = {}) {
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    // Keep callbacks in a ref so mutations always call the latest version
    const sideEffectsRef = useRef(sideEffects);
    sideEffectsRef.current = sideEffects;

    const invalidateDetail = () => void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });

    const saveMutation = useMutation({
        mutationFn: (patch: Partial<ApiDetailDto>) => updateApiGeneral(env!.id, apiId!, api!, patch),
        onSuccess: invalidateDetail,
    });

    const startMutation = useMutation({
        mutationFn: () => startApi(env!.id, apiId!),
        onSuccess: invalidateDetail,
    });

    const stopMutation = useMutation({
        mutationFn: () => stopApi(env!.id, apiId!),
        onSuccess: invalidateDetail,
    });

    const deleteMutation = useMutation({
        mutationFn: () => deleteApi(env!.id, apiId!),
        onSuccess: () => sideEffectsRef.current.onDeleteSuccess?.(),
    });

    const duplicateMutation = useMutation({
        mutationFn: (opts: { version: string; contextPath: string }) =>
            duplicateApi(env!.id, apiId!, {
                version: opts.version,
                ...(opts.contextPath ? { contextPath: opts.contextPath } : {}),
            }),
        onSuccess: newApi => sideEffectsRef.current.onDuplicateSuccess?.(newApi),
    });

    const importMutation = useMutation({
        mutationFn: (definition: unknown) => updateApiFromDefinition(env!.id, apiId!, definition),
        onSuccess: updatedApi => {
            invalidateDetail();
            sideEffectsRef.current.onImportSuccess?.(updatedApi);
        },
    });

    const pictureMutation = useMutation({
        mutationFn: (b64: string) => updateApiPicture(env!.id, apiId!, b64),
        onSuccess: invalidateDetail,
    });

    const removePictureMutation = useMutation({
        mutationFn: () => deleteApiPicture(env!.id, apiId!),
        onSuccess: invalidateDetail,
    });

    const backgroundMutation = useMutation({
        mutationFn: (b64: string) => updateApiBackground(env!.id, apiId!, b64),
        onSuccess: invalidateDetail,
    });

    const removeBackgroundMutation = useMutation({
        mutationFn: () => deleteApiBackground(env!.id, apiId!),
        onSuccess: invalidateDetail,
    });

    return {
        saveMutation,
        startMutation,
        stopMutation,
        deleteMutation,
        duplicateMutation,
        importMutation,
        pictureMutation,
        removePictureMutation,
        backgroundMutation,
        removeBackgroundMutation,
    };
}
