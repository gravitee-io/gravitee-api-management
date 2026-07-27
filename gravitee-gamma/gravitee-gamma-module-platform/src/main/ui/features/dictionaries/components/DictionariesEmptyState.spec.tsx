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

import { DictionariesEmptyState } from './DictionariesEmptyState';

describe('DictionariesEmptyState', () => {
    it('renders the why/how-it-works/benefits content', () => {
        render(<DictionariesEmptyState />);

        expect(screen.getByText('Why configure dictionaries?')).not.toBeNull();
        expect(screen.getByText('How it works')).not.toBeNull();
        expect(screen.getByText('Dictionary')).not.toBeNull();
        expect(screen.getByText('Referenced by policies')).not.toBeNull();
        expect(screen.getByText('Reference shared lookup data from any API policy in this environment')).not.toBeNull();
        expect(screen.getByText(/Dynamic dictionaries poll an HTTP provider/)).not.toBeNull();
    });
});
