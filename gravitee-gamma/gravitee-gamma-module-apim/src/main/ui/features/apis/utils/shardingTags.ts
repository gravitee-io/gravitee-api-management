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

export interface ShardingTagSelectability {
    /** Sharding tags carried by the parent API / API Product (the subset a plan may draw from). */
    allowedTags: string[];
    /** Tag ids the current user is allowed to assign. */
    userTags: string[];
    /** Whole control is read-only (no permission / closed plan). */
    readOnly?: boolean;
}

/**
 * Whether a sharding tag must be shown disabled on a plan form.
 *
 * Mirrors the classic console plan general step: a plan tag must belong to the parent entity's
 * tags (backend subset rule `validatePlanTagsAgainstApiTags` / `…ApiProductTags`) AND be usable
 * by the current user (`GET {org}/user/tags`). The whole control is also disabled when read-only.
 */
export function isShardingTagDisabled(tagId: string, { allowedTags, userTags, readOnly = false }: ShardingTagSelectability): boolean {
    return readOnly || !allowedTags.includes(tagId) || !userTags.includes(tagId);
}
