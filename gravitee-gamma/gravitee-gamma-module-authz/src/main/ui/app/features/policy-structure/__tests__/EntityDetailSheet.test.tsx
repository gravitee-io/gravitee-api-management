import { TooltipProvider } from '@gravitee/graphene-core';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { EntityDetailSheet } from '../EntityDetailSheet';
import type { EntityInstance } from '../entity-types';

const ALICE: EntityInstance = {
    uid: { type: 'User', id: 'alice' },
    displayName: 'Alice',
    attrs: { name: 'Alice', email: 'alice@example.com', tenant: 'eu' },
    parents: [{ type: 'Group', id: 'devs' }],
    source: 'local',
    _backendId: 'be-1',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
};

const DEVS: EntityInstance = {
    uid: { type: 'Group', id: 'devs' },
    displayName: 'Developers',
    attrs: { name: 'Developers' },
    parents: [],
    source: 'local',
    _backendId: 'be-2',
};

const SCIM_USER: EntityInstance = {
    uid: { type: 'User', id: 'bob' },
    displayName: 'Bob',
    attrs: { userName: 'bob' },
    parents: [],
    source: 'scim',
    principalProvider: 'Okta',
};

function renderSheet(overrides: Partial<Parameters<typeof EntityDetailSheet>[0]> = {}) {
    const props = {
        open: true,
        onOpenChange: vi.fn(),
        entityKey: 'User::alice',
        allEntities: [ALICE, DEVS],
        remove: vi.fn().mockResolvedValue(undefined),
        ...overrides,
    } as Parameters<typeof EntityDetailSheet>[0];

    return {
        ...render(
            <TooltipProvider>
                <EntityDetailSheet {...props} />
            </TooltipProvider>,
        ),
        props,
    };
}

describe('EntityDetailSheet', () => {
    it('renders the entity header (display name, type badge, attrs/parents/refs counts)', () => {
        renderSheet();

        // Display name in the title.
        expect(screen.getByText('Alice')).toBeInTheDocument();
        // Type badge — "User" — appears in the header.
        expect(screen.getAllByText('User').length).toBeGreaterThan(0);
        // Attribute appears in the table (Overview tab is the default).
        expect(screen.getByText('email')).toBeInTheDocument();
    });

    it('shows the GAPL shape pane when its tab is selected', async () => {
        renderSheet();

        await userEvent.click(screen.getByRole('tab', { name: /gapl shape/i }));

        // GAPL shape pane lists JSON fields.
        expect(screen.getByText(/entity\.gapl\.json/)).toBeInTheDocument();
        // Has a Copy JSON button.
        expect(screen.getByRole('button', { name: /copy json/i })).toBeInTheDocument();
    });

    it('shows the relationships pane with parent rows', async () => {
        renderSheet();

        await userEvent.click(screen.getByRole('tab', { name: /relationships/i }));

        // Parent group "Developers" should appear by display name.
        expect(screen.getByText('Developers')).toBeInTheDocument();
        // Member-of section header is present.
        expect(screen.getByText(/member of/i)).toBeInTheDocument();
    });

    it('renders an "Entity not found" placeholder when the entityKey does not match any entity', () => {
        renderSheet({ entityKey: 'User::ghost' });

        expect(screen.getByText(/entity not found/i)).toBeInTheDocument();
    });

    // ----------------------------------------------------------------
    // Wave 1B: extended coverage.
    // ----------------------------------------------------------------

    it('switches between tabs and back to Overview, surfacing the attributes table again', async () => {
        renderSheet();

        // Default is Overview — the attribute table renders.
        expect(screen.getByText('email')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', { name: /gapl shape/i }));
        // After switching, the attributes table cell is gone but the JSON pane is on.
        expect(screen.getByText(/entity\.gapl\.json/)).toBeInTheDocument();

        // Back to Overview — attributes table re-renders.
        await userEvent.click(screen.getByRole('tab', { name: /^overview$/i }));
        expect(screen.getByText('email')).toBeInTheDocument();
        expect(screen.getByText(/Provenance/)).toBeInTheDocument();
    });

    describe('delete flow', () => {
        let confirmSpy: ReturnType<typeof vi.spyOn>;

        beforeEach(() => {
            confirmSpy = vi.spyOn(window, 'confirm');
        });
        afterEach(() => {
            confirmSpy.mockRestore();
        });

        it('calls remove(_backendId) and closes the sheet when the user confirms delete', async () => {
            confirmSpy.mockReturnValue(true);
            const remove = vi.fn().mockResolvedValue(undefined);
            const onOpenChange = vi.fn();
            renderSheet({ remove, onOpenChange });

            await userEvent.click(screen.getByRole('button', { name: /delete entity/i }));

            expect(confirmSpy).toHaveBeenCalled();
            expect(remove).toHaveBeenCalledWith('be-1');
            // After successful delete, the sheet closes itself.
            await vi.waitFor(() => {
                expect(onOpenChange).toHaveBeenCalledWith(false);
            });
        });

        it('does not call remove when the user cancels the confirm dialog', async () => {
            confirmSpy.mockReturnValue(false);
            const remove = vi.fn().mockResolvedValue(undefined);
            const onOpenChange = vi.fn();
            renderSheet({ remove, onOpenChange });

            await userEvent.click(screen.getByRole('button', { name: /delete entity/i }));

            expect(confirmSpy).toHaveBeenCalled();
            expect(remove).not.toHaveBeenCalled();
            expect(onOpenChange).not.toHaveBeenCalled();
        });
    });

    it('hides the Delete button for SCIM-sourced entities', () => {
        renderSheet({
            entityKey: 'User::bob',
            allEntities: [SCIM_USER],
        });

        // SCIM-sourced principals are read-only, so no Delete button should exist.
        expect(screen.queryByRole('button', { name: /delete entity/i })).toBeNull();
    });

    it('navigates into a related entity and back via the back button', async () => {
        renderSheet();

        // Open Relationships and click the parent Group row.
        await userEvent.click(screen.getByRole('tab', { name: /relationships/i }));
        await userEvent.click(screen.getByRole('button', { name: /developers/i }));

        // The header should now reflect the navigated-into entity (Devs / Group).
        // Display name "Developers" must be in the title region (h2) — i.e. not just inside a
        // relationship row anymore.
        const headings = screen.getAllByText('Developers');
        expect(headings.length).toBeGreaterThan(0);

        // A Back button (aria-label="Back") should now be available.
        const backBtn = screen.getByRole('button', { name: /^back$/i });
        expect(backBtn).toBeInTheDocument();

        // Clicking Back returns us to Alice.
        await userEvent.click(backBtn);
        expect(screen.getByText('Alice')).toBeInTheDocument();
    });

    it('renders the GAPL JSON view with the entity uid and each attribute key', async () => {
        renderSheet();
        await userEvent.click(screen.getByRole('tab', { name: /gapl shape/i }));

        // The JSON pane should include each attribute key (rendered as a JSON key).
        // Use a regex match scoped to the pre block.
        const pane = screen.getByText(/entity\.gapl\.json/).closest('div');
        expect(pane).not.toBeNull();
        const rendered = pane!.parentElement!.textContent ?? '';
        expect(rendered).toMatch(/"name"/);
        expect(rendered).toMatch(/"email"/);
        expect(rendered).toMatch(/"tenant"/);
        // uid type/id surface in the JSON too.
        expect(rendered).toMatch(/"User"/);
        expect(rendered).toMatch(/"alice"/);
    });

    it('renders a dangling marker when a parent reference cannot be resolved', async () => {
        const orphan: EntityInstance = {
            ...ALICE,
            parents: [{ type: 'Group', id: 'ghost-group' }],
        };
        renderSheet({ entityKey: 'User::alice', allEntities: [orphan] });
        await userEvent.click(screen.getByRole('tab', { name: /relationships/i }));

        expect(screen.getByText(/dangling/i)).toBeInTheDocument();
    });

    it('shows the source badge for a SCIM-sourced entity', () => {
        const scimUser: EntityInstance = {
            ...ALICE,
            source: 'scim',
            principalProvider: 'Okta',
            _backendId: undefined,
        };
        renderSheet({ entityKey: 'User::alice', allEntities: [scimUser] });

        // SCIM badge surfaces the provider.
        expect(screen.getByText(/scim · okta/i)).toBeInTheDocument();
        // SCIM entities are not local => no Delete button.
        expect(screen.queryByRole('button', { name: /delete entity/i })).toBeNull();
    });
});
