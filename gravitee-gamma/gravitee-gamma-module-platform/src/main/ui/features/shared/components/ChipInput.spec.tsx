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

import { fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';

import { ChipInput, type ChipInputProps } from './ChipInput';

const SUGGESTIONS = ['Content-Type', 'Authorization', 'X-Requested-With'] as const;

function Harness({
    initial = [],
    ...props
}: Partial<ChipInputProps> & {
    initial?: string[];
}) {
    const [values, setValues] = useState(initial);
    return <ChipInput id="headers" values={values} onChange={setValues} placeholder="Add header" {...props} />;
}

describe('ChipInput', () => {
    it('commits the draft on Enter when there are no suggestions (OAuth redirect URIs)', () => {
        const onChange = jest.fn();
        render(<ChipInput values={[]} onChange={onChange} placeholder="Enter a redirect URI" />);
        const input = screen.getByPlaceholderText('Enter a redirect URI');
        expect(input.getAttribute('role')).toBeNull();
        fireEvent.change(input, { target: { value: 'https://app.example.com/callback' } });
        fireEvent.keyDown(input, { key: 'Enter' });
        expect(onChange).toHaveBeenCalledWith(['https://app.example.com/callback']);
    });

    it('exposes a combobox and lets Arrow keys plus Enter select a suggestion', () => {
        render(<Harness suggestions={SUGGESTIONS} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        expect(screen.getByRole('listbox')).not.toBeNull();

        fireEvent.keyDown(input, { key: 'ArrowDown' });
        expect(input.getAttribute('aria-activedescendant')).toBe('headers-option-0');
        expect(screen.getByRole('option', { name: 'Content-Type' }).getAttribute('aria-selected')).toBe('true');

        fireEvent.keyDown(input, { key: 'ArrowDown' });
        expect(input.getAttribute('aria-activedescendant')).toBe('headers-option-1');
        expect(screen.getByRole('option', { name: 'Authorization' }).getAttribute('aria-selected')).toBe('true');

        fireEvent.keyDown(input, { key: 'Enter' });
        expect(screen.getByText('Authorization')).not.toBeNull();
        expect(screen.queryByRole('listbox')).toBeNull();
    });

    it('commits the typed draft on Enter when no suggestion is highlighted', () => {
        render(<Harness suggestions={SUGGESTIONS} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.change(input, { target: { value: 'X-Custom' } });
        fireEvent.keyDown(input, { key: 'Enter' });
        expect(screen.getByText('X-Custom')).not.toBeNull();
        expect(screen.queryByText('Content-Type')).toBeNull();
    });

    it('clears the highlight after typing so Enter still adds a custom value', () => {
        render(<Harness suggestions={SUGGESTIONS} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.keyDown(input, { key: 'ArrowDown' });
        fireEvent.change(input, { target: { value: 'X-Custom' } });
        fireEvent.keyDown(input, { key: 'Enter' });
        expect(screen.getByText('X-Custom')).not.toBeNull();
        expect(screen.queryByText('Content-Type')).toBeNull();
    });

    it('closes the list on Escape', () => {
        render(<Harness suggestions={SUGGESTIONS} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        expect(screen.getByRole('listbox')).not.toBeNull();
        fireEvent.keyDown(input, { key: 'Escape' });
        expect(screen.queryByRole('listbox')).toBeNull();
        expect(input.getAttribute('aria-expanded')).toBe('false');
    });

    it('commits the draft on blur by default', () => {
        const onChange = jest.fn();
        render(<ChipInput values={[]} onChange={onChange} placeholder="Add origin" />);
        const input = screen.getByPlaceholderText('Add origin');
        fireEvent.change(input, { target: { value: 'https://app.example.com' } });
        fireEvent.blur(input);
        expect(onChange).toHaveBeenCalledWith(['https://app.example.com']);
    });

    it('discards the draft on blur when addOnBlur is false', () => {
        const onChange = jest.fn();
        render(<ChipInput values={[]} onChange={onChange} placeholder="Add header" addOnBlur={false} />);
        const input = screen.getByPlaceholderText('Add header');
        fireEvent.change(input, { target: { value: 'Cont' } });
        fireEvent.blur(input);
        expect(onChange).not.toHaveBeenCalled();
        expect((input as HTMLInputElement).value).toBe('');
    });

    it('marks the input invalid and points aria-describedby at the error', () => {
        render(
            <ChipInput
                id="branded-domains-0"
                values={['localhost']}
                onChange={jest.fn()}
                placeholder="partners.example.com"
                invalid
                describedBy="branded-domains-0-error"
            />,
        );
        const input = screen.getByPlaceholderText('partners.example.com');
        expect(input.getAttribute('aria-invalid')).toBe('true');
        expect(input.getAttribute('aria-describedby')).toBe('branded-domains-0-error');
    });
});
