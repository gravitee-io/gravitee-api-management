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
import { ThemeProvider } from '@gravitee/graphene-core';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactElement } from 'react';

import { installFakeIndexedDB, resetFakeIndexedDB } from '@apim/portal-editor/testing/fake-indexeddb';
import { PublishDialog } from './PublishDialog';

installFakeIndexedDB();

const portals = [
    { id: 'portal-1', name: 'Payments Portal' },
    { id: 'portal-2', name: 'Fitness Portal' },
] as const;

const defaultProps = {
    open: true,
    onOpenChange: jest.fn(),
    portals: [...portals],
    selectedPortalId: 'portal-1',
    onSelectedPortalIdChange: jest.fn(),
    initialParentId: null,
    apiId: 'api-1',
    isPublishing: false,
    standaloneEditorBaseUrl: '/portal-editor',
    onQuickPublish: jest.fn(),
    onConfirm: jest.fn(),
};

function renderDialog(ui: ReactElement) {
    return render(<ThemeProvider>{ui}</ThemeProvider>);
}

describe('PublishDialog', () => {
    beforeEach(() => {
        resetFakeIndexedDB();
        jest.clearAllMocks();
    });

    it('should call onQuickPublish with the selected portal', async () => {
        const onQuickPublish = jest.fn();

        renderDialog(<PublishDialog {...defaultProps} onQuickPublish={onQuickPublish} />);

        expect(screen.queryByRole('heading', { name: /^quick publish$/i })).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole('button', { name: /^quick publish$/i }));

        await waitFor(() => {
            expect(onQuickPublish).toHaveBeenCalledWith({ portalId: 'portal-1' });
        });
    });

    it('should hide the location panel by default and reveal it when toggled', async () => {
        const user = userEvent.setup();

        renderDialog(<PublishDialog {...defaultProps} />);

        expect(screen.queryByRole('listbox', { name: /publish location/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /^publish$/i })).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: /^choose location$/i }));

        expect(screen.getByRole('listbox', { name: /publish location/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /^publish$/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /open designer/i })).toBeInTheDocument();
    });

    it('should publish with the selected portal and location after expanding', async () => {
        const onConfirm = jest.fn();
        const user = userEvent.setup();

        renderDialog(<PublishDialog {...defaultProps} onConfirm={onConfirm} />);

        await user.click(screen.getByRole('button', { name: /^choose location$/i }));
        fireEvent.click(screen.getByRole('button', { name: /^publish$/i }));

        await waitFor(() => {
            expect(onConfirm).toHaveBeenCalledWith({
                portalId: 'portal-1',
                parentId: null,
                mode: 'replace',
            });
        });
    });

    it('should open the portal designer in a new window', async () => {
        const user = userEvent.setup();
        const openSpy = jest.spyOn(window, 'open').mockImplementation(() => null);

        renderDialog(<PublishDialog {...defaultProps} />);

        await user.click(screen.getByRole('button', { name: /^choose location$/i }));
        fireEvent.click(screen.getByRole('button', { name: /open designer/i }));

        expect(openSpy).toHaveBeenCalledWith('/portal-editor/portals/portal-1/edit', '_blank', 'noopener,noreferrer');

        openSpy.mockRestore();
    });

    it('should not render a cancel button', () => {
        renderDialog(<PublishDialog {...defaultProps} />);

        expect(screen.queryByRole('button', { name: /cancel/i })).not.toBeInTheDocument();
    });
});
