import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AddLocalPrincipalDialog } from '../AddLocalPrincipalDialog';
import type { EntityInstance } from '../entity-types';

function makeProps(overrides: Partial<Parameters<typeof AddLocalPrincipalDialog>[0]> = {}): Parameters<typeof AddLocalPrincipalDialog>[0] {
    return {
        open: true,
        onOpenChange: vi.fn(),
        create: vi.fn().mockResolvedValue({}),
        allEntities: [],
        onAdded: vi.fn(),
        ...overrides,
    };
}

const GROUP_DEVS: EntityInstance = {
    uid: { type: 'Group', id: 'group.devs' },
    displayName: 'Developers',
    attrs: { name: 'Developers' },
    parents: [],
    source: 'local',
};

describe('AddLocalPrincipalDialog', () => {
    it('opens with the User kind selected and shows User-specific fields', () => {
        render(<AddLocalPrincipalDialog {...makeProps()} />);

        expect(screen.getByRole('heading', { name: /add local principal/i })).toBeInTheDocument();
        // Display name input is always present.
        expect(screen.getByLabelText(/display name/i)).toBeInTheDocument();
        // Email is User-specific and should be visible by default.
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
        // Placeholder hint confirms User-mode.
        const nameInput = screen.getByLabelText(/display name/i) as HTMLInputElement;
        expect(nameInput.placeholder).toMatch(/Alice/i);
    });

    it('keeps the Create button disabled when name + email are empty (User kind)', () => {
        render(<AddLocalPrincipalDialog {...makeProps()} />);

        const createBtn = screen.getByRole('button', { name: /create principal/i });
        expect(createBtn).toBeDisabled();
    });

    it('calls create() with name + email attrs when submitting a valid User', async () => {
        const create = vi.fn().mockResolvedValue({});
        const onAdded = vi.fn();
        const onOpenChange = vi.fn();

        render(<AddLocalPrincipalDialog {...makeProps({ create, onAdded, onOpenChange })} />);

        await userEvent.type(screen.getByLabelText(/display name/i), 'Alice Nguyen');
        await userEvent.type(screen.getByLabelText(/email/i), 'alice@gravitee.io');

        const createBtn = screen.getByRole('button', { name: /create principal/i });
        expect(createBtn).not.toBeDisabled();

        await userEvent.click(createBtn);

        expect(create).toHaveBeenCalledTimes(1);
        const payload = create.mock.calls[0][0];
        expect(payload.uid).toBe('User::"user.alice"');
        expect(payload.attributes).toMatchObject({
            name: 'Alice Nguyen',
            email: 'alice@gravitee.io',
            tenant: 'gravitee',
            _displayName: 'Alice Nguyen',
        });
        expect(payload.parents).toEqual([]);

        expect(onAdded).toHaveBeenCalledWith({ type: 'User', id: 'user.alice' });
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('switches to Group kind: hides Email field and submits with description', async () => {
        const create = vi.fn().mockResolvedValue({});
        render(<AddLocalPrincipalDialog {...makeProps({ create })} />);

        // Click the Group kind tile.
        await userEvent.click(screen.getByRole('button', { name: /^group/i }));

        // Email should no longer be rendered.
        expect(screen.queryByLabelText(/email/i)).not.toBeInTheDocument();
        // Description field appears for Group.
        expect(screen.getByLabelText(/description/i)).toBeInTheDocument();

        await userEvent.type(screen.getByLabelText(/display name/i), 'Platform Engineering');
        await userEvent.type(screen.getByLabelText(/description/i), 'Owns the platform.');

        const createBtn = screen.getByRole('button', { name: /create principal/i });
        expect(createBtn).not.toBeDisabled();
        await userEvent.click(createBtn);

        expect(create).toHaveBeenCalledTimes(1);
        const payload = create.mock.calls[0][0];
        expect(payload.uid).toBe('Group::"group.platform-engineering"');
        expect(payload.attributes).toMatchObject({
            name: 'Platform Engineering',
            description: 'Owns the platform.',
        });
        // No email/tenant for Group.
        expect(payload.attributes).not.toHaveProperty('email');
    });

    it('Cancel closes the dialog without calling create()', async () => {
        const create = vi.fn();
        const onOpenChange = vi.fn();
        render(<AddLocalPrincipalDialog {...makeProps({ create, onOpenChange })} />);

        // Type something so we know Cancel is the only path that closed it.
        await userEvent.type(screen.getByLabelText(/display name/i), 'Will Cancel');

        // The footer Cancel sits next to "Create principal"; pick it specifically.
        const cancelBtn = screen.getByRole('button', { name: /^cancel$/i });
        await userEvent.click(cancelBtn);

        expect(create).not.toHaveBeenCalled();
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('uses the Override slug verbatim, without the auto kind prefix (bug F)', async () => {
        // Bug F: typing "alice" into Override slug produced User::"user.alice"
        // because the prefix was always prepended. After the fix, Override
        // is taken as-is and the submitted UID must be User::"alice".
        const create = vi.fn().mockResolvedValue({});
        const onAdded = vi.fn();
        render(<AddLocalPrincipalDialog {...makeProps({ create, onAdded })} />);

        await userEvent.type(screen.getByLabelText(/display name/i), 'Alice Nguyen');
        await userEvent.type(screen.getByLabelText(/email/i), 'alice@gravitee.io');
        // The Override input has no <label>; it's identified by its placeholder.
        const overrideInput = screen.getByPlaceholderText(/override slug/i);
        await userEvent.clear(overrideInput);
        await userEvent.type(overrideInput, 'alice');

        await userEvent.click(screen.getByRole('button', { name: /create principal/i }));

        expect(create).toHaveBeenCalledTimes(1);
        const payload = create.mock.calls[0][0];
        expect(payload.uid).toBe('User::"alice"');
        expect(onAdded).toHaveBeenCalledWith({ type: 'User', id: 'alice' });
    });

    it('still prepends the auto prefix when Override is left empty', async () => {
        // Sanity: the auto-prefix path is unchanged.
        const create = vi.fn().mockResolvedValue({});
        render(<AddLocalPrincipalDialog {...makeProps({ create })} />);

        await userEvent.type(screen.getByLabelText(/display name/i), 'Bob Smith');
        await userEvent.type(screen.getByLabelText(/email/i), 'bob@gravitee.io');

        await userEvent.click(screen.getByRole('button', { name: /create principal/i }));

        expect(create).toHaveBeenCalledTimes(1);
        expect(create.mock.calls[0][0].uid).toBe('User::"user.bob"');
    });

    it('renders existing groups as parent-group chips for User kind', () => {
        render(<AddLocalPrincipalDialog {...makeProps({ allEntities: [GROUP_DEVS] })} />);

        // The "Parent groups (optional)" label is shown for User kind, with the Group as a chip.
        expect(screen.getByText(/parent groups/i)).toBeInTheDocument();
        // The chip is a button labelled with the group display name.
        const chip = screen.getByRole('button', { name: /developers/i });
        expect(chip).toBeInTheDocument();
        // Sanity check: the dialog itself contains the chip.
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByRole('button', { name: /developers/i })).toBe(chip);
    });
});
