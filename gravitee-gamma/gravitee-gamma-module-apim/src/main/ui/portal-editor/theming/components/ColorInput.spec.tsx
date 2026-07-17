/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { renderWithGraphene } from '@gravitee/graphene-core/testing';
import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ColorInput } from './ColorInput';

describe('ColorInput', () => {
    it('should call onChange with transparent when Transparent is clicked', async () => {
        const onChange = jest.fn();

        renderWithGraphene(<ColorInput value="#6366f1" onChange={onChange} label="Primary" />);

        await userEvent.click(screen.getByRole('button', { name: 'Set Primary to transparent' }));

        expect(onChange).toHaveBeenCalledWith('transparent');
    });

    it('should mark Transparent button as pressed when value is transparent', () => {
        renderWithGraphene(<ColorInput value="transparent" onChange={jest.fn()} label="Primary" />);

        expect(screen.getByRole('button', { name: 'Set Primary to transparent' })).toHaveAttribute('aria-pressed', 'true');
    });

    it('should not mark Transparent button as pressed for opaque colors', () => {
        renderWithGraphene(<ColorInput value="#6366f1" onChange={jest.fn()} label="Primary" />);

        expect(screen.getByRole('button', { name: 'Set Primary to transparent' })).toHaveAttribute('aria-pressed', 'false');
    });

    it('should replace transparent with picked color from native input', () => {
        const onChange = jest.fn();

        renderWithGraphene(<ColorInput value="transparent" onChange={onChange} label="Primary" />);

        const nativeInput = document.querySelector('input[type="color"]') as HTMLInputElement;
        fireEvent.change(nativeInput, { target: { value: '#ff0000' } });

        expect(onChange).toHaveBeenCalledWith('#ff0000');
    });

    it('should keep native color input value as valid hex when current value is transparent', () => {
        renderWithGraphene(<ColorInput value="transparent" onChange={jest.fn()} />);

        const nativeInput = document.querySelector('input[type="color"]') as HTMLInputElement;

        expect(nativeInput.value).toBe('#000000');
    });

    it('should keep native color input value in sync for opaque hex colors', () => {
        renderWithGraphene(<ColorInput value="#6366f1" onChange={jest.fn()} />);

        const nativeInput = document.querySelector('input[type="color"]') as HTMLInputElement;

        expect(nativeInput.value).toBe('#6366f1');
    });
});
