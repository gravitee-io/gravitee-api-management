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

import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen } from '@testing-library/react';

import { ColorField } from './ColorField';

beforeAll(() => {
    Object.defineProperty(window, 'matchMedia', {
        writable: true,
        value: jest.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: jest.fn(),
            removeListener: jest.fn(),
            addEventListener: jest.fn(),
            removeEventListener: jest.fn(),
            dispatchEvent: jest.fn(),
        })),
    });
    global.ResizeObserver = class ResizeObserver {
        observe() {}
        unobserve() {}
        disconnect() {}
    } as typeof ResizeObserver;
});

describe('ColorField', () => {
    it('labels the hex input separately from the color picker', () => {
        const onChange = jest.fn();
        renderWithGraphene(
            <>
                <label htmlFor="idp-color">Authentication button color</label>
                <ColorField id="idp-color" value="#112233" disabled={false} onChange={onChange} />
            </>,
        );
        const picker = screen.getByLabelText('Authentication button color');
        expect(picker.getAttribute('id')).toBe('idp-color');
        expect(picker.getAttribute('aria-label')).toBeNull();
        const hex = screen.getByLabelText('Authentication button color hex value');
        fireEvent.change(hex, { target: { value: '#abcdef' } });
        fireEvent.blur(hex);
        expect(onChange).toHaveBeenCalledWith('#abcdef');
    });

    it('reverts invalid hex on blur instead of posting it', () => {
        const onChange = jest.fn();
        renderWithGraphene(<ColorField id="idp-color" value="#112233" disabled={false} onChange={onChange} />);
        const hex = screen.getByLabelText('Authentication button color hex value');
        fireEvent.change(hex, { target: { value: 'red' } });
        fireEvent.blur(hex);
        expect(onChange).not.toHaveBeenCalled();
        expect(hex.getAttribute('value') ?? (hex as HTMLInputElement).value).toBe('#112233');
    });
});
