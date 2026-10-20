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

import { render, screen, waitFor } from '@testing-library/react';

import { GraviteeMarkdownPreview } from './GraviteeMarkdownPreview';

// The factory runs when the module is first required, so it records whether the bundle was loaded
// lazily (on render) or eagerly (when the component was imported). An eager import makes this
// suite fail at load with "Cannot access 'mockLoadBundle' before initialization".
const mockRegister = jest.fn<Promise<void>, []>();
const mockLoadBundle = jest.fn(() => ({ registerGmdViewerElement: mockRegister }));
jest.mock('@gravitee/gravitee-markdown-element', () => mockLoadBundle());

const bundleLoadedOnImport = mockLoadBundle.mock.calls.length > 0;

type GmdViewerElement = HTMLElement & { content: string };

const contentSetter = jest.fn<void, [string]>();

class FakeGmdViewer extends HTMLElement {
    private markdown = '';

    get content() {
        return this.markdown;
    }

    set content(value: string) {
        this.markdown = value;
        contentSetter(value);
    }
}

function viewer(container: HTMLElement): GmdViewerElement | null {
    return container.querySelector<GmdViewerElement>('gmd-viewer');
}

describe('GraviteeMarkdownPreview', () => {
    beforeAll(() => {
        if (!customElements.get('gmd-viewer')) {
            customElements.define('gmd-viewer', FakeGmdViewer);
        }
    });

    beforeEach(() => {
        mockRegister.mockReset().mockResolvedValue(undefined);
        contentSetter.mockClear();
    });

    it('does not load the viewer bundle until a preview is rendered', () => {
        expect(bundleLoadedOnImport).toBe(false);
    });

    it('shows a loading status until the viewer is registered, then renders it', async () => {
        let finishRegistering: () => void = () => undefined;
        mockRegister.mockReturnValue(new Promise<void>(resolve => (finishRegistering = resolve)));

        const { container } = render(<GraviteeMarkdownPreview content="# Hello" />);

        expect(screen.getByRole('status', { name: 'Loading preview' })).toBeInTheDocument();
        expect(viewer(container)).not.toBeInTheDocument();

        finishRegistering();

        await waitFor(() => expect(viewer(container)).toBeInTheDocument());
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('passes the markdown through the content property, not an attribute', async () => {
        const { container } = render(<GraviteeMarkdownPreview content="# Hello" />);

        await waitFor(() => expect(viewer(container)).toBeInTheDocument());

        expect(viewer(container)?.content).toBe('# Hello');
        expect(viewer(container)).not.toHaveAttribute('content');
    });

    it('updates the viewer as the markdown changes', async () => {
        const { container, rerender } = render(<GraviteeMarkdownPreview content="# First" />);
        await waitFor(() => expect(viewer(container)).toBeInTheDocument());

        rerender(<GraviteeMarkdownPreview content="# Second" />);

        expect(viewer(container)?.content).toBe('# Second');
    });

    it('shows an error instead of loading forever when the bundle cannot be loaded', async () => {
        const failure = new Error('chunk failed');
        mockRegister.mockRejectedValue(failure);
        const consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);

        const { container } = render(<GraviteeMarkdownPreview content="# Hello" />);

        expect(await screen.findByRole('alert')).toHaveTextContent('The preview could not be loaded');
        expect(viewer(container)).not.toBeInTheDocument();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
        expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('GraviteeMarkdownPreview'), failure);

        consoleError.mockRestore();
    });
});
