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

import { Badge, Button, Skeleton } from '@gravitee/graphene-core';
import { ArrowLeftIcon, UsersRoundIcon } from '@gravitee/graphene-core/icons';
import { Link, useNavigate, useParams } from 'react-router-dom';

import { GroupMembershipTable } from '../features/groups/components/GroupMembershipTable';
import { GroupMembersTable } from '../features/groups/components/GroupMembersTable';
import {
    useGroupApis,
    useGroupApplications,
    useGroupApiProducts,
    useGroupDetail,
    useGroupMembers,
} from '../features/groups/hooks/useGroupDetail';
import { hasEventRule } from '../features/groups/utils/groupPayload';

// Classic Console's group.component.ts has no delete action on this screen at all — a group can only be
// deleted from the list (groups.component.ts). Editing group settings (name, default roles, invitation
// rules) also only happens from the list's Edit action, not this page — the detail page is read-only.
// Member management (add/invite/edit roles/remove) is added on top of this read-only view in a
// follow-up PR (FOUND-106/FOUND-107).
export function GroupDetailPage() {
    const { groupId } = useParams<{ groupId: string }>();
    const navigate = useNavigate();

    const { data: group, isLoading, isError } = useGroupDetail(groupId);
    const { data: members = [], isLoading: membersLoading } = useGroupMembers(groupId);
    const { data: apis = [], isLoading: apisLoading } = useGroupApis(groupId);
    const { data: applications = [], isLoading: applicationsLoading } = useGroupApplications(groupId);
    const { data: apiProducts = [], isLoading: apiProductsLoading } = useGroupApiProducts(groupId);

    if (isLoading) {
        return (
            <div className="space-y-4">
                <Skeleton className="h-8 w-32" />
                <Skeleton className="h-32 w-full rounded-xl" />
                <Skeleton className="h-56 w-full rounded-xl" />
            </div>
        );
    }

    if (isError || !group) {
        return (
            <div className="space-y-4">
                <Button type="button" variant="ghost" className="gap-1.5 px-0" onClick={() => navigate('..')}>
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to groups
                </Button>
                <p className="text-sm text-muted-foreground">Group not found or failed to load.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <Button type="button" variant="ghost" className="gap-1.5 px-0 text-muted-foreground" asChild>
                <Link to="..">
                    <ArrowLeftIcon className="size-4" aria-hidden />
                    Back to groups
                </Link>
            </Button>

            <section className="rounded-xl border bg-card p-5">
                <div className="flex items-start justify-between gap-4">
                    <div className="flex min-w-0 items-start gap-3">
                        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-orange-100 text-orange-700">
                            <UsersRoundIcon className="size-5" aria-hidden />
                        </div>
                        <div className="min-w-0 space-y-1">
                            <div className="flex flex-wrap items-center gap-2">
                                <h1 className="text-xl font-semibold tracking-tight">{group.name}</h1>
                                {hasEventRule(group, 'API_CREATE') && (
                                    <Badge variant="default" className="text-xs font-normal">
                                        Auto APIs
                                    </Badge>
                                )}
                                {hasEventRule(group, 'API_PRODUCT_CREATE') && (
                                    <Badge variant="default" className="text-xs font-normal">
                                        Auto API Products
                                    </Badge>
                                )}
                                {hasEventRule(group, 'APPLICATION_CREATE') && (
                                    <Badge variant="default" className="text-xs font-normal">
                                        Auto Applications
                                    </Badge>
                                )}
                            </div>
                            <p className="text-sm text-muted-foreground">No description provided.</p>
                        </div>
                    </div>
                </div>
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">Settings</h2>
                <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-3">
                    <div>
                        <dt className="text-sm text-muted-foreground">Default API role</dt>
                        <dd className="text-sm font-medium">{group.roles?.API ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Default API product role</dt>
                        <dd className="text-sm font-medium">{group.roles?.API_PRODUCT ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Default application role</dt>
                        <dd className="text-sm font-medium">{group.roles?.APPLICATION ?? 'Not set'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Lock API role</dt>
                        <dd className="text-sm font-medium">{group.lock_api_role ? 'Yes' : 'No'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Lock API product role</dt>
                        <dd className="text-sm font-medium">{group.lock_api_product_role ? 'Yes' : 'No'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Lock application role</dt>
                        <dd className="text-sm font-medium">{group.lock_application_role ? 'Yes' : 'No'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Maximum members</dt>
                        <dd className="text-sm font-medium">{group.max_invitation ?? 'Unlimited'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Invitation via search</dt>
                        <dd className="text-sm font-medium">{group.system_invitation ? 'Allowed' : 'Not allowed'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Invitation via email</dt>
                        <dd className="text-sm font-medium">{group.email_invitation ? 'Allowed' : 'Not allowed'}</dd>
                    </div>
                    <div>
                        <dt className="text-sm text-muted-foreground">Notify on new members</dt>
                        <dd className="text-sm font-medium">{group.disable_membership_notifications ? 'No' : 'Yes'}</dd>
                    </div>
                </dl>
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <div>
                    <h2 className="text-base font-semibold">Members</h2>
                    <p className="text-sm text-muted-foreground">Direct members of this group and their scoped roles.</p>
                </div>
                <GroupMembersTable members={members} loading={membersLoading} />
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">APIs</h2>
                <GroupMembershipTable
                    items={apis}
                    loading={apisLoading}
                    ariaLabel="APIs"
                    searchPlaceholder="Search APIs…"
                    emptyTitle="No dependent APIs to display"
                    emptyDescription="APIs associated with this group will appear here."
                />
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">API Products</h2>
                <GroupMembershipTable
                    items={apiProducts}
                    loading={apiProductsLoading}
                    ariaLabel="API Products"
                    searchPlaceholder="Search API Products…"
                    emptyTitle="No dependent API Products to display"
                    emptyDescription="API Products associated with this group will appear here."
                />
            </section>

            <section className="space-y-4 rounded-xl border bg-card p-5">
                <h2 className="text-base font-semibold">Applications</h2>
                <GroupMembershipTable
                    items={applications}
                    loading={applicationsLoading}
                    ariaLabel="Applications"
                    searchPlaceholder="Search Applications…"
                    emptyTitle="No dependent applications to display"
                    emptyDescription="Applications associated with this group will appear here."
                />
            </section>
        </div>
    );
}
