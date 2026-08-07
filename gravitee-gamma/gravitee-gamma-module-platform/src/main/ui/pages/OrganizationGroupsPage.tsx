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

import { Button } from '@gravitee/graphene-core';
import { ArrowLeftIcon } from '@gravitee/graphene-core/icons';
import { Link } from 'react-router-dom';

import { OrganizationGroupsTable } from '../features/groups/components/OrganizationGroupsTable';
import { useOrganizationGroups } from '../features/groups/hooks/useGroups';

/** FOUND-111 — an org-wide, cross-environment view for Organization Administrators, reached from the
 *  per-environment Groups list. Unlike that list, this page is read-only: no create/edit/delete,
 *  since acting on a group requires switching into its owning environment first. */
export function OrganizationGroupsPage() {
    const { data: groups = [], isLoading, isError } = useOrganizationGroups();

    if (isError) {
        return (
            <div className="flex items-center justify-center p-8">
                <p className="text-sm text-muted-foreground">Failed to load groups. Please refresh and try again.</p>
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

            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">All Groups</h1>
                <p className="text-sm text-muted-foreground">Every group across every environment in this organization.</p>
            </div>

            <OrganizationGroupsTable groups={groups} loading={isLoading} />
        </div>
    );
}
