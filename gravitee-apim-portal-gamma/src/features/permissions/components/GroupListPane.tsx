/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Button, Input } from '@gravitee/graphene-core';
import { PlusIcon } from '@gravitee/graphene-core/icons';

import type { PermissionsGroupSummary } from '../hooks/usePermissionsDirectory';
import { PORTAL_GROUP_MANAGEMENT_MODE_LABELS } from '../types/permissions.types';

interface GroupListPaneProps {
    readonly groups: readonly PermissionsGroupSummary[];
    readonly selectedGroupId: string | null;
    readonly canCreate: boolean;
    readonly query: string;
    readonly onQueryChange: (query: string) => void;
    readonly onSelect: (groupId: string) => void;
    readonly onCreate: () => void;
}

export function GroupListPane({
    groups,
    selectedGroupId,
    canCreate,
    query,
    onQueryChange,
    onSelect,
    onCreate,
}: GroupListPaneProps) {
    return (
        <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-lg border bg-card">
            <div className="flex items-center justify-between border-b bg-muted/40 px-3 py-2">
                <span className="text-sm font-medium">Groups</span>
                <Button
                    type="button"
                    variant="ghost"
                    size="icon-sm"
                    aria-label="Create group"
                    disabled={!canCreate}
                    title={canCreate ? 'Create group' : 'Select a tenant you administer first'}
                    onClick={onCreate}
                >
                    <PlusIcon className="size-4" aria-hidden />
                </Button>
            </div>

            <div className="border-b p-2">
                <Input
                    placeholder="Search groups"
                    aria-label="Search groups"
                    value={query}
                    onChange={event => onQueryChange(event.target.value)}
                />
            </div>

            <ul className="min-h-0 flex-1 overflow-y-auto">
                {groups.length === 0 ? (
                    <li className="px-3 py-6 text-center text-sm text-muted-foreground">
                        No groups in this tenant yet.
                    </li>
                ) : (
                    groups.map(group => {
                        const isSelected = group.id === selectedGroupId;
                        const mode = PORTAL_GROUP_MANAGEMENT_MODE_LABELS[group.managementMode];

                        return (
                            <li key={group.id}>
                                <button
                                    type="button"
                                    aria-current={isSelected}
                                    onClick={() => onSelect(group.id)}
                                    className={`w-full border-b border-l-2 px-3 py-2 text-left transition-colors ${
                                        isSelected
                                            ? 'border-l-primary bg-accent'
                                            : 'border-l-transparent hover:bg-muted/50'
                                    }`}
                                >
                                    <span
                                        className={`block truncate text-sm ${
                                            isSelected ? 'font-medium text-primary' : 'font-medium'
                                        }`}
                                    >
                                        {group.name}
                                    </span>
                                    <span className="mt-0.5 block truncate text-xs text-muted-foreground">
                                        <span title={mode.long} className="font-medium">
                                            {mode.short}
                                        </span>
                                        {' · '}
                                        {group.memberCount} {group.memberCount === 1 ? 'member' : 'members'}
                                    </span>
                                </button>
                            </li>
                        );
                    })
                )}
            </ul>
        </div>
    );
}
