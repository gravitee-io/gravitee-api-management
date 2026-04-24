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
import { getPrimaryHrid, resolveEnvironmentFromSegment, shouldRewriteIdToHrid } from './environment.utils';
import { buildEnvironment, TEST_ENVIRONMENTS } from '../../testing/factories';

describe('environment.utils', () => {
    describe('getPrimaryHrid', () => {
        it('should return first hrid when present', () => {
            expect(getPrimaryHrid(buildEnvironment({ hrids: ['a', 'b'] }))).toBe('a');
        });

        it('should fall back to id when no hrids', () => {
            expect(getPrimaryHrid(buildEnvironment({ id: 'only-id', hrids: [] }))).toBe('only-id');
        });
    });

    describe('resolveEnvironmentFromSegment', () => {
        it('should match hrid case-insensitively', () => {
            const env = resolveEnvironmentFromSegment(TEST_ENVIRONMENTS, 'ENV-1');
            expect(env?.id).toBe('env-1-id');
        });

        it('should return null for empty segment', () => {
            expect(resolveEnvironmentFromSegment(TEST_ENVIRONMENTS, '')).toBeNull();
        });
    });

    describe('shouldRewriteIdToHrid', () => {
        it('should be true when segment is id and hrids exist', () => {
            const env = buildEnvironment({ id: 'abc', hrids: ['x'] });
            expect(shouldRewriteIdToHrid(env, 'abc')).toBe(true);
        });

        it('should be false when URL already uses primary hrid', () => {
            const env = buildEnvironment({ id: 'abc', hrids: ['x'] });
            expect(shouldRewriteIdToHrid(env, 'x')).toBe(false);
        });
    });
});
