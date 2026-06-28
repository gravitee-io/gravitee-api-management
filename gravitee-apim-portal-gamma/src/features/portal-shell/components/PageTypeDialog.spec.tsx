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

import { USER_MENU_PAGE_TYPE_OPTIONS } from '../utils/page-type-options';
import { PageTypeDialog } from './PageTypeDialog';

describe('PageTypeDialog', () => {
    it('should list all page types with labels', () => {
        renderWithGraphene(<PageTypeDialog open onOpenChange={jest.fn()} onSelect={jest.fn()} />);

        expect(screen.getByRole('heading', { name: 'Choose page type' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Block/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /OpenAPI/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /HTML/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /AsyncAPI/i })).toBeInTheDocument();
    });

    it('should call onSelect with the chosen page type', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();

        renderWithGraphene(<PageTypeDialog open onOpenChange={jest.fn()} onSelect={onSelect} />);

        await user.click(screen.getByRole('option', { name: /OpenAPI/i }));

        expect(onSelect).toHaveBeenCalledWith('OPENAPI');
    });

    it('should render only the provided page type options', () => {
        renderWithGraphene(
            <PageTypeDialog
                open
                onOpenChange={jest.fn()}
                onSelect={jest.fn()}
                options={USER_MENU_PAGE_TYPE_OPTIONS}
            />,
        );

        expect(screen.getByRole('option', { name: /Block/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /HTML/i })).toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /OpenAPI/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('option', { name: /AsyncAPI/i })).not.toBeInTheDocument();
    });
});
