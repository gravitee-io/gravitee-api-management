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
import { isShardingTagDisabled } from './shardingTags';

describe('isShardingTagDisabled (plan sharding tag subset + user rule)', () => {
    const allowedTags = ['public', 'private'];
    const userTags = ['public', 'private', 'internal'];

    it('enables a tag that is both on the parent entity and usable by the user', () => {
        expect(isShardingTagDisabled('public', { allowedTags, userTags })).toBe(false);
    });

    it('disables a tag not assigned to the parent entity (subset rule)', () => {
        // 'internal' is usable by the user but the parent API/Product does not carry it.
        expect(isShardingTagDisabled('internal', { allowedTags, userTags })).toBe(true);
    });

    it('disables a tag the current user is not allowed to use', () => {
        // 'private' is on the parent but the user lacks access to it.
        expect(isShardingTagDisabled('private', { allowedTags, userTags: ['public'] })).toBe(true);
    });

    it('disables every tag when the parent entity has no tags', () => {
        expect(isShardingTagDisabled('public', { allowedTags: [], userTags })).toBe(true);
    });

    it('disables every tag when the control is read-only, even when otherwise selectable', () => {
        expect(isShardingTagDisabled('public', { allowedTags, userTags, readOnly: true })).toBe(true);
    });
});
