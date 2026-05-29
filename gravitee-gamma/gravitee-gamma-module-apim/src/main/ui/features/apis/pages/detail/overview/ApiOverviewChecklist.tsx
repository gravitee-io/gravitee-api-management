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
import { ActivityIcon, LockIcon, UsersIcon, WorkflowIcon } from '@gravitee/graphene-core/icons';

import { OverviewChecklistCard, type OverviewChecklistItem } from '../../../../../shared/components/OverviewChecklistCard';
import { useChecklistOverrides } from '../../../../../shared/hooks/useChecklistOverrides';
import type { AlertTrigger, ApiDetailDto } from '../../../types';
import type { MembersResponse } from '../../../types/members.types';
import { hasDefaultEndpointGroupBackendSecurityConfigured } from '../../../utils/endpointGroupBackendSecurity';

function buildChecklistItems(
    api: ApiDetailDto | null,
    membersData: MembersResponse | undefined,
    alertsData: AlertTrigger[] | undefined,
    itemDone: (autoDone: boolean, id: string) => boolean,
): OverviewChecklistItem[] {
    const hasBackendSecurity = hasDefaultEndpointGroupBackendSecurityConfigured(api);
    const memberCount = membersData?.pagination?.totalCount ?? 0;
    const hasAlerts = Boolean(alertsData?.length);

    return [
        {
            id: 'endpoint-security',
            label: 'Configure backend security on your endpoint group',
            tooltip: 'Set up SSL/TLS, proxy authentication, or upstream headers on the default endpoint group shared configuration.',
            to: '../endpoints/list?editGroup=0&step=configuration',
            icon: LockIcon,
            actionLabel: 'Open configuration',
            done: itemDone(hasBackendSecurity, 'endpoint-security'),
        },
        {
            id: 'security-policies',
            label: 'Apply security policies',
            tooltip: 'Use the Policy Studio to add rate limiting, transformations, or custom security policies to your API flows.',
            to: '../policy-studio',
            icon: WorkflowIcon,
            actionLabel: 'Open Policy Studio',
            done: itemDone(false, 'security-policies'),
        },
        {
            id: 'alerts',
            label: 'Set up alerts',
            tooltip: 'Get notified when your API exceeds error thresholds or latency spikes.',
            to: '../alerts',
            icon: ActivityIcon,
            actionLabel: 'Open Alerts',
            done: itemDone(hasAlerts, 'alerts'),
        },
        {
            id: 'team-access',
            label: 'Invite teammates and assign roles',
            tooltip: 'Collaborate on this API — control who can view, edit, publish plans, or own the API.',
            to: '../user-permissions',
            icon: UsersIcon,
            actionLabel: 'Manage Access',
            done: itemDone(memberCount > 1, 'team-access'),
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
    const { overrideDone, overrideUndone, toggle } = useChecklistOverrides(api?.id);

    // done = (auto-detected && user hasn't manually unchecked) || user manually checked
    function itemDone(autoDone: boolean, id: string): boolean {
        return (autoDone && !overrideUndone.has(id)) || overrideDone.has(id);
    }

    const isReady = !isLoadingMembers && !isLoadingAlerts;

    return (
        <OverviewChecklistCard
            description="Finish setting up your API proxy. Each row links to the right screen."
            items={buildChecklistItems(api, membersData, alertsData, itemDone)}
            isReady={isReady}
            onToggle={toggle}
        />
    );
}
