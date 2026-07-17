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
jest.mock('@blocknote/react', () => ({
    createReactBlockSpec: (config: Record<string, unknown>, implementation: Record<string, unknown>) => ({
        ...config,
        implementation,
    }),
}));

const mockNavigateToPageSlug = jest.fn();

jest.mock('../../portal-shell/hooks/usePortalPageNavigation', () => ({
    usePortalPageNavigation: () => ({
        navigateToPageSlug: mockNavigateToPageSlug,
        getPagePath: (slug: string) => `/portals/p1/${slug}`,
    }),
}));

jest.mock('../../portal-shell/context/PortalPageContext', () => ({
    usePortalPageOptional: () => ({
        portalId: 'p1',
        navItems: [
            { id: 'page-home', portalId: 'p1', title: 'Home', type: 'PAGE', parentId: null, order: 0, slug: 'home-abc123' },
            { id: 'page-about', portalId: 'p1', title: 'About', type: 'PAGE', parentId: null, order: 1, slug: 'about-def456' },
        ],
    }),
}));

import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { renderPortalUi } from '../../testing/render-portal-ui';
import { ButtonBlock } from './ButtonBlock';

describe('ButtonBlock', () => {
    const createBlock = (overrides: Record<string, string> = {}) => ({
        props: {
            label: 'Get Started',
            link: 'home-abc123',
            appearance: 'filled',
            instanceStyle: '{}',
            pickLinkOpen: 'false',
            ...overrides,
        },
    });

    const createEditor = (isEditable: boolean) => ({
        isEditable,
        updateBlock: jest.fn(),
    });

    function renderButton(isEditable: boolean, blockOverrides: Record<string, string> = {}) {
        const block = createBlock(blockOverrides);
        const editor = createEditor(isEditable);
        const { implementation } = ButtonBlock as { implementation: { render: (props: never) => JSX.Element } };

        function ButtonPreview() {
            return implementation.render({ block, editor } as never);
        }

        return { ...renderPortalUi(<ButtonPreview />), editor, block };
    }

    beforeEach(() => {
        mockNavigateToPageSlug.mockReset();
    });

    it('should render a WYSIWYG button in edit mode', () => {
        renderButton(true);

        expect(screen.getByRole('button', { name: 'Edit Get Started' })).toBeInTheDocument();
        expect(screen.queryByDisplayValue('Get Started')).not.toBeInTheDocument();
        expect(screen.queryByPlaceholderText('Link')).not.toBeInTheDocument();
    });

    it('should enter rename mode on double-click in edit mode', async () => {
        const user = userEvent.setup();
        renderButton(true);

        await user.dblClick(screen.getByLabelText('Edit Get Started'));

        expect(screen.getByRole('textbox', { name: 'Edit Get Started' })).toBeInTheDocument();
    });

    it('should show Edit and Style in the context menu', async () => {
        const user = userEvent.setup();
        renderButton(true);

        await user.pointer({
            keys: '[MouseRight>]',
            target: screen.getByRole('button', { name: 'Edit Get Started' }),
        });

        expect(screen.getByRole('menuitem', { name: 'Edit' })).toBeInTheDocument();
        expect(screen.getByRole('menuitem', { name: 'Style' })).toBeInTheDocument();
    });

    it('should open the link picker from the Edit context menu item', async () => {
        const user = userEvent.setup();
        renderButton(true);

        await user.pointer({
            keys: '[MouseRight>]',
            target: screen.getByRole('button', { name: 'Edit Get Started' }),
        });
        await user.click(screen.getByRole('menuitem', { name: 'Edit' }));

        await waitFor(() => {
            expect(screen.getByRole('textbox', { name: 'Search for a page' })).toBeInTheDocument();
        });
        expect(screen.queryByRole('menuitem', { name: 'Style' })).not.toBeInTheDocument();
    });

    it('should open the link picker when pickLinkOpen is true', async () => {
        const { editor, block } = renderButton(true, { pickLinkOpen: 'true' });

        await waitFor(() => {
            expect(screen.getByRole('textbox', { name: 'Search for a page' })).toBeInTheDocument();
        });
        expect(editor.updateBlock).toHaveBeenCalledWith(block, { props: { pickLinkOpen: 'false' } });
    });

    it('should render a button and navigate on click in read-only mode', () => {
        renderButton(false);

        const button = screen.getByRole('button', { name: 'Get Started' });
        expect(button).toBeInTheDocument();

        fireEvent.click(button);
        expect(mockNavigateToPageSlug).toHaveBeenCalledWith('home-abc123');
    });
});
