import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PolicyBuilder } from '../PolicyBuilder';
import type { PolicyStatement } from '../statement-to-gapl';

describe('PolicyBuilder', () => {
    it('emits a new permit statement when "Add permit" is clicked', async () => {
        const onChange = vi.fn();

        render(
            <PolicyBuilder
                policyName="p1"
                target={null}
                statements={[]}
                principalOptions={[]}
                actionOptions={[]}
                resourceOptions={[]}
                resourceGroups={[]}
                onChange={onChange}
            />,
        );

        await userEvent.click(screen.getByRole('button', { name: /add permit statement/i }));

        expect(onChange).toHaveBeenCalledTimes(1);
        const next = onChange.mock.calls[0][0] as readonly PolicyStatement[];
        expect(next).toHaveLength(1);
        expect(next[0].effect).toBe('permit');
    });

    it('emits a new forbid statement when "Add forbid" is clicked', async () => {
        const onChange = vi.fn();

        render(
            <PolicyBuilder
                policyName="p2"
                target={null}
                statements={[]}
                principalOptions={[]}
                actionOptions={[]}
                resourceOptions={[]}
                resourceGroups={[]}
                onChange={onChange}
            />,
        );

        await userEvent.click(screen.getByRole('button', { name: /add forbid statement/i }));

        expect(onChange).toHaveBeenCalledTimes(1);
        const next = onChange.mock.calls[0][0] as readonly PolicyStatement[];
        expect(next).toHaveLength(1);
        expect(next[0].effect).toBe('forbid');
    });

    it('renders empty state when no statements', () => {
        render(
            <PolicyBuilder
                policyName="p3"
                target={null}
                statements={[]}
                principalOptions={[]}
                actionOptions={[]}
                resourceOptions={[]}
                resourceGroups={[]}
                onChange={vi.fn()}
            />,
        );

        expect(screen.getByText(/no statements yet/i)).toBeInTheDocument();
    });
});
