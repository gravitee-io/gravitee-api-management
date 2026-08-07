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

import type { Group, GroupEventRule, GroupEventName } from '../types/group';

/** Shared by the groups list and detail page for the "Auto APIs/API Products/Applications" badges. */
export function hasEventRule(group: Group, event: GroupEventName): boolean {
    return (group.event_rules ?? []).some(rule => rule.event === event);
}

/** Rebuilds the full event-rules list from the three toggles the UI manages. */
export function buildEventRules(events: { apiCreate: boolean; applicationCreate: boolean; apiProductCreate: boolean }): GroupEventRule[] {
    const rules: GroupEventRule[] = [];
    if (events.apiCreate) rules.push({ event: 'API_CREATE' });
    if (events.applicationCreate) rules.push({ event: 'APPLICATION_CREATE' });
    if (events.apiProductCreate) rules.push({ event: 'API_PRODUCT_CREATE' });
    return rules;
}

/**
 * Keeps any other role scope untouched — only sets/clears API, APPLICATION, and API_PRODUCT.
 * Always returns a map (possibly `{}`), never `undefined`: the v1 API treats an absent `roles`
 * field on the PUT body as "no change", so clearing every role to "None" must still send `{}`
 * to actually clear them — omitting the field would silently leave the old roles in place.
 */
export function buildRolesMap(
    existingRoles: Record<string, string> | undefined,
    apiRole: string,
    applicationRole: string,
    apiProductRole: string,
): Record<string, string> {
    const roles = { ...(existingRoles ?? {}) };
    if (apiRole) {
        roles.API = apiRole;
    } else {
        delete roles.API;
    }
    if (applicationRole) {
        roles.APPLICATION = applicationRole;
    } else {
        delete roles.APPLICATION;
    }
    if (apiProductRole) {
        roles.API_PRODUCT = apiProductRole;
    } else {
        delete roles.API_PRODUCT;
    }
    return roles;
}

/** Parses the "Maximum members" text input — blank, non-positive, or non-numeric all mean unlimited (`null`). */
export function parseMaxInvitation(value: string): number | null {
    const trimmed = value.trim();
    if (!trimmed) return null;
    const parsed = Number(trimmed);
    return Number.isFinite(parsed) && parsed >= 1 ? parsed : null;
}
