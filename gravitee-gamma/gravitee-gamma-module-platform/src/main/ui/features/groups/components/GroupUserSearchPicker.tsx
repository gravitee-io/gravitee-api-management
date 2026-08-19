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

import { Badge, Button, Checkbox, cn, InputGroup, InputGroupAddon, InputGroupInput, Label, Skeleton } from '@gravitee/graphene-core';
import { SearchIcon, XIcon } from '@gravitee/graphene-core/icons';

import type { SearchableUser } from '../../../shared/types/userSearch';
import { isSameUser } from '../../../shared/utils/userSearch';
import { UserAvatar } from '../../users/components/UserAvatar';

const SEARCH_INPUT_ID = 'group-add-members-search';

export function GroupUserSearchPicker({
    search,
    onSearchChange,
    debouncedQuery,
    isFetching,
    candidates,
    selected,
    onToggle,
    invitationLimitReached,
    groupMemberCapReached = false,
    disabled = false,
}: Readonly<{
    search: string;
    onSearchChange: (value: string) => void;
    debouncedQuery: string;
    isFetching: boolean;
    candidates: SearchableUser[];
    selected: SearchableUser[];
    onToggle: (user: SearchableUser) => void;
    invitationLimitReached: boolean;
    /** True when existing members alone already hit max_invitation (not just the current selection). */
    groupMemberCapReached?: boolean;
    disabled?: boolean;
}>) {
    const searchDisabled = disabled || invitationLimitReached;

    function renderSearchResults() {
        if (invitationLimitReached) {
            return (
                <p className="px-1 py-2 text-sm text-muted-foreground">
                    {groupMemberCapReached
                        ? 'This group has reached its maximum number of members.'
                        : 'Selection limit reached for this group.'}
                </p>
            );
        }
        if (debouncedQuery.trim().length < 2) {
            return <p className="px-1 py-2 text-sm text-muted-foreground">Type at least 2 characters to search for users.</p>;
        }
        if (isFetching || search !== debouncedQuery) {
            return (
                <div className="space-y-2 p-1">
                    <Skeleton className="h-10 rounded" />
                    <Skeleton className="h-10 rounded" />
                </div>
            );
        }
        if (candidates.length === 0) {
            return <p className="px-1 py-2 text-sm text-muted-foreground">No users found.</p>;
        }
        return (
            <div className="rounded-lg border">
                {candidates.map(user => {
                    const isChecked = selected.some(u => isSameUser(u, user));
                    const inputId = `add-member-${user.id ?? user.reference}`;
                    return (
                        <label
                            key={user.id ?? user.reference}
                            htmlFor={inputId}
                            className={cn(
                                'flex items-center gap-3 border-b px-3 py-2.5 last:border-b-0',
                                disabled ? 'opacity-50' : 'cursor-pointer hover:bg-muted/50',
                            )}
                        >
                            <Checkbox id={inputId} checked={isChecked} onCheckedChange={() => onToggle(user)} disabled={disabled} />
                            <UserAvatar name={user.displayName} />
                            <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-medium">{user.displayName}</p>
                                {user.email ? <p className="truncate text-xs text-muted-foreground">{user.email}</p> : null}
                            </div>
                        </label>
                    );
                })}
            </div>
        );
    }

    return (
        <div className="space-y-2">
            <Label htmlFor={SEARCH_INPUT_ID} className="text-sm font-medium">
                Search users
            </Label>
            <InputGroup>
                <InputGroupAddon align="inline-start">
                    <SearchIcon className="size-3.5 text-muted-foreground" aria-hidden />
                </InputGroupAddon>
                <InputGroupInput
                    id={SEARCH_INPUT_ID}
                    placeholder="Search by name or email…"
                    value={search}
                    onChange={e => onSearchChange(e.target.value)}
                    disabled={searchDisabled}
                />
            </InputGroup>

            {selected.length > 0 && (
                <div className="flex flex-wrap gap-1.5" aria-label="Selected users">
                    {selected.map(user => (
                        <Badge key={user.id ?? user.reference} variant="secondary" className="gap-0.5 pr-1 font-normal">
                            {user.displayName}
                            <Button
                                type="button"
                                variant="ghost"
                                size="icon-xs"
                                className="ml-0.5 shrink-0 hover:text-destructive"
                                onClick={() => onToggle(user)}
                                aria-label={`Remove ${user.displayName}`}
                                disabled={disabled}
                            >
                                <XIcon className="size-3" aria-hidden />
                            </Button>
                        </Badge>
                    ))}
                </div>
            )}

            {renderSearchResults()}
        </div>
    );
}
