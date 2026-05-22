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
import { GaplPreview } from '../GaplPreview';
import type { PolicyStatement } from '../statement-to-gapl';

const STATEMENTS: readonly PolicyStatement[] = [
    {
        id: 'stmt-1',
        effect: 'permit',
        principals: [{ id: 'p1', kind: 'user', label: 'alice' }],
        actions: [{ id: 'a1', label: 'can_read' }],
        resources: [{ id: 'r1', kind: 'mcp_server', label: 'flight-mcp' }],
        condition: '',
    },
];

describe('GaplPreview', () => {
    it('renders policy header and the GAPL code by default', () => {
        render(<GaplPreview policyName="my-policy" target={null} statements={STATEMENTS} />);
        expect(screen.getByText('Preview')).toBeInTheDocument();
        // Default view is "code" — look for an artefact from the generated GAPL.
        expect(screen.getByText(/permit/i)).toBeInTheDocument();
    });

    it('toggles to the form view when Form button is clicked', async () => {
        render(<GaplPreview policyName="my-policy" target={null} statements={STATEMENTS} />);

        await userEvent.click(screen.getByRole('button', { name: /form/i }));

        // Form view shows labelled rows like "Principals" (header dt + footer
        // stat) and the principal label "alice".
        expect(screen.getAllByText('Principals').length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('alice')).toBeInTheDocument();
        expect(screen.getByText('Statement 1')).toBeInTheDocument();
    });

    it('copies generated GAPL to clipboard and flips to "Copied" feedback', async () => {
        const writeText = vi.fn().mockResolvedValue(undefined);
        Object.assign(navigator, { clipboard: { writeText } });

        render(<GaplPreview policyName="my-policy" target={null} statements={STATEMENTS} />);

        await userEvent.click(screen.getByRole('button', { name: /copy/i }));

        expect(writeText).toHaveBeenCalledTimes(1);
        // The button label should switch to "Copied" while the timeout is pending.
        expect(await screen.findByRole('button', { name: /copied/i })).toBeInTheDocument();
    });

    it('shows an empty-state message in form view when no statements are provided', async () => {
        render(<GaplPreview policyName="empty" target={null} statements={[]} />);

        await userEvent.click(screen.getByRole('button', { name: /form/i }));

        expect(screen.getByText(/no statements yet/i)).toBeInTheDocument();
    });
});
