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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { UseSchemaResult } from '../../../shared/hooks/useSchema';
import { SchemaPage } from '../SchemaPage';

const useSchemaMock = vi.fn<[], UseSchemaResult>();

vi.mock('@gravitee/gamma-modules-sdk', async importOriginal => ({
    ...(await importOriginal<object>()),
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

vi.mock('../../../shared/hooks/useSchema', () => ({
    useSchema: () => useSchemaMock(),
}));

// Monaco can't render under jsdom — stub it to surface the value as plain text.
vi.mock('../../../components/MonacoEditor', () => ({
    MonacoEditor: ({ value }: { value: string }) => <pre data-testid="monaco">{value}</pre>,
}));

const SAMPLE = `entity Group {
  name: String
};
entity User in [Group] {
  name: String,
  email: String
};
entity MCPServer {
  url: String
};
action "can_invoke" appliesTo {
  principal: [User],
  resource: [MCPServer]
};`;

function loaded(schemaText = SAMPLE): UseSchemaResult {
    return { schema: { schemaText }, notFound: false, isLoading: false, error: undefined };
}

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

beforeEach(() => {
    useSchemaMock.mockReset();
    useSchemaMock.mockReturnValue(loaded());
});

describe('SchemaPage', () => {
    it('renders an error alert when the schema query fails', () => {
        useSchemaMock.mockReturnValue({ schema: null, notFound: false, isLoading: false, error: 'boom' });
        render(<SchemaPage />);
        expect(screen.getByText('Could not load schema')).toBeInTheDocument();
        expect(screen.getByText('boom')).toBeInTheDocument();
    });

    it('shows the empty state when no schema is defined', () => {
        useSchemaMock.mockReturnValue({ schema: null, notFound: true, isLoading: false, error: undefined });
        render(<SchemaPage />);
        expect(screen.getByText('No schema defined yet')).toBeInTheDocument();
    });

    it('renders summary chips computed from the parsed schema', () => {
        render(<SchemaPage />);
        const summary = screen.getByLabelText('Schema summary');
        expect(summary).toHaveTextContent('3 entities');
        expect(summary).toHaveTextContent('1 actions');
        expect(summary).toHaveTextContent('2 principal kinds');
        expect(summary).toHaveTextContent('1 resource kinds');
    });

    it('surfaces parser diagnostics instead of silently looking empty', () => {
        useSchemaMock.mockReturnValue(loaded('entity {'));
        render(<SchemaPage />);
        expect(screen.getByText('Schema could not be fully parsed')).toBeInTheDocument();
    });

    it('does not show the diagnostics alert for a well-formed schema', () => {
        render(<SchemaPage />);
        expect(screen.queryByText('Schema could not be fully parsed')).not.toBeInTheDocument();
    });

    it('shows the raw schema in the code tab by default (read-only)', () => {
        render(<SchemaPage />);
        expect(screen.getByTestId('monaco')).toHaveTextContent('entity User in [Group]');
    });

    it('renders entity cards with attributes when the Entities tab is selected', async () => {
        const user = userEvent.setup();
        render(<SchemaPage />);

        await user.click(screen.getByRole('tab', { name: /Entities/i }));
        await waitFor(() => expect(screen.getByText('email: String')).toBeInTheDocument());
        expect(screen.getByText('url: String')).toBeInTheDocument();
    });

    it('does not render an Actions tab (actions have their own page)', () => {
        render(<SchemaPage />);
        expect(screen.queryByRole('tab', { name: /Actions/i })).not.toBeInTheDocument();
        // The actions count is still surfaced as a summary chip.
        expect(screen.getByLabelText('Schema summary')).toHaveTextContent('1 actions');
    });

    it('jumps from the outline to the entity in the Entities tab', async () => {
        const user = userEvent.setup();
        render(<SchemaPage />);

        // Code tab is active first → Entities cards not mounted yet.
        expect(screen.queryByText('url: String')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'MCPServer' }));

        // Outline click switches to the Entities tab and reveals the entity's attributes.
        await waitFor(() => expect(screen.getByText('url: String')).toBeInTheDocument());
    });
});
