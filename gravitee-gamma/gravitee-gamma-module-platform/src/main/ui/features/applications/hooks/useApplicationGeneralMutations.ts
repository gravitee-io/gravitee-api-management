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
import type { CreateClientCertificate, UpdateClientCertificate } from '../types/applicationCertificate';
import { applicationDetailKeys, applicationListKeys } from '../utils/queryKeys';

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

    const createCertificateMutation = useMutation({
        mutationFn: (certificate: CreateClientCertificate) => createApplicationCertificate(env!.id, applicationId!, certificate),
        onSuccess: invalidateCertificates,
    });

    const updateCertificateMutation = useMutation({
        mutationFn: ({ certificateId, update }: { certificateId: string; update: UpdateClientCertificate }) =>
            updateApplicationCertificate(env!.id, applicationId!, certificateId, update),
        onSuccess: invalidateCertificates,
    });

    return {
        saveMutation,
        deleteMutation,
        createCertificateMutation,
        updateCertificateMutation,
    };
}
