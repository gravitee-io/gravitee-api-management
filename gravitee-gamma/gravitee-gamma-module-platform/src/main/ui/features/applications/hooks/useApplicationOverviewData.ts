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

import { useApplicationMembers } from './useApplicationMembers';
import { useApplicationSubscriptionCount } from './useApplicationSubscriptions';
import { listApplicationNotifications } from '../services/applicationNotifications';
import type { ApplicationSubscriptionsFilters } from '../types/applicationSubscription';
import { ALL_SUBSCRIPTION_STATUSES } from '../utils/applicationSubscriptionConstants';
import { applicationNotificationKeys } from '../utils/queryKeys';

const ALL_SUBSCRIPTIONS_FILTER: ApplicationSubscriptionsFilters = { status: [...ALL_SUBSCRIPTION_STATUSES] };
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

export function useApplicationOverviewData(applicationId: string | undefined): ApplicationOverviewData {
    const env = useEnvironment();
    const envId = env?.id ?? '';
    const enabled = Boolean(env && applicationId);

    // Shares React Query cache with Application user permissions (same list key).
    const membersQuery = useApplicationMembers(applicationId);

    // Shares cache with Notification settings (same list key).
    const notificationsQuery = useQuery({
        queryKey: applicationNotificationKeys.list(envId, applicationId ?? ''),
        queryFn: () => listApplicationNotifications(envId, applicationId!),
        enabled,
        staleTime: 30_000,
    });

    // Count keys are shared with the subscriptions list (primed on list fetch with matching filters).
    const subscriptionsCountQuery = useApplicationSubscriptionCount(applicationId, ALL_SUBSCRIPTIONS_FILTER);
    const activeSubscriptionsCountQuery = useApplicationSubscriptionCount(applicationId, ACTIVE_SUBSCRIPTIONS_FILTER);
    const pendingSubscriptionsCountQuery = useApplicationSubscriptionCount(applicationId, PENDING_SUBSCRIPTIONS_FILTER);

    return {
        activeSubscriptionCount: activeSubscriptionsCountQuery.data ?? 0,
        isError:
            membersQuery.isError ||
            notificationsQuery.isError ||
            subscriptionsCountQuery.isError ||
            activeSubscriptionsCountQuery.isError ||
            pendingSubscriptionsCountQuery.isError,
        isLoadingActiveSubscriptions: activeSubscriptionsCountQuery.isLoading,
        isLoadingMembers: membersQuery.isLoading,
        isLoadingNotifications: notificationsQuery.isLoading,
        isLoadingPendingSubscriptions: pendingSubscriptionsCountQuery.isLoading,
        isLoadingSubscriptions: subscriptionsCountQuery.isLoading,
        memberCount: membersQuery.data?.length ?? 0,
        notificationCount: notificationsQuery.data?.length ?? 0,
        pendingSubscriptionCount: pendingSubscriptionsCountQuery.data ?? 0,
        subscriptionCount: subscriptionsCountQuery.data ?? 0,
    };
}
