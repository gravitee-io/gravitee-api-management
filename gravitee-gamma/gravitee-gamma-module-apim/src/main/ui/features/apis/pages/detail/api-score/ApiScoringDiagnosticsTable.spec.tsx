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
import { fireEvent, render, screen } from '@testing-library/react';

import { ApiScoringDiagnosticsTable } from './ApiScoringDiagnosticsTable';
import type { ScoringDiagnostic } from '../../../types/scoring';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

function row(message: string, overrides: Partial<ScoringDiagnostic> = {}): ScoringDiagnostic {
    return {
        severity: 'ERROR',
        message,
        range: { start: { line: 1, character: 0 }, end: { line: 1, character: 1 } },
        path: '$.info',
        ...overrides,
    };
}

describe('ApiScoringDiagnosticsTable', () => {
    it('searches across severity, recommendation, and path', () => {
        render(
            <ApiScoringDiagnosticsTable
                ariaLabel="diagnostics"
                diagnostics={[
                    row('Operation is missing a security requirement.', { path: '$.paths./pet.get' }),
                    row('Add an example', { severity: 'INFO', path: '$.components.schemas.Pet' }),
                ]}
            />,
        );
        expect(screen.getByText('Operation is missing a security requirement.')).toBeInTheDocument();
        fireEvent.change(screen.getByPlaceholderText('Search severity, recommendation, or path...'), { target: { value: 'example' } });
        expect(screen.queryByText('Operation is missing a security requirement.')).not.toBeInTheDocument();
        expect(screen.getByText('Add an example')).toBeInTheDocument();
    });

    it('paginates at 5 rows per page', () => {
        render(
            <ApiScoringDiagnosticsTable
                ariaLabel="diagnostics"
                diagnostics={[row('one'), row('two'), row('three'), row('four'), row('five'), row('six')]}
            />,
        );
        expect(screen.getByText('one')).toBeInTheDocument();
        expect(screen.getByText('five')).toBeInTheDocument();
        expect(screen.queryByText('six')).not.toBeInTheDocument();
        expect(screen.getByText(/1-5 of 6/i)).toBeInTheDocument();
    });

    it('clamps to the last page when diagnostics shrink below the current page', () => {
        const many = Array.from({ length: 12 }, (_, index) => row(`finding-${index + 1}`));
        const { rerender } = render(<ApiScoringDiagnosticsTable ariaLabel="diagnostics" diagnostics={many} />);

        fireEvent.click(screen.getByRole('button', { name: /next page/i }));
        fireEvent.click(screen.getByRole('button', { name: /next page/i }));
        expect(screen.getByText('finding-11')).toBeInTheDocument();
        expect(screen.queryByText('finding-1')).not.toBeInTheDocument();

        rerender(<ApiScoringDiagnosticsTable ariaLabel="diagnostics" diagnostics={[row('only-error')]} />);
        expect(screen.getByText('only-error')).toBeInTheDocument();
        expect(screen.queryByText('No diagnostics match the current search.')).not.toBeInTheDocument();
    });

    it('sorts by Severity and Path', () => {
        render(
            <ApiScoringDiagnosticsTable
                ariaLabel="diagnostics"
                diagnostics={[
                    row('warn-finding', { severity: 'WARN', path: '$.z' }),
                    row('error-finding', { severity: 'ERROR', path: '$.a' }),
                ]}
            />,
        );

        const messages = () => screen.getAllByText(/-finding$/).map(node => node.textContent);
        expect(messages()).toEqual(['warn-finding', 'error-finding']);

        fireEvent.click(screen.getByRole('button', { name: /^severity$/i }));
        expect(messages()).toEqual(['error-finding', 'warn-finding']);

        fireEvent.click(screen.getByRole('button', { name: /^path$/i }));
        expect(messages()).toEqual(['error-finding', 'warn-finding']);
    });

    it('omits the search empty description when the table is empty without a search', () => {
        render(<ApiScoringDiagnosticsTable ariaLabel="diagnostics" diagnostics={[]} />);
        expect(screen.getByText('No data to display')).toBeInTheDocument();
        expect(screen.queryByText('No diagnostics match the current search.')).not.toBeInTheDocument();
    });

    it('explains a search miss when the search has no matches', () => {
        render(<ApiScoringDiagnosticsTable ariaLabel="diagnostics" diagnostics={[row('missing security')]} />);
        fireEvent.change(screen.getByPlaceholderText('Search severity, recommendation, or path...'), { target: { value: 'nope' } });
        expect(screen.getByText('No data to display')).toBeInTheDocument();
        expect(screen.getByText('No diagnostics match the current search.')).toBeInTheDocument();
    });
});
