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

import { LayoutSelector } from './LayoutSelector';

describe('LayoutSelector', () => {
    it('should call onChange with header-content-footer layout', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<LayoutSelector value="sidebar-content" onChange={onChange} />);

        await user.click(screen.getByRole('radio', { name: 'Header, content, footer' }));

        expect(onChange).toHaveBeenCalledWith('header-content-footer');
    });

    it('should call onChange with sidebar-content layout', async () => {
        const user = userEvent.setup();
        const onChange = jest.fn();

        renderWithGraphene(<LayoutSelector value="header-content-footer" onChange={onChange} />);

        await user.click(screen.getByRole('radio', { name: 'Sidebar and content' }));

        expect(onChange).toHaveBeenCalledWith('sidebar-content');
    });
});
