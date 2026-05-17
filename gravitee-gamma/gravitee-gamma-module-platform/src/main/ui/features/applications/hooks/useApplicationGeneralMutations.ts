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

import {
    createApplicationCertificate,
    deleteApplication,
    updateApplication,
    updateApplicationCertificate,
    type UpdateApplicationPayload,
} from '../services/applicationDetail';
import type { ApplicationListItem } from '../types/application';
import type { ClientCertificate, CreateClientCertificate, UpdateClientCertificate } from '../types/applicationCertificate';
import { applicationDetailKeys, applicationListKeys } from '../utils/queryKeys';

export const GRACE_PERIOD_UPDATE_ERROR = 'GracePeriodUpdateError';

export interface AddCertificateWithGraceInput {
    readonly create: CreateClientCertificate;
    readonly gracePeriod?: {
        readonly certificateId: string;
        readonly name: string;
        readonly endsAt: string;
    };
}

export interface GracePeriodUpdateFailure {
    readonly message: string;
    readonly certificateId: string;
    readonly name: string;
    readonly endsAt: string;
}

function isGracePeriodUpdateError(error: unknown): error is Error {
    return error instanceof Error && error.name === GRACE_PERIOD_UPDATE_ERROR;
}

export function toGracePeriodUpdateFailure(error: unknown): GracePeriodUpdateFailure | null {
    if (!isGracePeriodUpdateError(error)) {
        return null;
    }
    const failure = (error as Error & { gracePeriod?: GracePeriodUpdateFailure }).gracePeriod;
    return failure ?? null;
}

interface ApplicationGeneralSideEffects {
    onDeleteSuccess?: () => void;
}

export function useApplicationGeneralMutations(
    application: ApplicationListItem | null,
    applicationId: string | undefined,
    sideEffects: ApplicationGeneralSideEffects = {},
) {
    const env = useEnvironment();
    const queryClient = useQueryClient();
    const sideEffectsRef = useRef(sideEffects);
    sideEffectsRef.current = sideEffects;

    const invalidateApplicationList = () => {
        void queryClient.invalidateQueries({ queryKey: applicationListKeys.all });
    };

    const invalidateDetail = () => {
        void queryClient.invalidateQueries({
            queryKey: applicationDetailKeys.detail(env?.id ?? '', applicationId ?? ''),
        });
        invalidateApplicationList();
    };

    const invalidateCertificates = () => {
        void queryClient.invalidateQueries({
            queryKey: applicationDetailKeys.certificates(env?.id ?? '', applicationId ?? ''),
        });
        invalidateDetail();
    };

    const saveMutation = useMutation({
        mutationFn: (payload: UpdateApplicationPayload) => updateApplication(env!.id, application!, payload),
        onSuccess: invalidateDetail,
    });

    const deleteMutation = useMutation({
        mutationFn: () => deleteApplication(env!.id, applicationId!),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: applicationListKeys.all });
            sideEffectsRef.current.onDeleteSuccess?.();
        },
    });

    const addCertificateWithGraceMutation = useMutation({
        mutationFn: async ({ create, gracePeriod }: AddCertificateWithGraceInput): Promise<ClientCertificate> => {
            const created = await createApplicationCertificate(env!.id, applicationId!, create);
            if (!gracePeriod) {
                return created;
            }
            try {
                await updateApplicationCertificate(env!.id, applicationId!, gracePeriod.certificateId, {
                    name: gracePeriod.name,
                    endsAt: gracePeriod.endsAt,
                });
            } catch (cause) {
                const detail = cause instanceof Error ? cause.message : 'Grace period update failed.';
                const error = new Error(`Certificate added but grace period update failed: ${detail}`);
                error.name = GRACE_PERIOD_UPDATE_ERROR;
                (error as Error & { gracePeriod: GracePeriodUpdateFailure }).gracePeriod = {
                    message: error.message,
                    certificateId: gracePeriod.certificateId,
                    name: gracePeriod.name,
                    endsAt: gracePeriod.endsAt,
                };
                throw error;
            }
            return created;
        },
        onSuccess: invalidateCertificates,
    });

    const updateCertificateMutation = useMutation({
        mutationFn: ({ certificateId, update }: { certificateId: string; update: UpdateClientCertificate }) =>
            updateApplicationCertificate(env!.id, applicationId!, certificateId, update),
        onSuccess: invalidateCertificates,
    });

    const isMutating =
        saveMutation.isPending ||
        deleteMutation.isPending ||
        addCertificateWithGraceMutation.isPending ||
        updateCertificateMutation.isPending;

    return {
        saveMutation,
        deleteMutation,
        addCertificateWithGraceMutation,
        updateCertificateMutation,
        isMutating,
    };
}
