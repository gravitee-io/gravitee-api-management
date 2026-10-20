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

import { SpecPreviewFrame } from './SpecPreviewFrame';
import type { SpecPreview } from '../../../hooks/useSpecPreview';

const PAYMENT_EVENTS = { asyncapi: '3.0.0', info: { title: 'Payment events', version: '1.0.0' } };

function renderFrame(preview: SpecPreview) {
    return render(
        <SpecPreviewFrame preview={preview}>{document => <p>Viewer: {(document.info as { title: string }).title}</p>}</SpecPreviewFrame>,
    );
}

/** The closest element around `element` that asks for a light colour scheme, if any. */
function lightSurfaceAround(element: HTMLElement): HTMLElement | null {
    let current: HTMLElement | null = element;
    while (current && current.style.colorScheme !== 'light') {
        current = current.parentElement;
    }
    return current;
}

describe('SpecPreviewFrame', () => {
    beforeEach(() => {
        document.documentElement.classList.add('dark');
    });

    afterEach(() => {
        document.documentElement.classList.remove('dark');
        jest.restoreAllMocks();
    });

    it('renders the viewer on a light surface, as the portal does, even when Gamma is dark', () => {
        renderFrame({ document: PAYMENT_EVENTS, problem: undefined });

        const surface = lightSurfaceAround(screen.getByText('Viewer: Payment events'));
        expect(surface).not.toBeNull();
        expect(surface).toHaveStyle({ backgroundColor: 'rgb(255, 255, 255)' });
        expect(surface?.style.color).not.toBe('');
    });

    it('keeps the out-of-date warning outside the light surface, in the Gamma theme', () => {
        renderFrame({ document: PAYMENT_EVENTS, problem: { status: 'invalid', message: 'bad indentation' } });

        const surface = lightSurfaceAround(screen.getByText('Viewer: Payment events'));
        expect(screen.getByText('Preview not updated: bad indentation')).toBeInTheDocument();
        expect(surface).not.toContainElement(screen.getByText('Preview not updated: bad indentation'));
    });

    it('keeps the error of a viewer that fails outside the light surface, in the Gamma theme', () => {
        jest.spyOn(console, 'error').mockImplementation(() => undefined);
        const FailingViewer = () => {
            throw new Error('viewer failure');
        };

        const { container } = render(
            <SpecPreviewFrame preview={{ document: PAYMENT_EVENTS, problem: undefined }}>{() => <FailingViewer />}</SpecPreviewFrame>,
        );

        expect(screen.getByText('The preview could not be loaded.')).toBeInTheDocument();
        expect(Array.from(container.querySelectorAll<HTMLElement>('*')).some(element => element.style.colorScheme === 'light')).toBe(false);
    });

    it('shows no light surface when there is nothing to preview', () => {
        const { container } = renderFrame({ document: undefined, problem: { status: 'empty' } });

        expect(screen.getByText('Nothing to preview yet.')).toBeInTheDocument();
        expect(Array.from(container.querySelectorAll<HTMLElement>('*')).some(element => element.style.colorScheme === 'light')).toBe(false);
    });
});
