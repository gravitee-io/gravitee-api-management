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

import { ApiScoringEducationalEmptyState } from './ApiScoringEducationalEmptyState';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

describe('ApiScoringEducationalEmptyState', () => {
    it('renders educational copy and the How it works flow', () => {
        render(<ApiScoringEducationalEmptyState />);

        expect(screen.getByTestId('api-scoring-educational-empty')).not.toBeNull();
        expect(screen.getByText('Why run API Score?')).not.toBeNull();
        expect(screen.getByText('How it works')).not.toBeNull();
        expect(screen.getByText(/Click/)).not.toBeNull();
        expect(screen.getByText('Evaluate')).not.toBeNull();
    });
});
