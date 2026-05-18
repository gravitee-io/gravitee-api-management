import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { EntityResponse } from '../../../../lib/api/authz-api.types';
import { EntityEditorForm } from '../EntityEditorForm';

function setup(overrides: Partial<Parameters<typeof EntityEditorForm>[0]> = {}) {
    const onCancel = vi.fn();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    const utils = render(<EntityEditorForm onCancel={onCancel} onSubmit={onSubmit} {...overrides} />);
    return { ...utils, onCancel, onSubmit };
}

const ALICE: EntityResponse = {
    id: 'be-alice',
    environmentId: 'env-1',
    uid: 'User::"alice"',
    attributes: { name: 'Alice', tenant: 'eu' },
    parents: ['Group::"devs"', 'Group::"admins"'],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-02T00:00:00Z',
};

describe('EntityEditorForm — render state', () => {
    it('renders blank state when no `initial` is provided', () => {
        setup();
        expect(screen.getByLabelText(/Entity UID/i)).toHaveValue('');
        // Empty attribute bag serialises as `{}`.
        expect(screen.getByLabelText(/Attributes/i)).toHaveValue('{}');
        expect(screen.getByLabelText(/Parents/i)).toHaveValue('');
        // Save button labelled for create flow and disabled when UID empty.
        const submitBtn = screen.getByRole('button', { name: /Create entity/i });
        expect(submitBtn).toBeDisabled();
    });

    it('prefills values when an `initial` entity is supplied', () => {
        setup({ initial: ALICE });
        expect(screen.getByLabelText(/Entity UID/i)).toHaveValue('User::"alice"');
        const attrs = screen.getByLabelText(/Attributes/i) as HTMLTextAreaElement;
        // The textarea should contain the parsed attributes serialised as
        // pretty-printed JSON; round-tripping confirms the shape.
        expect(JSON.parse(attrs.value)).toEqual({ name: 'Alice', tenant: 'eu' });
        expect(screen.getByLabelText(/Parents/i)).toHaveValue('Group::"devs"\nGroup::"admins"');
        expect(screen.getByRole('button', { name: /Update entity/i })).toBeEnabled();
    });
});

describe('EntityEditorForm — submit validation', () => {
    it('keeps the submit button disabled while UID is blank', async () => {
        const { onSubmit } = setup();
        const submitBtn = screen.getByRole('button', { name: /Create entity/i });
        expect(submitBtn).toBeDisabled();
        await userEvent.click(submitBtn);
        expect(onSubmit).not.toHaveBeenCalled();
    });

    it('submits with a parsed object when UID and JSON object are valid', async () => {
        const user = userEvent.setup();
        const { onSubmit } = setup();
        await user.type(screen.getByLabelText(/Entity UID/i), 'User::"bob"');
        // `fireEvent.change` avoids userEvent.type's special-character grammar
        // (`{`, `[`, `,`) which would otherwise need escaping in JSON inputs.
        fireEvent.change(screen.getByLabelText(/Attributes/i), { target: { value: '{"role":"admin"}' } });
        await user.click(screen.getByRole('button', { name: /Create entity/i }));
        expect(onSubmit).toHaveBeenCalledTimes(1);
        expect(onSubmit).toHaveBeenCalledWith({
            uid: 'User::"bob"',
            attributes: { role: 'admin' },
            parents: [],
        });
    });

    it('rejects a JSON string primitive ("hello") with a validation error and does not submit', async () => {
        const user = userEvent.setup();
        const { onSubmit } = setup();
        await user.type(screen.getByLabelText(/Entity UID/i), 'User::"bob"');
        fireEvent.change(screen.getByLabelText(/Attributes/i), { target: { value: '"hello"' } });
        await user.click(screen.getByRole('button', { name: /Create entity/i }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText(/Attributes must be a JSON object/i)).toBeInTheDocument();
    });

    it('rejects a JSON array ([1,2,3]) with a validation error and does not submit', async () => {
        const user = userEvent.setup();
        const { onSubmit } = setup();
        await user.type(screen.getByLabelText(/Entity UID/i), 'User::"bob"');
        fireEvent.change(screen.getByLabelText(/Attributes/i), { target: { value: '[1,2,3]' } });
        await user.click(screen.getByRole('button', { name: /Create entity/i }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText(/Attributes must be a JSON object/i)).toBeInTheDocument();
    });

    it('rejects malformed JSON with a parse error', async () => {
        const user = userEvent.setup();
        const { onSubmit } = setup();
        await user.type(screen.getByLabelText(/Entity UID/i), 'User::"bob"');
        fireEvent.change(screen.getByLabelText(/Attributes/i), { target: { value: '{not json' } });
        await user.click(screen.getByRole('button', { name: /Create entity/i }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText(/Attributes must be valid JSON/i)).toBeInTheDocument();
    });

    it('rejects a JSON `null` literal with a validation error', async () => {
        const user = userEvent.setup();
        const { onSubmit } = setup();
        await user.type(screen.getByLabelText(/Entity UID/i), 'User::"bob"');
        fireEvent.change(screen.getByLabelText(/Attributes/i), { target: { value: 'null' } });
        await user.click(screen.getByRole('button', { name: /Create entity/i }));
        expect(onSubmit).not.toHaveBeenCalled();
        expect(screen.getByText(/Attributes must be a JSON object/i)).toBeInTheDocument();
    });
});

describe('EntityEditorForm — cancel', () => {
    it('invokes onCancel when the Cancel button is clicked', async () => {
        const user = userEvent.setup();
        const { onCancel } = setup();
        await user.click(screen.getByRole('button', { name: /Cancel/i }));
        expect(onCancel).toHaveBeenCalledTimes(1);
    });
});
