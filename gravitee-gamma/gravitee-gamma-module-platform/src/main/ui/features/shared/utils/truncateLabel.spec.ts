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
import { OVERVIEW_DESCRIPTION_MAX_LENGTH, truncateLabel } from './truncateLabel';

describe('truncateLabel', () => {
    it('returns the label unchanged when within the limit', () => {
        expect(truncateLabel('Short name')).toBe('Short name');
    });

    it('truncates with an ellipsis when over the limit', () => {
        const long = 'a'.repeat(60);
        expect(truncateLabel(long, 48)).toHaveLength(48);
        expect(truncateLabel(long, 48).endsWith('…')).toBe(true);
    });

    it('truncates overview descriptions at the dedicated limit', () => {
        const long = 'b'.repeat(OVERVIEW_DESCRIPTION_MAX_LENGTH + 10);
        expect(truncateLabel(long, OVERVIEW_DESCRIPTION_MAX_LENGTH)).toHaveLength(OVERVIEW_DESCRIPTION_MAX_LENGTH);
        expect(truncateLabel(long, OVERVIEW_DESCRIPTION_MAX_LENGTH).endsWith('…')).toBe(true);
    });
});
