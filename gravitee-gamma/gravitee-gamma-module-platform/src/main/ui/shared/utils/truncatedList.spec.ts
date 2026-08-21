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

import { formatTruncatedNameSummary } from './truncatedList';

describe('formatTruncatedNameSummary', () => {
    it('shows the first three names then an ellipsis when there are more', () => {
        expect(formatTruncatedNameSummary(['USER', 'ORG_TEST1', 'ORG_TEST2', 'ADMIN'])).toEqual({
            display: 'USER, ORG_TEST1, ORG_TEST2...',
            full: 'USER, ORG_TEST1, ORG_TEST2, ADMIN',
            truncated: true,
        });
    });

    it('shows every name when there are three or fewer', () => {
        expect(formatTruncatedNameSummary(['USER', 'ADMIN'])).toEqual({
            display: 'USER, ADMIN',
            full: 'USER, ADMIN',
            truncated: false,
        });
    });
});
