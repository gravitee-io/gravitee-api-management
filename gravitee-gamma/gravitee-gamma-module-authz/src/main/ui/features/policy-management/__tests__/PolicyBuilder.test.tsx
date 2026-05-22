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
