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
import { OrganizationGroupsTable } from '../features/groups/components/OrganizationGroupsTable';
import { SectionError } from '../features/groups/components/SectionError';
import { useOrganizationGroups } from '../shared/hooks/useOrganizationGroups';

export function OrganizationGroupsPage() {
    const { data: groups = [], isLoading, isError } = useOrganizationGroups();

    if (isError) {
        return <SectionError message="Failed to load organization groups. Please refresh and try again." />;
    }

    return (
        <div className="space-y-6">
            <div className="space-y-1">
                <h1 className="text-2xl font-semibold tracking-tight">Organization Groups</h1>
                <p className="text-sm text-muted-foreground">View groups across every environment in the organization.</p>
            </div>

            <OrganizationGroupsTable groups={groups} loading={isLoading} />
        </div>
    );
}
