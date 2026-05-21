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
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useEnvironmentPermissionsReady } from '../../../shared/hooks/useEnvironmentPermissions';
import { useApplicationDetailContext } from '../context/ApplicationDetailContext';
import {
    createApplicationSubscription,
    listApplicationSubscriptions,
    listSubscribableApiPlans,
    listSubscribableApiProductPlans,
    searchApiProductsForSubscription,
    searchApisForSubscription,
} from '../services/applicationSubscriptions';
import type { NewApplicationSubscriptionPayload, SubscriptionReferenceSelection } from '../types/applicationSubscription';
import { applicationDetailKeys, applicationSubscriptionKeys } from '../utils/queryKeys';

const API_KEY_SUBSCRIPTIONS_PAGE = 1;
const API_KEY_SUBSCRIPTIONS_PAGE_SIZE = 20;
const API_KEY_SUBSCRIPTION_STATUSES = ['ACCEPTED', 'PAUSED', 'PENDING'] as const;
const API_KEY_SUBSCRIPTIONS_FILTERS = { status: [...API_KEY_SUBSCRIPTION_STATUSES], securityTypes: ['API_KEY'] };

export function useCanSearchApiProductsForSubscription(): boolean {
    const { permissionsReady: applicationPermissionsReady } = useApplicationDetailContext();
    const environmentPermissionsReady = useEnvironmentPermissionsReady();
    const hasPermission = useHasPermission({ anyOf: ['environment-api_product-r'] });

    return applicationPermissionsReady && environmentPermissionsReady && hasPermission;
}

export function useSubscriptionReferenceSearch(query: string) {
    const env = useEnvironment();
    const { permissionsReady: applicationPermissionsReady } = useApplicationDetailContext();
    const environmentPermissionsReady = useEnvironmentPermissionsReady();
    const trimmed = query.trim();
    const permissionsReady = applicationPermissionsReady && environmentPermissionsReady;
    const canSearchApiProducts = useCanSearchApiProductsForSubscription();

    return useQuery({
        queryKey: applicationSubscriptionKeys.referenceSearch(env?.id ?? '', trimmed, canSearchApiProducts),
        queryFn: async () => {
            const [apisResponse, productsResponse] = await Promise.all([
                searchApisForSubscription(env!.id, trimmed),
                canSearchApiProducts ? searchApiProductsForSubscription(env!.id, trimmed) : Promise.resolve({ data: [] }),
            ]);

            return [
                ...apisResponse.data.map(value => ({ type: 'API' as const, value })),
                ...productsResponse.data.map(value => ({ type: 'API_PRODUCT' as const, value })),
            ].sort((a, b) => a.value.name.localeCompare(b.value.name));
        },
        enabled: Boolean(env && trimmed.length > 0 && permissionsReady),
        staleTime: 30_000,
    });
}

export function useSubscribablePlans(reference: SubscriptionReferenceSelection | null, applicationId: string | undefined) {
    const env = useEnvironment();

    return useQuery({
        queryKey: applicationSubscriptionKeys.subscribablePlans(
            env?.id ?? '',
            reference?.type ?? '',
            reference?.id ?? '',
            applicationId ?? '',
        ),
        queryFn: () => {
            if (!env || !reference || !applicationId) {
                throw new Error('Subscribable plans query requires environment, reference, and application');
            }
            return reference.type === 'API'
                ? listSubscribableApiPlans(env.id, reference.id, applicationId)
                : listSubscribableApiProductPlans(env.id, reference.id, applicationId);
        },
        enabled: Boolean(env && reference && applicationId),
        staleTime: 30_000,
    });
}

export function useApplicationApiKeySubscriptions(applicationId: string | undefined, enabled: boolean) {
    const env = useEnvironment();

    return useQuery({
        queryKey: ['application-api-key-subscriptions', env?.id ?? '', applicationId ?? ''],
        queryFn: async () => {
            const response = await listApplicationSubscriptions(
                env!.id,
                applicationId!,
                API_KEY_SUBSCRIPTIONS_FILTERS,
                API_KEY_SUBSCRIPTIONS_PAGE,
                API_KEY_SUBSCRIPTIONS_PAGE_SIZE,
            );
            return response.data ?? [];
        },
        enabled: Boolean(env && applicationId && enabled),
        staleTime: 30_000,
    });
}

export function useCreateApplicationSubscription(applicationId: string | undefined) {
    const env = useEnvironment();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ planId, payload }: { planId: string; payload: NewApplicationSubscriptionPayload }) =>
            createApplicationSubscription(env!.id, applicationId!, planId, payload),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: applicationSubscriptionKeys.all });
            if (applicationId && env?.id) {
                void queryClient.invalidateQueries({
                    queryKey: applicationDetailKeys.detail(env.id, applicationId),
                });
            }
        },
    });
}
