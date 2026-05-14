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

import { ApimApiError } from '../../../shared/api/apimClient';
import { createApplication } from '../services/applicationCreate';
import type { ApplicationTypeConfig, CreatedApplication, RegisterApplicationDraft } from '../types/applicationCreate';
import { mapDraftToCreateRequest } from '../utils/applicationCreateMapper';
import { applicationListKeys } from '../utils/queryKeys';

interface CreateApplicationInput {
    draft: RegisterApplicationDraft;
    selectedType: ApplicationTypeConfig;
}

export function useCreateApplication() {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation<CreatedApplication, ApimApiError, CreateApplicationInput>({
        mutationFn: async ({ draft, selectedType }) => {
            if (!env) throw new ApimApiError(0, 'Environment not ready');
            return createApplication(env.id, mapDraftToCreateRequest(draft, selectedType));
        },
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: applicationListKeys.all });
        },
    });
}
