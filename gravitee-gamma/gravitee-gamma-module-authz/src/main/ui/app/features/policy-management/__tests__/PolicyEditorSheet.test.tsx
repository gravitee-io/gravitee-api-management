import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { PolicyResponse } from '../../../../lib/api/authz-api.types';
import type { PolicyEditorSheetProps } from '../PolicyEditorSheet';
import { PolicyEditorSheet } from '../PolicyEditorSheet';
import type { ServicePageConfig } from '../ServicePolicyPage';

function makePolicy(overrides: Partial<PolicyResponse> = {}): PolicyResponse {
    return {
        id: 'p1',
        environmentId: 'DEFAULT',
        name: 'Existing Policy',
        description: null,
        policyText: 'permit (principal == user::"alice", action == action::"read", resource == tool::"t");',
        type: 'CUSTOM',
        target: null,
        status: 'DRAFT',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        ...overrides,
    };
}

const config: ServicePageConfig = {
    type: 'CUSTOM',
    title: 'Custom Policies',
    description: 'Test',
    createButtonLabel: 'Create',
    searchPlaceholder: 'Search…',
    hasTarget: false,
    serviceLabel: 'Custom',
};

function makeProps(overrides: Partial<PolicyEditorSheetProps> = {}): PolicyEditorSheetProps {
    return {
        config,
        open: true,
        policy: null,
        initialTarget: null,
        submitError: null,
        principalOptions: [],
        actionOptions: [],
        onOpenChange: vi.fn(),
        onSubmit: vi.fn().mockResolvedValue(undefined),
        ...overrides,
    };
}

describe('PolicyEditorSheet', () => {
    it('renders when open', () => {
        render(<PolicyEditorSheet {...makeProps()} />);
        expect(screen.getByRole('textbox', { name: /policy name/i })).toBeInTheDocument();
    });

    it('does not render editor content when closed', () => {
        render(<PolicyEditorSheet {...makeProps({ open: false })} />);
        expect(screen.queryByRole('textbox', { name: /policy name/i })).not.toBeInTheDocument();
    });

    it('calls onSubmit with name and type on save', async () => {
        const onSubmit = vi.fn().mockResolvedValue(undefined);

        render(<PolicyEditorSheet {...makeProps({ onSubmit })} />);

        const nameInput = screen.getByRole('textbox', { name: /policy name/i });
        await userEvent.clear(nameInput);
        await userEvent.type(nameInput, 'My Test Policy');

        await userEvent.click(screen.getByRole('button', { name: /create policy/i }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'My Test Policy',
                    type: 'CUSTOM',
                }),
            );
        });
    });

    it('save button is disabled when name is empty', async () => {
        render(<PolicyEditorSheet {...makeProps()} />);

        const saveBtn = screen.getByRole('button', { name: /create policy/i });
        // Name starts empty so button should be disabled
        expect(saveBtn).toBeDisabled();
    });

    it('calls onOpenChange(false) when cancel clicked', async () => {
        const onOpenChange = vi.fn();

        render(<PolicyEditorSheet {...makeProps({ onOpenChange })} />);

        await userEvent.click(screen.getByRole('button', { name: /cancel/i }));

        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('Deploy button is disabled when policy is null (new draft not saved yet)', () => {
        render(<PolicyEditorSheet {...makeProps()} />);

        const deployBtn = screen.getByRole('button', { name: /deploy to pdp/i });
        expect(deployBtn).toBeDisabled();
    });

    it('renders Deploy button enabled when policy is saved (status=DRAFT)', () => {
        const policy = makePolicy({ status: 'DRAFT' });
        render(<PolicyEditorSheet {...makeProps({ policy })} />);

        const deployBtn = screen.getByRole('button', { name: /deploy to pdp/i });
        expect(deployBtn).not.toBeDisabled();
    });

    it('Deploy click calls onSubmit with status DEPLOYED', async () => {
        const policy = makePolicy({ status: 'DRAFT' });
        const onSubmit = vi.fn().mockResolvedValue(undefined);

        render(<PolicyEditorSheet {...makeProps({ policy, onSubmit })} />);

        await userEvent.click(screen.getByRole('button', { name: /deploy to pdp/i }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    status: 'DEPLOYED',
                }),
            );
        });
    });

    it("Deploy button shows 'Deployed (N min ago)' when status is DEPLOYED", () => {
        const policy = makePolicy({
            status: 'DEPLOYED',
            // 5 minutes ago — relativeTime() returns "5 min ago".
            updatedAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
        });

        render(<PolicyEditorSheet {...makeProps({ policy })} />);

        const deployBtn = screen.getByRole('button', { name: /deployed \(.+ min ago\)/i });
        expect(deployBtn).toBeInTheDocument();
        expect(deployBtn).toBeDisabled();
    });

    it('toggles to code pane when Code button clicked', async () => {
        render(<PolicyEditorSheet {...makeProps()} />);

        await userEvent.click(screen.getByRole('button', { name: /code/i }));

        // Code pane shows a textarea for GAPL
        expect(screen.getByRole('textbox', { name: /gapl policy text/i })).toBeInTheDocument();
    });

    it('enables the Visual toggle and seeds visual statements when editing a parseable policy', async () => {
        const policy = makePolicy();
        render(<PolicyEditorSheet {...makeProps({ policy })} />);

        // The Visual button should not be disabled.
        const visualBtn = screen.getByRole('button', { name: /^visual$/i });
        expect(visualBtn).not.toBeDisabled();

        // Toggling to Code should preview the visual-derived GAPL and include
        // the parsed UID — proving the parser actually seeded statements.
        await userEvent.click(screen.getByRole('button', { name: /^code$/i }));
        const code = screen.getByRole('textbox', { name: /gapl policy text/i }) as HTMLTextAreaElement;
        await waitFor(() => expect(code.value).toContain('user::"alice"'));
    });

    it('disables the Visual toggle for unparseable GAPL and keeps original policyText', async () => {
        const exotic = 'permit (principal == user::"a", action == action::"r", resource == tool::"t") unless { false };';
        const policy = makePolicy({ policyText: exotic });
        render(<PolicyEditorSheet {...makeProps({ policy })} />);

        const visualBtn = screen.getByRole('button', { name: /^visual$/i });
        expect(visualBtn).toBeDisabled();
        expect(visualBtn).toHaveAttribute('title', expect.stringContaining('visual editor cannot represent'));

        // The code textarea should hold the original GAPL untouched.
        const code = screen.getByRole('textbox', { name: /gapl policy text/i }) as HTMLTextAreaElement;
        expect(code.value).toBe(exotic);
    });

    it('does not auto-overwrite policyText from visual when editing in code view', async () => {
        // Hand-crafted GAPL with deliberate formatting differences from
        // statementToGapl's output. After the Bug D fix the editor must
        // preserve this verbatim until the user actually edits the visual
        // statements — switching tabs alone never rewrites the buffer.
        const handcrafted = 'permit (principal == user::"alice", action == action::"read", resource == tool::"t") ;';
        const policy = makePolicy({ policyText: handcrafted });
        render(<PolicyEditorSheet {...makeProps({ policy })} />);

        await userEvent.click(screen.getByRole('button', { name: /^code$/i }));
        const code = screen.getByRole('textbox', { name: /gapl policy text/i }) as HTMLTextAreaElement;
        // Bug D — byte-for-byte preservation: switching tabs without
        // touching the visual side must NOT regenerate the stored GAPL.
        expect(code.value).toBe(handcrafted);
    });

    // Bug D — full edit-flow regression: open with valid GAPL, ensure the
    // visual side is seeded, toggle to Code, then back to Visual, and
    // confirm the original policyText survives untouched until the user
    // edits something on the visual side.
    it('round-trips the edit flow without rewriting the stored GAPL (Bug D)', async () => {
        const original = 'permit (principal == User::"alice", action == Action::"read", resource == Resource::"r1");';
        const policy = makePolicy({ policyText: original });
        render(<PolicyEditorSheet {...makeProps({ policy })} />);

        // Visual toggle is enabled, statements seeded — proves the
        // GAPL→visual parser ran on open.
        const visualBtn = screen.getByRole('button', { name: /^visual$/i });
        expect(visualBtn).not.toBeDisabled();

        // Toggle to Code.
        await userEvent.click(screen.getByRole('button', { name: /^code$/i }));
        let code = screen.getByRole('textbox', { name: /gapl policy text/i }) as HTMLTextAreaElement;
        expect(code.value).toBe(original);

        // Toggle back to Visual.
        await userEvent.click(screen.getByRole('button', { name: /^visual$/i }));
        // Toggle to Code again — must STILL be the original, regardless of
        // how many times the user flips between tabs without editing.
        await userEvent.click(screen.getByRole('button', { name: /^code$/i }));
        code = screen.getByRole('textbox', { name: /gapl policy text/i }) as HTMLTextAreaElement;
        expect(code.value).toBe(original);
        // No 'Modified in code' badge either — buffer is in sync with what
        // the server sent.
        expect(screen.queryByText(/modified in code/i)).not.toBeInTheDocument();
    });

    it('does not lowercase the action namespace on save when only metadata changed (Bug D)', async () => {
        // Stored policy uses uppercase 'Action::' (canonical schema casing).
        // Saving without touching the GAPL must echo the original text back
        // unchanged — otherwise the visual roundtrip silently lowercases.
        const original = 'permit (principal == User::"alice", action == Action::"read", resource == Resource::"r1");';
        const policy = makePolicy({ policyText: original });
        const onSubmit = vi.fn().mockResolvedValue(undefined);

        render(<PolicyEditorSheet {...makeProps({ policy, onSubmit })} />);
        await userEvent.click(screen.getByRole('button', { name: /update policy/i }));

        await waitFor(() => {
            expect(onSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    policyText: original,
                }),
            );
        });
    });
});
