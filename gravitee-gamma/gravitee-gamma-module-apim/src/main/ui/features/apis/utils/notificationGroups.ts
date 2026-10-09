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
import type { Group } from '../types/members.types';

export interface NotificationGroupOption {
    id: string;
    name: string;
}

/** Maps API group refs (id or name) to selectable options. */
export function mapApiGroupsToNotificationOptions(apiGroupRefs: string[] | undefined, allGroups: Group[]): NotificationGroupOption[] {
    const options: NotificationGroupOption[] = [];
    for (const ref of apiGroupRefs ?? []) {
        const group = allGroups.find(g => g.id === ref || g.name === ref);
        if (group) {
            options.push({ id: group.id, name: group.name });
        }
    }
    return options;
}

/** Prepend a forced Primary Owner option when missing. */
export function withPrimaryOwnerOption(options: NotificationGroupOption[], primaryOwnerId: string | undefined): NotificationGroupOption[] {
    if (!primaryOwnerId) return options;
    if (options.some(g => g.id === primaryOwnerId)) return options;
    return [{ id: primaryOwnerId, name: 'Primary Owner' }, ...options];
}

/** Notification groups plus forced primary owner for the form selection. */
export function selectedGroupsWithPrimaryOwner(notificationGroups: string[] | undefined, primaryOwnerId: string | undefined): string[] {
    const selected = [...(notificationGroups ?? [])];
    if (primaryOwnerId && !selected.includes(primaryOwnerId)) {
        selected.push(primaryOwnerId);
    }
    return selected;
}

/** Remove the primary-owner id before persisting groups. */
export function cleanseNotificationGroups(selectedGroups: string[], primaryOwnerId: string | undefined): string[] {
    if (!primaryOwnerId) return selectedGroups;
    return selectedGroups.filter(g => g !== primaryOwnerId);
}
