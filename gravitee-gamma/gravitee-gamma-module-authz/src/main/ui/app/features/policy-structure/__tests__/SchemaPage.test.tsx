import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders as render } from '../../../../../../test/ui/render-with-providers';
import { SchemaPage } from '../SchemaPage';

const getSchemaSpy = vi.fn();

vi.mock('../../../../lib/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        getSchema: (env: string) => getSchemaSpy(env),
    },
}));

const editorStub = {
    revealLineInCenter: vi.fn(),
    setPosition: vi.fn(),
    focus: vi.fn(),
};

vi.mock('../../../../components/MonacoEditor', () => ({
    MonacoEditor: ({
        value,
        ariaLabel,
        onMount,
    }: {
        value: string;
        ariaLabel?: string;
        onMount?: (editor: typeof editorStub) => void;
    }) => {
        if (onMount) onMount(editorStub);
        return <textarea aria-label={ariaLabel} value={value} readOnly />;
    },
}));

beforeEach(() => {
    getSchemaSpy.mockReset();
    editorStub.revealLineInCenter.mockReset();
    editorStub.setPosition.mockReset();
    editorStub.focus.mockReset();
});

describe('SchemaPage', () => {
    it('renders existing schema and outline', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText: 'entity User { name: String };\n',
            updatedAt: '2026-04-24T10:00:00Z',
        });

        render(<SchemaPage />);

        await waitFor(() =>
            expect((screen.getByLabelText(/gapl schema viewer/i) as HTMLTextAreaElement).value).toContain('entity User'),
        );
        await waitFor(() => expect(screen.getByText('User')).toBeInTheDocument());
    });

    it('shows empty state on 404', async () => {
        const { ApiError } = await import('../../../../lib/api/authz-api-client');
        getSchemaSpy.mockRejectedValue(new ApiError(404, 'Not found'));

        render(<SchemaPage />);

        await waitFor(() => expect(screen.getByText(/schema not available/i)).toBeInTheDocument());
        expect((screen.getByLabelText(/gapl schema viewer/i) as HTMLTextAreaElement).value).toBe('');
    });

    it('does not render a Save, Reset, Delete or Validate button', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText: 'entity User {};\n',
            updatedAt: '2026-04-24T10:00:00Z',
        });

        render(<SchemaPage />);

        await waitFor(() =>
            expect((screen.getByLabelText(/gapl schema viewer/i) as HTMLTextAreaElement).value).toContain('entity User'),
        );

        expect(screen.queryByRole('button', { name: /save/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /reset/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /delete/i })).toBeNull();
        expect(screen.queryByRole('button', { name: /validate/i })).toBeNull();
    });

    it('calls editor.revealLineInCenter when outline button is clicked', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText: 'entity User { name: String };\n',
            updatedAt: '2026-04-24T10:00:00Z',
        });

        render(<SchemaPage />);

        const userButton = await screen.findByText('User');
        await userEvent.click(userButton);

        expect(editorStub.revealLineInCenter).toHaveBeenCalledWith(1);
        expect(editorStub.setPosition).toHaveBeenCalledWith({ lineNumber: 1, column: 1 });
        expect(editorStub.focus).toHaveBeenCalled();
    });

    it('renders four KPI badges with counts derived from the schema', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText:
                'entity User { name: String };\n' +
                'entity Group {};\n' +
                'entity API { name: String };\n' +
                'action "read" appliesTo { principal: [User], resource: [API] };\n',
            updatedAt: '2026-04-24T10:00:00Z',
        });

        render(<SchemaPage />);

        const editor = (await screen.findByLabelText(/gapl schema viewer/i)) as HTMLTextAreaElement;
        await waitFor(() => expect(editor.value).toContain('entity User'));

        expect(screen.getByText('entities')).toBeInTheDocument();
        expect(screen.getByText('actions')).toBeInTheDocument();
        expect(screen.getByText('principal kinds')).toBeInTheDocument();
        expect(screen.getByText('resource kinds')).toBeInTheDocument();

        const entitiesPill = screen.getByText('entities').closest('div');
        const actionsPill = screen.getByText('actions').closest('div');
        const principalsPill = screen.getByText('principal kinds').closest('div');
        const resourcesPill = screen.getByText('resource kinds').closest('div');

        expect(entitiesPill).toHaveTextContent('3');
        expect(actionsPill).toHaveTextContent('1');
        expect(principalsPill).toHaveTextContent('2');
        expect(resourcesPill).toHaveTextContent('1');
    });

    it('Export downloads the current schema as schema.gapl', async () => {
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText: 'entity User {};\n',
            updatedAt: '2026-04-24T10:00:00Z',
        });

        const createObjectURL = vi.fn().mockReturnValue('blob:mock-url');
        const revokeObjectURL = vi.fn();
        const originalCreate = URL.createObjectURL;
        const originalRevoke = URL.revokeObjectURL;
        Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createObjectURL });
        Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeObjectURL });

        const clickSpy = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

        try {
            render(<SchemaPage />);

            const editor = (await screen.findByLabelText(/gapl schema viewer/i)) as HTMLTextAreaElement;
            await waitFor(() => expect(editor.value).toContain('entity User'));

            await userEvent.click(screen.getByRole('button', { name: /export/i }));

            expect(createObjectURL).toHaveBeenCalledTimes(1);
            const blobArg = createObjectURL.mock.calls[0][0] as Blob;
            expect(blobArg).toBeInstanceOf(Blob);
            await expect(blobArg.text()).resolves.toContain('entity User');

            expect(clickSpy).toHaveBeenCalledTimes(1);
            expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
        } finally {
            clickSpy.mockRestore();
            Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: originalCreate });
            Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: originalRevoke });
        }
    });
});
