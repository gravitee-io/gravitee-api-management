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
import type { SearchableUser } from '../../../shared/types/userSearch';

export interface FailedMemberAdd {
    user: SearchableUser;
    reason: string;
}

export function summarizeAddMembersResults(
    users: ReadonlyArray<SearchableUser>,
    results: ReadonlyArray<PromiseSettledResult<unknown>>,
): { succeededCount: number; failed: FailedMemberAdd[] } {
    let succeededCount = 0;
    const failed: FailedMemberAdd[] = [];

    results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
            succeededCount += 1;
        } else {
            const reason = result.reason instanceof Error ? result.reason.message : String(result.reason);
            failed.push({ user: users[index]!, reason });
        }
    });

    return { succeededCount, failed };
}

function addMemberUserLabel(user: SearchableUser): string {
    return user.displayName || user.email || user.reference;
}

/** User-facing message when one or more bulk member adds fail (including partial success). */
export function formatAddMembersResultMessage(total: number, succeededCount: number, failed: ReadonlyArray<FailedMemberAdd>): string {
    const failedDescriptions = failed.map(({ user, reason }) => {
        const label = addMemberUserLabel(user);
        return reason ? `${label} (${reason})` : label;
    });
    const failedList = failedDescriptions.join(', ');

    if (succeededCount === 0) {
        if (total === 1) {
            const only = failed[0];
            if (!only) {
                return 'Failed to add member.';
            }
            return only.reason ? `Failed to add member: ${only.reason}` : `Failed to add ${addMemberUserLabel(only.user)}.`;
        }
        return `Failed to add ${total} members: ${failedList}.`;
    }

    return `Added ${succeededCount} of ${total} members. Failed to add: ${failedList}.`;
}

export class AddMembersFailedError extends Error {
    public readonly succeededCount: number;
    public readonly failedUsers: SearchableUser[];

    constructor(message: string, succeededCount: number, failedUsers: SearchableUser[]) {
        super(message);
        this.name = 'AddMembersFailedError';
        this.succeededCount = succeededCount;
        this.failedUsers = failedUsers;
    }
}

export function throwIfAnyAddFailed(users: ReadonlyArray<SearchableUser>, results: ReadonlyArray<PromiseSettledResult<unknown>>): void {
    const { succeededCount, failed } = summarizeAddMembersResults(users, results);
    if (failed.length > 0) {
        throw new AddMembersFailedError(
            formatAddMembersResultMessage(users.length, succeededCount, failed),
            succeededCount,
            failed.map(f => f.user),
        );
    }
}
