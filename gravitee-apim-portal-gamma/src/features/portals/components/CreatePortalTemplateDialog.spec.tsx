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

import { CreatePortalTemplateDialog } from './CreatePortalTemplateDialog';

describe('CreatePortalTemplateDialog', () => {
    it('should list all portal templates with labels', () => {
        renderWithGraphene(
            <CreatePortalTemplateDialog open isPending={false} onOpenChange={jest.fn()} onSelect={jest.fn()} />,
        );

        expect(screen.getByRole('heading', { name: 'Choose portal template' })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Blank/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Starter/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Payments API Portal/i })).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Active Fitness Partner APIs/i })).toBeInTheDocument();
    });

    it('should call onSelect with the chosen template id', async () => {
        const user = userEvent.setup();
        const onSelect = jest.fn();

        renderWithGraphene(
            <CreatePortalTemplateDialog open isPending={false} onOpenChange={jest.fn()} onSelect={onSelect} />,
        );

        await user.click(screen.getByRole('option', { name: /Payments API Portal/i }));

        expect(onSelect).toHaveBeenCalledWith('payments');
    });

    it('should disable template options while creation is pending', () => {
        renderWithGraphene(
            <CreatePortalTemplateDialog open isPending onOpenChange={jest.fn()} onSelect={jest.fn()} />,
        );

        expect(screen.getByText('Creating portal…')).toBeInTheDocument();
        expect(screen.getByRole('option', { name: /Blank/i })).toBeDisabled();
    });
});
