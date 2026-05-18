import { TooltipProvider } from '@gravitee/graphene-core';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders as render } from '../../../../../../test/ui/render-with-providers';
import { EntitiesPage } from '../EntitiesPage';

const listSpy = vi.fn();
const createSpy = vi.fn();
const deleteSpy = vi.fn();
const listPoliciesSpy = vi.fn();
const getSchemaSpy = vi.fn();

vi.mock('../../../../lib/api/authz-api.service', () => ({
    DEFAULT_PER_PAGE: 10,
    authzApiService: {
        listEntities: (env: string, params?: unknown) => listSpy(env, params),
        createEntity: (env: string, req: unknown) => createSpy(env, req),
        updateEntity: vi.fn(),
        deleteEntity: (env: string, id: string) => deleteSpy(env, id),
        listPolicies: (env: string, params?: unknown) => listPoliciesSpy(env, params),
        getSchema: (env: string) => getSchemaSpy(env),
    },
}));

function renderPage() {
    return render(
        <TooltipProvider>
            <EntitiesPage />
        </TooltipProvider>,
    );
}

beforeEach(() => {
    listSpy.mockReset();
    createSpy.mockReset();
    deleteSpy.mockReset();
    listPoliciesSpy.mockReset();
    getSchemaSpy.mockReset();
    // Default: no policies loaded — most tests don't care about cross-refs.
    listPoliciesSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 1000 });
    // Default: empty schema — KPI bar shows 0 for action / kind counts.
    getSchemaSpy.mockResolvedValue({ environmentId: 'DEFAULT', schemaText: '', updatedAt: '' });
    vi.spyOn(window, 'confirm').mockReturnValue(true);
});

describe('EntitiesPage', () => {
    it('renders empty state when no entities', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });

        renderPage();

        await waitFor(() => expect(screen.getByText(/no principals yet/i)).toBeInTheDocument());
    });

    it('renders entity rows after load', async () => {
        listSpy.mockResolvedValue({
            data: [
                {
                    id: '1',
                    environmentId: 'DEFAULT',
                    uid: 'User::"alice"',
                    attributes: { name: 'Alice' },
                    parents: [],
                    createdAt: '2025-01-01T00:00:00.000Z',
                    updatedAt: '2025-01-01T00:00:00.000Z',
                },
            ],
            total: 1,
            page: 1,
            perPage: 10,
        });

        renderPage();

        // The entity id 'alice' should appear in the table
        await waitFor(() => expect(screen.getAllByText('alice')[0]).toBeInTheDocument());
        // The type badge should be present
        expect(screen.getAllByText('User')[0]).toBeInTheDocument();
    });

    it('shows KPI badge bar with counts derived from entities + schema', async () => {
        listSpy.mockResolvedValue({
            data: [
                {
                    id: '1',
                    environmentId: 'DEFAULT',
                    uid: 'User::"alice"',
                    attributes: {},
                    parents: [],
                    createdAt: '',
                    updatedAt: '',
                },
            ],
            total: 7,
            page: 1,
            perPage: 10,
        });
        // Schema: 2 actions, 1 principal kind (User), 1 resource kind (MCPServer).
        getSchemaSpy.mockResolvedValue({
            environmentId: 'DEFAULT',
            schemaText: [
                'entity User { name: String };',
                'entity MCPServer { name: String };',
                'action "read"  appliesTo { principal: [User], resource: [MCPServer] };',
                'action "write" appliesTo { principal: [User], resource: [MCPServer] };',
            ].join('\n'),
            updatedAt: '',
        });

        renderPage();

        const bar = await screen.findByLabelText(/entity statistics/i);
        expect(bar).toHaveTextContent(/7\s*entities/);
        expect(bar).toHaveTextContent(/2\s*actions/);
        expect(bar).toHaveTextContent(/1\s*principal kinds/);
        expect(bar).toHaveTextContent(/1\s*resource kinds/);
    });

    it('falls back to zero schema-derived counts when schema is missing', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });
        getSchemaSpy.mockResolvedValue({ environmentId: 'DEFAULT', schemaText: '', updatedAt: '' });

        renderPage();

        const bar = await screen.findByLabelText(/entity statistics/i);
        expect(bar).toHaveTextContent(/0\s*actions/);
        expect(bar).toHaveTextContent(/0\s*principal kinds/);
        expect(bar).toHaveTextContent(/0\s*resource kinds/);
    });

    it('switches between Principals, Resources and entities.json tabs', async () => {
        listSpy.mockResolvedValue({
            data: [
                {
                    id: 'e1',
                    environmentId: 'DEFAULT',
                    uid: 'User::"alice"',
                    attributes: { name: 'Alice' },
                    parents: [],
                    createdAt: '',
                    updatedAt: '',
                },
                {
                    id: 'e2',
                    environmentId: 'DEFAULT',
                    uid: 'MCPServer::"flights-mcp"',
                    attributes: { name: 'Flights MCP' },
                    parents: [],
                    createdAt: '',
                    updatedAt: '',
                },
            ],
            total: 2,
            page: 1,
            perPage: 10,
        });

        renderPage();

        // Default: Principals tab — alice visible, flights-mcp hidden.
        await waitFor(() => expect(screen.getAllByText('alice')[0]).toBeInTheDocument());
        expect(screen.queryByText('flights-mcp')).not.toBeInTheDocument();

        // Switch to Resources tab.
        await userEvent.click(screen.getByRole('tab', { name: /resources/i }));
        await waitFor(() => expect(screen.getAllByText('flights-mcp')[0]).toBeInTheDocument());

        // Switch to entities.json tab — JSON dump replaces the table.
        await userEvent.click(screen.getByRole('tab', { name: /entities\.json/i }));
        await waitFor(() => expect(screen.getByText(/read-only snapshot/i)).toBeInTheDocument());
        // Highlighted JSON contains the entity uid string.
        expect(screen.getByText(/"alice"/)).toBeInTheDocument();
        expect(screen.getByText(/"flights-mcp"/)).toBeInTheDocument();
    });

    it('opens Add Principal dialog on header button click', async () => {
        listSpy.mockResolvedValue({ data: [], total: 0, page: 1, perPage: 10 });

        renderPage();
        await waitFor(() => screen.getByText(/no principals yet/i));

        // Click the Add Principal header button (first occurrence — in the page header)
        const btns = screen.getAllByRole('button', { name: /add principal/i });
        await userEvent.click(btns[0]);

        // Dialog title
        await waitFor(() => expect(screen.getByRole('heading', { name: /add local principal/i })).toBeInTheDocument());
    });

    it('shows error alert on load failure', async () => {
        listSpy.mockRejectedValue(new Error('Network error'));

        renderPage();
        await waitFor(() => expect(screen.getByText(/could not load entities/i)).toBeInTheDocument());
        expect(screen.getByText(/network error/i)).toBeInTheDocument();
    });

    it('shows the count of policies that reference each entity', async () => {
        listSpy.mockResolvedValue({
            data: [
                {
                    id: 'e1',
                    environmentId: 'DEFAULT',
                    uid: 'User::"alice"',
                    attributes: { name: 'Alice' },
                    parents: [],
                    createdAt: '',
                    updatedAt: '',
                },
            ],
            total: 1,
            page: 1,
            perPage: 10,
        });
        listPoliciesSpy.mockResolvedValue({
            data: [
                {
                    id: 'p1',
                    environmentId: 'DEFAULT',
                    name: 'Read access',
                    description: null,
                    policyText: 'permit ( principal == User::"alice", action == action::"read" );',
                    type: 'API',
                    target: null,
                    status: 'DRAFT',
                    createdAt: '',
                    updatedAt: '',
                },
            ],
            total: 1,
            page: 1,
            perPage: 1000,
        });

        renderPage();

        // Wait for entity row to appear first.
        await waitFor(() => expect(screen.getAllByText('alice')[0]).toBeInTheDocument());
        // Then the policy badge button rendered by PoliciesCell ("policy" singular).
        await waitFor(() => {
            expect(screen.getByRole('button', { name: /1\s*policy/i })).toBeInTheDocument();
        });
    });
});
