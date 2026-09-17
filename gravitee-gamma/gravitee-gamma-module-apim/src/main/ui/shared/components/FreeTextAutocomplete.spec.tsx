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

import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';

import { FreeTextAutocomplete } from './FreeTextAutocomplete';

const SUGGESTIONS = ['Accept', 'Accept-Charset', 'Content-Type'] as const;

function ControlledAutocomplete({
    initial = '',
    suggestions = SUGGESTIONS,
    showInvalid,
    isValid,
}: {
    initial?: string;
    suggestions?: readonly string[];
    showInvalid?: boolean;
    isValid?: (value: string) => boolean;
}) {
    const [value, setValue] = useState(initial);
    return (
        <FreeTextAutocomplete
            value={value}
            onChange={setValue}
            suggestions={suggestions.filter(s => s.toLowerCase().includes(value.trim().toLowerCase()) || value.trim() === '')}
            aria-label="Header name"
            isValid={isValid}
            invalidMessage="Header name must not contain spaces."
            showInvalid={showInvalid}
        />
    );
}

describe('FreeTextAutocomplete', () => {
    it('opens suggestions on focus and selects with a click', async () => {
        const user = userEvent.setup();
        render(<ControlledAutocomplete />);

        const input = screen.getByRole('combobox', { name: /header name/i });
        await user.click(input);

        const listbox = await screen.findByRole('listbox');
        expect(within(listbox).getByRole('option', { name: 'Accept' })).toBeInTheDocument();

        await user.click(within(listbox).getByRole('option', { name: 'Content-Type' }));
        expect(input).toHaveValue('Content-Type');
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('navigates with ArrowDown/ArrowUp and selects with Enter; aria-activedescendant tracks the option', async () => {
        const user = userEvent.setup();
        render(<ControlledAutocomplete />);

        const input = screen.getByRole('combobox', { name: /header name/i });
        await user.click(input);

        await user.keyboard('{ArrowDown}');
        const first = await screen.findByRole('option', { name: 'Accept' });
        expect(first).toHaveAttribute('aria-selected', 'true');
        expect(input).toHaveAttribute('aria-activedescendant', first.id);

        await user.keyboard('{ArrowDown}');
        const second = screen.getByRole('option', { name: 'Accept-Charset' });
        expect(second).toHaveAttribute('aria-selected', 'true');
        expect(first).toHaveAttribute('aria-selected', 'false');
        expect(input).toHaveAttribute('aria-activedescendant', second.id);

        await user.keyboard('{Enter}');
        expect(input).toHaveValue('Accept-Charset');
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('closes the listbox on Escape without changing the value', async () => {
        const user = userEvent.setup();
        render(<ControlledAutocomplete initial="Accept" />);

        const input = screen.getByRole('combobox', { name: /header name/i });
        await user.click(input);
        expect(await screen.findByRole('listbox')).toBeInTheDocument();

        await user.keyboard('{Escape}');
        expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
        expect(input).toHaveValue('Accept');
    });

    it('gates invalid messaging behind showInvalid', () => {
        const isValid = (v: string) => v.length === 0 || /^\S*$/.test(v);
        const onChange = jest.fn();

        const { rerender } = render(
            <FreeTextAutocomplete
                value="Bad Name"
                onChange={onChange}
                suggestions={[]}
                aria-label="Header name"
                isValid={isValid}
                invalidMessage="Header name must not contain spaces."
                showInvalid={false}
            />,
        );
        expect(screen.queryByText(/must not contain spaces/i)).not.toBeInTheDocument();
        expect(screen.getByRole('combobox')).not.toHaveAttribute('aria-invalid');

        rerender(
            <FreeTextAutocomplete
                value="Bad Name"
                onChange={onChange}
                suggestions={[]}
                aria-label="Header name"
                isValid={isValid}
                invalidMessage="Header name must not contain spaces."
                showInvalid
            />,
        );
        expect(screen.getByText(/must not contain spaces/i)).toBeInTheDocument();
        expect(screen.getByRole('combobox')).toHaveAttribute('aria-invalid', 'true');
    });

    it('prevents input blur when mousedown occurs on the listbox', async () => {
        const user = userEvent.setup();
        render(<ControlledAutocomplete />);

        const input = screen.getByRole('combobox', { name: /header name/i });
        await user.click(input);
        const listbox = await screen.findByRole('listbox');

        await user.pointer({ keys: '[MouseLeft>]', target: listbox });
        expect(screen.getByRole('listbox')).toBeInTheDocument();
        await user.pointer({ keys: '[/MouseLeft]', target: listbox });
    });
});
