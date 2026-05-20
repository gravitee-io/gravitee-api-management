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
import { ActivityIcon, GlobeIcon, LockIcon, NetworkIcon, UsersIcon, WorkflowIcon } from '@gravitee/graphene-core/icons';
import { useMemo } from 'react';

import { OverviewChecklistCard, type OverviewChecklistItem } from '../../../../../shared/components/OverviewChecklistCard';
import type { AlertTrigger, ApiDetailDto } from '../../../types/api';
import type { MembersResponse } from '../../../types/members.types';

function buildChecklistItems(
    api: ApiDetailDto | null,
    membersData: MembersResponse | undefined,
    alertsData: AlertTrigger[] | undefined,
): OverviewChecklistItem[] {
    const hasEndpointGroup = Boolean(api?.endpointGroups?.[0]?.endpoints?.length);
    const memberCount = membersData?.pagination?.totalCount ?? 0;
    const isPublished = api?.lifecycleState === 'PUBLISHED';

    return [
        {
            id: 'endpoint-security',
            label: 'Configure upstream endpoints',
            tooltip: 'Set up load balancing, SSL/TLS, or authentication between the gateway and your upstream service.',
            to: '../endpoints/list',
            icon: NetworkIcon,
            actionLabel: 'Open Endpoints',
            done: hasEndpointGroup,
        },
        {
            id: 'security-policies',
            label: 'Apply security policies',
            tooltip: 'Use the Policy Studio to add rate limiting, transformations, or custom security policies to your API flows.',
            to: '../policy-studio',
            icon: WorkflowIcon,
            actionLabel: 'Open Policy Studio',
            done: false,
        },
        {
            id: 'authorization',
            label: 'Apply authorization',
            tooltip: 'Configure OAuth2, JWT, or API-key based authorization plans.',
            icon: LockIcon,
            actionLabel: 'Coming soon',
            done: false,
            comingSoon: true,
        },
        {
            id: 'alerts',
            label: 'Set up alerts',
            tooltip: 'Get notified when your API exceeds error thresholds or latency spikes.',
            to: '../alerts',
            icon: ActivityIcon,
            actionLabel: 'Open Alerts',
            done: Boolean(alertsData?.length),
        },
        {
            id: 'team-access',
            label: 'Invite teammates and assign roles',
            tooltip: 'Collaborate on this API — control who can view, edit, publish plans, or own the API.',
            to: '../user-permissions',
            icon: UsersIcon,
            actionLabel: 'Manage Access',
            done: memberCount > 1,
        },
        {
            id: 'publish',
            label: 'Publish API',
            tooltip: 'Set your API lifecycle state to Published so consumers can discover and subscribe to it.',
            to: '../general',
            icon: GlobeIcon,
            actionLabel: 'Open General',
            done: isPublished,
        },
    ];
}

interface ApiOverviewChecklistProps {
    api: ApiDetailDto | null;
    membersData: MembersResponse | undefined;
    alertsData: AlertTrigger[] | undefined;
    isLoadingMembers: boolean;
    isLoadingAlerts: boolean;
}

export function ApiOverviewChecklist({
    api,
    membersData,
    alertsData,
    isLoadingMembers,
    isLoadingAlerts,
}: Readonly<ApiOverviewChecklistProps>) {
    const isReady = !isLoadingMembers && !isLoadingAlerts;

    const items = useMemo(
        () => (isReady ? buildChecklistItems(api, membersData, alertsData) : []),
        [api, membersData, alertsData, isReady],
    );

    return (
        <OverviewChecklistCard
            description="Finish setting up your API proxy. Each row links to the right screen."
            items={items}
            isReady={isReady}
            totalCountHint={4}
        />
    );
}
