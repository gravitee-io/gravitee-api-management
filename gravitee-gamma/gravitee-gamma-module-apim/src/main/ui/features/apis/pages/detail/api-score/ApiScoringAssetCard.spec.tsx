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

import { ApiScoringAssetCard } from './ApiScoringAssetCard';
import type { ScoringAsset, ScoringDiagnostic } from '../../../types/scoring';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

function diagnostic(): ScoringDiagnostic {
    return {
        severity: 'ERROR',
        message: 'Operation is missing a security requirement.',
        range: { start: { line: 1, character: 0 }, end: { line: 1, character: 1 } },
        path: '$.info',
    };
}

function asset(overrides: Partial<ScoringAsset> = {}): ScoringAsset {
    return {
        name: 'petstore.yaml',
        type: 'SWAGGER',
        diagnostics: [],
        ...overrides,
    };
}

describe('ApiScoringAssetCard', () => {
    it('opens when diagnostics appear after a re-evaluation or filter change', () => {
        const { rerender } = render(<ApiScoringAssetCard asset={asset()} />);
        expect(screen.getByRole('button', { name: /petstore.yaml/i })).toHaveAttribute('aria-expanded', 'false');

        rerender(<ApiScoringAssetCard asset={asset({ diagnostics: [diagnostic()] })} />);
        expect(screen.getByRole('button', { name: /petstore.yaml/i })).toHaveAttribute('aria-expanded', 'true');
    });
});
