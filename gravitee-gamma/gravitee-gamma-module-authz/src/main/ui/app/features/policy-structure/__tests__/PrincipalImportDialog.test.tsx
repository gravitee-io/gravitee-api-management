import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PrincipalImportDialog } from '../PrincipalImportDialog';

function makeProps(overrides: Partial<Parameters<typeof PrincipalImportDialog>[0]> = {}): Parameters<typeof PrincipalImportDialog>[0] {
    return {
        open: true,
        onOpenChange: vi.fn(),
        create: vi.fn().mockResolvedValue({}),
        onImported: vi.fn(),
        ...overrides,
    };
}

/**
 * Set the value of a textarea so React's synthetic event system picks it up.
 * Calling .value = ... on the DOM node and dispatching a plain Event works for
 * vanilla HTML, but React monkey-patches the underlying setter, so we have to
 * call the native setter manually before firing the input event.
 */
function setTextareaValue(el: HTMLTextAreaElement, value: string) {
    const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value')?.set;
    nativeSetter?.call(el, value);
    fireEvent.input(el);
}

describe('PrincipalImportDialog', () => {
    it('parses a valid principal array and shows the preview row', async () => {
        render(<PrincipalImportDialog {...makeProps()} />);

        const textarea = screen.getByPlaceholderText(/"uid"/) as HTMLTextAreaElement;
        setTextareaValue(textarea, JSON.stringify([{ uid: { type: 'User', id: 'alice' }, attrs: { name: 'Alice' }, parents: [] }]));

        await userEvent.click(screen.getByRole('button', { name: /validate json/i }));

        await waitFor(() => expect(screen.getByText(/1 valid/)).toBeInTheDocument());
        expect(screen.getByText('alice')).toBeInTheDocument();
    });

    it('shows a parse error when the JSON is malformed', async () => {
        render(<PrincipalImportDialog {...makeProps()} />);

        const textarea = screen.getByPlaceholderText(/"uid"/) as HTMLTextAreaElement;
        setTextareaValue(textarea, '{ not valid json');

        await userEvent.click(screen.getByRole('button', { name: /validate json/i }));

        await waitFor(() => expect(screen.getByText(/parse error/i)).toBeInTheDocument());
    });

    it('calls create per principal when Import is clicked', async () => {
        const create = vi.fn().mockResolvedValue({});
        const onImported = vi.fn();
        render(<PrincipalImportDialog {...makeProps({ create, onImported })} />);

        const textarea = screen.getByPlaceholderText(/"uid"/) as HTMLTextAreaElement;
        setTextareaValue(
            textarea,
            JSON.stringify([
                { uid: { type: 'User', id: 'alice' }, attrs: { name: 'Alice' }, parents: [] },
                { uid: { type: 'User', id: 'bob' }, attrs: { name: 'Bob' }, parents: [] },
            ]),
        );

        await userEvent.click(screen.getByRole('button', { name: /validate json/i }));
        await waitFor(() => expect(screen.getByText(/2 valid/)).toBeInTheDocument());

        // The import button's label includes a count + the "principals" word —
        // match by text fragment to avoid coupling to exact pluralization.
        const importBtn = screen.getAllByRole('button').find(b => /^import\b.+principals?/i.test(b.textContent ?? ''));
        expect(importBtn).toBeDefined();
        await userEvent.click(importBtn!);

        await waitFor(() => expect(create).toHaveBeenCalledTimes(2));
        await waitFor(() => expect(onImported).toHaveBeenCalledWith({ added: 2, skipped: 0, failed: 0 }));
    });

    // ----------------------------------------------------------------
    // Wave 1B: extended coverage.
    // ----------------------------------------------------------------

    it('reports a mixed result when some create() calls reject', async () => {
        // Three principals — second one fails to import.
        const create = vi
            .fn()
            .mockResolvedValueOnce({ id: 'be-1' })
            .mockRejectedValueOnce(new Error('boom: backend conflict'))
            .mockResolvedValueOnce({ id: 'be-3' });
        const onImported = vi.fn();
        render(<PrincipalImportDialog {...makeProps({ create, onImported })} />);

        const textarea = screen.getByPlaceholderText(/"uid"/) as HTMLTextAreaElement;
        setTextareaValue(
            textarea,
            JSON.stringify([
                { uid: { type: 'User', id: 'alice' }, attrs: { name: 'Alice' }, parents: [] },
                { uid: { type: 'User', id: 'bob' }, attrs: { name: 'Bob' }, parents: [] },
                { uid: { type: 'User', id: 'carol' }, attrs: { name: 'Carol' }, parents: [] },
            ]),
        );

        await userEvent.click(screen.getByRole('button', { name: /validate json/i }));
        await waitFor(() => expect(screen.getByText(/3 valid/)).toBeInTheDocument());

        const importBtn = screen.getAllByRole('button').find(b => /^import\b.+principals?/i.test(b.textContent ?? ''));
        await userEvent.click(importBtn!);

        // Summary screen shows the counts.
        await waitFor(() => expect(screen.getByText(/import complete/i)).toBeInTheDocument());
        expect(screen.getByText(/2 imported, 0 skipped/i)).toBeInTheDocument();
        // The failed-items panel surfaces the rejected entity + reason.
        expect(screen.getByText(/User::bob: boom: backend conflict/)).toBeInTheDocument();
        // Download button for failure list is offered.
        expect(screen.getByRole('button', { name: /download failure list/i })).toBeInTheDocument();
        // The summary callback gets the full breakdown.
        expect(onImported).toHaveBeenCalledWith({ added: 2, skipped: 0, failed: 1 });
    });

    it('Cancel closes the dialog and never calls create()', async () => {
        const create = vi.fn();
        const onOpenChange = vi.fn();
        render(<PrincipalImportDialog {...makeProps({ create, onOpenChange })} />);

        // Type something so we can prove Cancel is the path that closes.
        const textarea = screen.getByPlaceholderText(/"uid"/) as HTMLTextAreaElement;
        setTextareaValue(textarea, '[]');

        await userEvent.click(screen.getByRole('button', { name: /^cancel$/i }));

        expect(onOpenChange).toHaveBeenCalledWith(false);
        expect(create).not.toHaveBeenCalled();
    });

    it('shows the empty-array notice when the JSON parses to []', async () => {
        render(<PrincipalImportDialog {...makeProps()} />);

        const textarea = screen.getByPlaceholderText(/"uid"/) as HTMLTextAreaElement;
        setTextareaValue(textarea, '[]');

        await userEvent.click(screen.getByRole('button', { name: /validate json/i }));

        await waitFor(() => expect(screen.getByText(/the array is empty/i)).toBeInTheDocument());
        // Import button stays disabled since validItems is 0.
        const importBtn = screen.getAllByRole('button').find(b => b.textContent?.trim().toLowerCase().startsWith('import'));
        expect(importBtn).toBeDefined();
        expect(importBtn).toBeDisabled();
    });

    it('disables the Import button before any JSON is validated', () => {
        render(<PrincipalImportDialog {...makeProps()} />);

        // The footer Import button is present but disabled when there's nothing to import.
        const importBtn = screen.getAllByRole('button').find(b => b.textContent?.trim().toLowerCase().startsWith('import'));
        expect(importBtn).toBeDefined();
        expect(importBtn).toBeDisabled();
    });
});
