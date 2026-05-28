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
import { BellIcon, PlugIcon, SlidersHorizontalIcon, UsersIcon } from '@gravitee/graphene-core/icons';

import { OverviewChecklistCard, type OverviewChecklistItem } from './OverviewChecklistCard';
import type { ApplicationOverviewData } from '../../hooks/useApplicationOverviewData';
import { useChecklistOverrides } from '../../hooks/useChecklistOverrides';
import type { ApplicationListItem } from '../../types/application';
import { hasReviewedGeneralSettings } from '../../utils/applicationOverviewHelpers';

interface ApplicationOverviewChecklistProps {
    readonly application: ApplicationListItem | null;
    readonly overviewData: ApplicationOverviewData;
}

export function ApplicationOverviewChecklist({ application, overviewData }: Readonly<ApplicationOverviewChecklistProps>) {
    const { overrideDone, overrideUndone, toggle } = useChecklistOverrides(application?.id);

    // done = (auto-detected && user hasn't manually unchecked) || user manually checked
    function itemDone(autoDone: boolean, id: string): boolean {
        return (autoDone && !overrideUndone.has(id)) || overrideDone.has(id);
    }

    const isReady = !overviewData.isLoadingMembers && !overviewData.isLoadingNotifications && !overviewData.isLoadingSubscriptions;

    const items: OverviewChecklistItem[] = [
        {
            actionLabel: 'Open General',
            done: itemDone(hasReviewedGeneralSettings(application), 'general-settings'),
            icon: SlidersHorizontalIcon,
            id: 'general-settings',
            label: 'Review general settings',
            to: '../general',
            tooltip: 'Review the application profile, security type, OAuth client details, and lifecycle settings.',
        },
        {
            actionLabel: 'Open Subscriptions',
            done: itemDone(overviewData.subscriptionCount > 0, 'subscriptions'),
            icon: PlugIcon,
            id: 'subscriptions',
            label: 'Subscribe to an API plan',
            to: '../subscriptions',
            tooltip: 'Create a subscription so this application can consume an API or API product plan.',
        },
        {
            actionLabel: 'Manage Access',
            done: itemDone(overviewData.memberCount > 1, 'team-access'),
            icon: UsersIcon,
            id: 'team-access',
            label: 'Invite teammates and assign roles',
            to: '../user-permissions',
            tooltip: 'Collaborate on this application by adding members, groups, and application-scoped roles.',
        },
        {
            actionLabel: 'Open Notifications',
            done: itemDone(overviewData.notificationCount > 0, 'notifications'),
            icon: BellIcon,
            id: 'notifications',
            label: 'Configure notification settings',
            to: '../notifications',
            tooltip: 'Subscribe this application to notifications for subscription, API key, and lifecycle events.',
        },
    ];

    return (
        <OverviewChecklistCard
            description="Finish setting up your application. Each row links to the right screen."
            isReady={isReady}
            items={items}
            onToggle={toggle}
            totalCountHint={4}
        />
    );
}
