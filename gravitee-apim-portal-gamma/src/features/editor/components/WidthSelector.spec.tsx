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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { WidthSelector } from './WidthSelector';

describe('WidthSelector', () => {
    it('should render labeled width options', () => {
        renderWithGraphene(<WidthSelector value="medium" onChange={jest.fn()} />);

        expect(screen.getByRole('radio', { name: /Wide/i })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /Medium/i })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /Narrow/i })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /Medium/i })).toHaveAttribute('aria-checked', 'true');
    });

    it('should call onChange with narrow when narrow is selected', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<WidthSelector value="medium" onChange={onChange} />);

        await user.click(screen.getByRole('radio', { name: 'Narrow' }));

        expect(onChange).toHaveBeenCalledWith('narrow');
    });

    it('should call onChange with medium when medium is selected', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<WidthSelector value="narrow" onChange={onChange} />);

        await user.click(screen.getByRole('radio', { name: 'Medium' }));

        expect(onChange).toHaveBeenCalledWith('medium');
    });

    it('should call onChange with wide when wide is selected', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<WidthSelector value="narrow" onChange={onChange} />);

        await user.click(screen.getByRole('radio', { name: 'Wide' }));

        expect(onChange).toHaveBeenCalledWith('wide');
    });
});
