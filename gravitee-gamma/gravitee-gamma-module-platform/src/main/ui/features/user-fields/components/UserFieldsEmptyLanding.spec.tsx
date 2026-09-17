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
import { render, screen } from '@testing-library/react';

import { UserFieldsEmptyLanding } from './UserFieldsEmptyLanding';

describe('UserFieldsEmptyLanding', () => {
    it('explains why a user field is worth creating', () => {
        render(<UserFieldsEmptyLanding />);

        expect(screen.getByRole('heading', { name: 'Why create a user field?' })).not.toBeNull();
        expect(screen.getByText('Without user fields')).not.toBeNull();
        expect(screen.getByText('With user fields')).not.toBeNull();
        expect(screen.getByText('Ask on registration')).not.toBeNull();
        expect(screen.getByText('Constrain the answers')).not.toBeNull();
    });

    it('stacks the comparison and feature-tile rows on narrow viewports', () => {
        const { container } = render(<UserFieldsEmptyLanding />);

        expect(container.querySelectorAll('.flex-col.md\\:flex-row').length).toBe(2);
    });
});
