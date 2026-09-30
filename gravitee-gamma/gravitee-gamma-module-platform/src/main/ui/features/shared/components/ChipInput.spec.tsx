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
import { CORS_DEFAULT_HTTP_HEADERS } from '../../../shared/cors';

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
        expect(input.getAttribute('aria-activedescendant')).toBeTruthy();
        expect(screen.getByRole('option', { name: 'Content-Type' })).toHaveAttribute('data-highlighted');

        fireEvent.keyDown(input, { key: 'ArrowDown' });
        expect(screen.getByRole('option', { name: 'Authorization' })).toHaveAttribute('data-highlighted');

        fireEvent.keyDown(input, { key: 'Enter' });
        expect(screen.getByText('Authorization')).not.toBeNull();
        expect(screen.getByRole('listbox')).not.toBeNull();
    });

    it('commits the typed draft on Enter when no suggestion is highlighted', () => {
        render(<Harness suggestions={SUGGESTIONS} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.change(input, { target: { value: 'X-Custom' } });
        fireEvent.keyDown(input, { key: 'Enter' });
        expect(screen.getByRole('button', { name: 'Remove X-Custom' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Remove Content-Type' })).toBeNull();
    });

    it('clears the highlight after typing so Enter still adds a custom value', () => {
        render(<Harness suggestions={SUGGESTIONS} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.keyDown(input, { key: 'ArrowDown' });
        fireEvent.change(input, { target: { value: 'X-Custom' } });
        fireEvent.keyDown(input, { key: 'Enter' });
        expect(screen.getByRole('button', { name: 'Remove X-Custom' })).not.toBeNull();
        expect(screen.queryByRole('button', { name: 'Remove Content-Type' })).toBeNull();
    });

    it('does not reopen the suggestion list when removing a chip', () => {
        render(<Harness suggestions={SUGGESTIONS} initial={['Authorization']} />);
        const input = screen.getByRole('combobox');
        expect(input.getAttribute('aria-expanded')).toBe('false');
        fireEvent.click(screen.getByRole('button', { name: 'Remove Authorization' }));
        expect(screen.queryByRole('button', { name: 'Remove Authorization' })).toBeNull();
        expect(input.getAttribute('aria-expanded')).toBe('false');
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

    it('reopens the suggestion list after selecting a value and clicking the input again', () => {
        render(<Harness suggestions={SUGGESTIONS} addOnBlur={false} />);
        const input = screen.getByRole('combobox');
        fireEvent.focus(input);
        fireEvent.click(screen.getByRole('option', { name: 'Authorization' }));
        expect(screen.getByText('Authorization')).not.toBeNull();
        expect(screen.getByRole('listbox')).not.toBeNull();

        fireEvent.blur(input);
        expect(screen.queryByRole('listbox')).toBeNull();

        fireEvent.click(input);
        expect(screen.getByRole('listbox')).not.toBeNull();
        expect(screen.getByRole('option', { name: 'Content-Type' })).not.toBeNull();
    });

    it('renders the suggestion list in a portal so it can scroll outside clipped containers', () => {
        const { container } = render(
            <div className="h-24 overflow-hidden">
                <Harness suggestions={CORS_DEFAULT_HTTP_HEADERS} />
            </div>,
        );
        fireEvent.focus(screen.getByRole('combobox'));
        const listbox = screen.getByRole('listbox');
        const clippedContainer = container.querySelector('.overflow-hidden');
        expect(clippedContainer).not.toBeNull();
        expect(clippedContainer?.contains(listbox)).toBe(false);
        expect(document.body.contains(listbox)).toBe(true);
        expect(listbox.style.maxHeight).toBe('14rem');
        expect(listbox.style.overflowY).toBe('auto');
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
