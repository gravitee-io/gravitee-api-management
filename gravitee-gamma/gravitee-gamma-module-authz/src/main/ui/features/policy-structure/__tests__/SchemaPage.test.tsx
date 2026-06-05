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
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ParsedSchema } from '../../../shared/engine-schema';
import type { UseParsedSchemaResult } from '../../../shared/hooks/useParsedSchema';
import type { UseSchemaResult } from '../../../shared/hooks/useSchema';
import type { UseSchemaValidationResult } from '../../../shared/hooks/useSchemaValidation';
import { SchemaPage } from '../SchemaPage';

const useSchemaMock = vi.fn<[], UseSchemaResult>();
const useParsedSchemaMock = vi.fn<[], UseParsedSchemaResult>();
const useSchemaValidationMock = vi.fn<[], UseSchemaValidationResult>();

vi.mock('@gravitee/gamma-modules-sdk', async importOriginal => ({
    ...(await importOriginal<object>()),
    useEnvironment: () => ({ id: 'DEFAULT' }),
}));

vi.mock('../../../shared/hooks/useSchema', () => ({
    useSchema: () => useSchemaMock(),
}));

vi.mock('../../../shared/hooks/useParsedSchema', () => ({
    useParsedSchema: () => useParsedSchemaMock(),
}));

vi.mock('../../../shared/hooks/useSchemaValidation', () => ({
    useSchemaValidation: () => useSchemaValidationMock(),
}));

const mutateMock = vi.fn();
const deleteMock = vi.fn();
let updateState: { mutate: () => void; isPending: boolean; isError: boolean; error: unknown };
vi.mock('../../../shared/hooks/useUpdateSchema', () => ({ useUpdateSchema: () => updateState }));
vi.mock('../../../shared/hooks/useDeleteSchema', () => ({ useDeleteSchema: () => ({ mutate: deleteMock, isPending: false }) }));

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

const SAMPLE_PARSED: ParsedSchema = {
    entities: [
        { name: 'Group', parents: [], attributes: [{ name: 'name', type: 'String' }] },
        { name: 'User', parents: ['Group'], attributes: [{ name: 'name', type: 'String' }, { name: 'email', type: 'String' }] },
        { name: 'MCPServer', parents: [], attributes: [{ name: 'url', type: 'String' }] },
    ],
    actions: [{ name: 'can_invoke', principals: ['User'], resources: ['MCPServer'] }],
};

function loaded(schemaText = SAMPLE): UseSchemaResult {
    return { schema: { schemaText }, notFound: false, isLoading: false, error: undefined } as UseSchemaResult;
}

beforeAll(() => {
    if (!Element.prototype.scrollIntoView) {
        Element.prototype.scrollIntoView = () => undefined;
    }
});

beforeEach(() => {
    useSchemaMock.mockReset();
    useSchemaMock.mockReturnValue(loaded());
    useParsedSchemaMock.mockReset();
    useParsedSchemaMock.mockReturnValue({ parsed: SAMPLE_PARSED, isLoading: false, error: undefined });
    useSchemaValidationMock.mockReset();
    useSchemaValidationMock.mockReturnValue({ errors: [], validating: false });
    mutateMock.mockReset();
    deleteMock.mockReset();
    updateState = { mutate: mutateMock, isPending: false, isError: false, error: undefined };
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
        expect(screen.getByLabelText('Entities')).toHaveTextContent('3');
        expect(screen.getByLabelText('Actions')).toHaveTextContent('1');
        expect(screen.getByLabelText('Principal kinds')).toHaveTextContent('2');
        expect(screen.getByLabelText('Resource kinds')).toHaveTextContent('1');
    });

    it('classifies custom-named types via appliesTo, not the built-in name map', () => {
        useParsedSchemaMock.mockReturnValue({
            parsed: {
                entities: [
                    { name: 'Subject', parents: [], attributes: [] },
                    { name: 'Report', parents: [], attributes: [] },
                ],
                actions: [{ name: 'read', principals: ['Subject'], resources: ['Report'] }],
            },
            isLoading: false,
            error: undefined,
        });
        render(<SchemaPage />);
        // Subject is used as a principal, Report as a resource — so they are NOT "Custom".
        expect(screen.getByLabelText('Principal kinds')).toHaveTextContent('1');
        expect(screen.getByLabelText('Resource kinds')).toHaveTextContent('1');
        expect(screen.getByText('Principals')).toBeInTheDocument();
        expect(screen.getByText('Resources')).toBeInTheDocument();
        expect(screen.queryByText('Custom')).not.toBeInTheDocument();
    });

    it('qualifies namespaced types in the outline', () => {
        useParsedSchemaMock.mockReturnValue({
            parsed: {
                entities: [{ name: 'myapp::Report', parents: [], attributes: [] }],
                actions: [{ name: 'read', principals: [], resources: ['myapp::Report'] }],
            },
            isLoading: false,
            error: undefined,
        });
        render(<SchemaPage />);
        expect(screen.getByRole('button', { name: 'myapp::Report' })).toBeInTheDocument();
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
        // The actions count is still surfaced as a summary KPI tile.
        expect(screen.getByLabelText('Actions')).toHaveTextContent('1');
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

    it('edits and saves a valid schema', () => {
        useSchemaMock.mockReturnValue(loaded('entity User {};'));
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /edit/i }));
        fireEvent.click(screen.getByRole('button', { name: /save/i }));
        expect(mutateMock).toHaveBeenCalledWith('entity User {};', expect.objectContaining({ onSuccess: expect.any(Function) }));
    });

    it('deletes the schema when Delete is clicked', () => {
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /delete/i }));
        expect(deleteMock).toHaveBeenCalledTimes(1);
    });

    it('cancels editing without saving', () => {
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /edit/i }));
        fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
        expect(screen.queryByRole('button', { name: /save/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /cancel/i })).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('opens the editor in create mode with Save disabled for a blank draft', () => {
        useSchemaMock.mockReturnValue({ schema: null, notFound: true, isLoading: false, error: undefined });
        useSchemaValidationMock.mockReturnValue({ errors: ['Schema must not be empty.'], validating: false });
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /create schema/i }));
        expect(screen.getByTestId('monaco')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
        expect(mutateMock).not.toHaveBeenCalled();
    });

    it('disables save when the draft has diagnostics', () => {
        useSchemaMock.mockReturnValue(loaded('action "x" { principal: [User] };'));
        useSchemaValidationMock.mockReturnValue({ errors: ["missing 'appliesTo'"], validating: false });
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /edit/i }));
        expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
    });

    it('disables save while a draft validation is still in flight', () => {
        useSchemaValidationMock.mockReturnValue({ errors: [], validating: true });
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /edit/i }));
        expect(screen.getByRole('button', { name: /save/i })).toBeDisabled();
    });

    it('surfaces a backend error when the save is rejected', () => {
        updateState = { mutate: mutateMock, isPending: false, isError: true, error: new Error("line 1:7 extraneous input '{'") };
        render(<SchemaPage />);
        fireEvent.click(screen.getByRole('button', { name: /edit/i }));
        expect(screen.getByText('Could not save schema')).toBeInTheDocument();
        expect(screen.getByText("line 1:7 extraneous input '{'")).toBeInTheDocument();
    });

    it('offers create when no schema exists', () => {
        useSchemaMock.mockReturnValue({ schema: null, notFound: true, isLoading: false, error: undefined });
        render(<SchemaPage />);
        expect(screen.getByRole('button', { name: /create schema/i })).toBeInTheDocument();
    });
});
