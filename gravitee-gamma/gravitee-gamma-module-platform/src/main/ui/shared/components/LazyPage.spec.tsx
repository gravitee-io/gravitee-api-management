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
import type { ComponentType } from 'react';

import { LazyPage } from './LazyPage';

function PageStub() {
    return <h1>Loaded page</h1>;
}

const PAGE_MODULE: { default: ComponentType } = { default: PageStub };

describe('LazyPage', () => {
    let consoleError: jest.SpyInstance;

    beforeEach(() => {
        consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    });

    afterEach(() => {
        consoleError.mockRestore();
    });

    it('shows a skeleton until the page code arrives, then the page', async () => {
        render(<LazyPage load={() => Promise.resolve(PAGE_MODULE)} />);

        expect(screen.getByLabelText('Loading content')).not.toBeNull();
        expect(await screen.findByRole('heading', { name: 'Loaded page' })).not.toBeNull();
    });

    it('keeps a failed load on the page, with a retry that loads the code again', async () => {
        const chunkError = new Error('Loading chunk 42 failed');
        const load = jest.fn().mockRejectedValueOnce(chunkError).mockResolvedValue(PAGE_MODULE);
        render(
            <>
                <nav>Module navigation</nav>
                <LazyPage load={load} />
            </>,
        );

        expect(await screen.findByText('This page could not be loaded.')).not.toBeNull();
        // The failure stays here: the rest of the module is still on screen.
        expect(screen.getByText('Module navigation')).not.toBeNull();
        expect(consoleError).toHaveBeenCalledWith('A lazily loaded page failed to load', chunkError);

        await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

        expect(await screen.findByRole('heading', { name: 'Loaded page' })).not.toBeNull();
        expect(load).toHaveBeenCalledTimes(2);
    });
});
