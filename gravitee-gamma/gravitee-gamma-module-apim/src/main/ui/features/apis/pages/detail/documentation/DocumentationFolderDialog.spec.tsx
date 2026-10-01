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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { DocumentationFolderDialog } from './DocumentationFolderDialog';

jest.mock('@gravitee/graphene-core/icons', () => new Proxy({}, { get: () => () => null }));

beforeAll(() => {
    global.ResizeObserver = class {
        observe() {}
        unobserve() {}
        disconnect() {}
    };
});

describe('DocumentationFolderDialog', () => {
    it('submits name and public visibility by default', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn();
        render(
            <DocumentationFolderDialog
                open
                existingNames={[]}
                readOnly={false}
                isSaving={false}
                onClose={jest.fn()}
                onSubmit={onSubmit}
            />,
        );

        await user.type(screen.getByRole('textbox', { name: /name/i }), 'Guides');
        await user.click(screen.getByRole('button', { name: /create folder/i }));

        expect(onSubmit).toHaveBeenCalledWith({ name: 'Guides', visibility: 'PUBLIC' });
    });

    it('locks visibility to private when the parent folder requires authentication', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn();
        render(
            <DocumentationFolderDialog
                open
                parentForcesPrivate
                existingNames={[]}
                readOnly={false}
                isSaving={false}
                onClose={jest.fn()}
                onSubmit={onSubmit}
            />,
        );

        expect(screen.getByText(/folder requiring authentication/i)).toBeInTheDocument();
        const authSwitch = screen.getByRole('switch', { name: /require authentication/i });
        expect(authSwitch).toBeDisabled();
        expect(authSwitch).toBeChecked();

        await user.type(screen.getByRole('textbox', { name: /name/i }), 'Nested');
        await user.click(screen.getByRole('button', { name: /create folder/i }));

        expect(onSubmit).toHaveBeenCalledWith({ name: 'Nested', visibility: 'PRIVATE' });
    });

    it('allows requiring authentication when creating a root folder', async () => {
        const user = userEvent.setup();
        const onSubmit = jest.fn();
        render(
            <DocumentationFolderDialog
                open
                existingNames={[]}
                readOnly={false}
                isSaving={false}
                onClose={jest.fn()}
                onSubmit={onSubmit}
            />,
        );

        await user.type(screen.getByRole('textbox', { name: /name/i }), 'Private guides');
        await user.click(screen.getByRole('switch', { name: /require authentication/i }));
        await user.click(screen.getByRole('button', { name: /create folder/i }));

        expect(onSubmit).toHaveBeenCalledWith({ name: 'Private guides', visibility: 'PRIVATE' });
    });
});
