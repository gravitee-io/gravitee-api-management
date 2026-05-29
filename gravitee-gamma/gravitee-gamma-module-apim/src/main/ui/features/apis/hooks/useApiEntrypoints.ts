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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';

import { useApiDetailContext } from '../context/ApiDetailContext';
import { getExposedEntrypoints, updateApiListeners } from '../services/entrypoints';
import type { HttpListener } from '../types';
import { apiDetailKeys, apiEntrypointKeys } from '../utils/queryKeys';

export function useApiEntrypoints(showConfig: boolean) {
    const { api } = useApiDetailContext();
    const { apiId } = useParams<{ apiId: string }>();
    const env = useEnvironment();
    const queryClient = useQueryClient();

    const exposedQuery = useQuery({
        queryKey: apiEntrypointKeys.exposed(env?.id ?? '', apiId ?? ''),
        queryFn: () => getExposedEntrypoints(env!.id, apiId!),
        enabled: Boolean(env && apiId && showConfig),
    });

    const saveMutation = useMutation({
        mutationFn: (listeners: HttpListener[]) => updateApiListeners(env!.id, apiId!, api!, listeners),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: apiDetailKeys.detail(env?.id ?? '', apiId ?? '') });
            void queryClient.invalidateQueries({ queryKey: apiEntrypointKeys.exposed(env?.id ?? '', apiId ?? '') });
        },
    });

    return { exposedQuery, saveMutation };
}
