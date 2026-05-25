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
import { useQuery } from '@tanstack/react-query';

import { listApplicationMembers } from '../services/applicationMembers';
import { listApplicationNotifications } from '../services/applicationNotifications';
import { listApplicationSubscriptions } from '../services/applicationSubscriptions';
import type { ApplicationSubscriptionsFilters } from '../types/applicationSubscription';
import { applicationMemberKeys, applicationNotificationKeys, applicationSubscriptionKeys } from '../utils/queryKeys';

const OVERVIEW_COUNT_PAGE = 1;
const OVERVIEW_COUNT_SIZE = 1;

const ACTIVE_SUBSCRIPTIONS_FILTER: ApplicationSubscriptionsFilters = { status: ['ACCEPTED'] };
const PENDING_SUBSCRIPTIONS_FILTER: ApplicationSubscriptionsFilters = { status: ['PENDING'] };

export interface ApplicationOverviewData {
    readonly activeSubscriptionCount: number;
    readonly isError: boolean;
    readonly isLoadingActiveSubscriptions: boolean;
    readonly isLoadingMembers: boolean;
    readonly isLoadingNotifications: boolean;
    readonly isLoadingPendingSubscriptions: boolean;
    readonly isLoadingSubscriptions: boolean;
    readonly memberCount: number;
    readonly notificationCount: number;
    readonly pendingSubscriptionCount: number;
    readonly subscriptionCount: number;
}

function getSubscriptionCount(response: Awaited<ReturnType<typeof listApplicationSubscriptions>> | undefined): number {
    return response?.page?.total_elements ?? 0;
}

export function useApplicationOverviewData(applicationId: string | undefined): ApplicationOverviewData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && applicationId);

    const membersQuery = useQuery({
        queryKey: applicationMemberKeys.list(envId, applicationId ?? ''),
        queryFn: () => listApplicationMembers(envId, applicationId!),
        enabled,
        staleTime: 30_000,
    });

    const notificationsQuery = useQuery({
        queryKey: applicationNotificationKeys.list(envId, applicationId ?? ''),
        queryFn: () => listApplicationNotifications(envId, applicationId!),
        enabled,
        staleTime: 30_000,
    });

    const subscriptionsQuery = useQuery({
        queryKey: applicationSubscriptionKeys.list(envId, applicationId ?? '', undefined, OVERVIEW_COUNT_PAGE, OVERVIEW_COUNT_SIZE),
        queryFn: () => listApplicationSubscriptions(envId, applicationId!, undefined, OVERVIEW_COUNT_PAGE, OVERVIEW_COUNT_SIZE),
        enabled,
        staleTime: 30_000,
    });

    const activeSubscriptionsQuery = useQuery({
        queryKey: applicationSubscriptionKeys.list(
            envId,
            applicationId ?? '',
            ACTIVE_SUBSCRIPTIONS_FILTER,
            OVERVIEW_COUNT_PAGE,
            OVERVIEW_COUNT_SIZE,
        ),
        queryFn: () =>
            listApplicationSubscriptions(envId, applicationId!, ACTIVE_SUBSCRIPTIONS_FILTER, OVERVIEW_COUNT_PAGE, OVERVIEW_COUNT_SIZE),
        enabled,
        staleTime: 30_000,
    });

    const pendingSubscriptionsQuery = useQuery({
        queryKey: applicationSubscriptionKeys.list(
            envId,
            applicationId ?? '',
            PENDING_SUBSCRIPTIONS_FILTER,
            OVERVIEW_COUNT_PAGE,
            OVERVIEW_COUNT_SIZE,
        ),
        queryFn: () =>
            listApplicationSubscriptions(envId, applicationId!, PENDING_SUBSCRIPTIONS_FILTER, OVERVIEW_COUNT_PAGE, OVERVIEW_COUNT_SIZE),
        enabled,
        staleTime: 30_000,
    });

    return {
        activeSubscriptionCount: getSubscriptionCount(activeSubscriptionsQuery.data),
        isError:
            membersQuery.isError ||
            notificationsQuery.isError ||
            subscriptionsQuery.isError ||
            activeSubscriptionsQuery.isError ||
            pendingSubscriptionsQuery.isError,
        isLoadingActiveSubscriptions: activeSubscriptionsQuery.isLoading,
        isLoadingMembers: membersQuery.isLoading,
        isLoadingNotifications: notificationsQuery.isLoading,
        isLoadingPendingSubscriptions: pendingSubscriptionsQuery.isLoading,
        isLoadingSubscriptions: subscriptionsQuery.isLoading,
        memberCount: membersQuery.data?.length ?? 0,
        notificationCount: notificationsQuery.data?.length ?? 0,
        pendingSubscriptionCount: getSubscriptionCount(pendingSubscriptionsQuery.data),
        subscriptionCount: getSubscriptionCount(subscriptionsQuery.data),
    };
}
