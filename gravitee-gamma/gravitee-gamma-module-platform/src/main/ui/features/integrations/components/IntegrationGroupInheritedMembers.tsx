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
import {
    Alert,
    AlertDescription,
    Card,
    CardContent,
    CardHeader,
    CardTitle,
    Empty,
    EmptyHeader,
    EmptyTitle,
    Skeleton,
} from '@gravitee/graphene-core';

import { GroupMembersSection } from '../../shared/components';
import type { GroupMember } from '../../shared/types/groupMembers';
import { useIntegrationGroupMembers, type IntegrationGroupMembersView } from '../hooks/useIntegrationGroupMembers';

function getIntegrationRoleName(member: GroupMember): string {
    return member.roles.INTEGRATION ?? '—';
}

function isShown(view: IntegrationGroupMembersView): boolean {
    return view.status === 'forbidden' || view.status === 'error' || (view.status === 'loaded' && view.members.length > 0);
}

function GroupView({ view }: Readonly<{ view: IntegrationGroupMembersView }>) {
    if (view.status === 'forbidden') {
        return (
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-sm">{view.group.name}</CardTitle>
                </CardHeader>
                <CardContent>
                    <p className="text-sm text-muted-foreground">
                        You do not have the appropriate permissions to view members of this group.
                    </p>
                </CardContent>
            </Card>
        );
    }

    if (view.status === 'error') {
        return (
            <Alert variant="destructive">
                <AlertDescription>Failed to load members for group {view.group.name}.</AlertDescription>
            </Alert>
        );
    }

    if (view.status !== 'loaded') {
        return null;
    }

    return <GroupMembersSection groupName={view.group.name} members={view.members} getRoleName={getIntegrationRoleName} />;
}

export function IntegrationGroupInheritedMembers({ integrationId, groupIds }: Readonly<{ integrationId: string; groupIds: string[] }>) {
    const { views, isLoading } = useIntegrationGroupMembers(integrationId, groupIds);
    const shownViews = views.filter(isShown);

    function renderContent() {
        if (isLoading) {
            return <Skeleton className="h-24 rounded-lg" />;
        }

        if (shownViews.length === 0) {
            return (
                <Empty>
                    <EmptyHeader>
                        <EmptyTitle>No group members</EmptyTitle>
                    </EmptyHeader>
                </Empty>
            );
        }

        return (
            <div className="space-y-4">
                {shownViews.map(view => (
                    <GroupView key={view.group.id} view={view} />
                ))}
            </div>
        );
    }

    return (
        <Card>
            <CardHeader className="pb-3">
                <CardTitle className="text-base">Group Inherited Members</CardTitle>
            </CardHeader>
            <CardContent>{renderContent()}</CardContent>
        </Card>
    );
}
