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
import { act, render, screen } from '@testing-library/react';

import { AsyncApiPreview } from './AsyncApiPreview';
import { type AsyncApiViewer, loadAsyncApiViewer } from './specViewers';
import { SPEC_PREVIEW_DELAY_MS } from '../../../hooks/useSpecPreview';

jest.mock('./specViewers', () => ({ loadAsyncApiViewer: jest.fn() }));

const mockLoadAsyncApiViewer = jest.mocked(loadAsyncApiViewer);

const FakeAsyncApiViewer = (({ schema }: { schema: { info: { title: string } } }) => (
    <h2>AsyncAPI: {schema.info.title}</h2>
)) as unknown as AsyncApiViewer;

const PAYMENT_EVENTS = 'asyncapi: 3.0.0\ninfo:\n  title: Payment events\n  version: 1.0.0\n';
const ORDER_EVENTS = 'asyncapi: 3.0.0\ninfo:\n  title: Order events\n  version: 1.0.0\n';
const BROKEN = 'asyncapi: 3.0.0\ninfo:\n  title: Order events\n   version: 1.0.0\n';

describe('AsyncApiPreview', () => {
    beforeEach(() => {
        mockLoadAsyncApiViewer.mockReset().mockResolvedValue(FakeAsyncApiViewer);
    });

    it('shows a loading status, then previews the document with the AsyncAPI viewer', async () => {
        render(<AsyncApiPreview content={PAYMENT_EVENTS} />);

        expect(screen.getByRole('status', { name: 'Loading preview' })).toBeInTheDocument();
        expect(await screen.findByRole('heading', { name: 'AsyncAPI: Payment events' })).toBeInTheDocument();
        expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    describe('following the content', () => {
        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('previews the new document once typing has paused', async () => {
            const { rerender } = render(<AsyncApiPreview content={PAYMENT_EVENTS} />);
            await screen.findByRole('heading', { name: 'AsyncAPI: Payment events' });

            rerender(<AsyncApiPreview content={ORDER_EVENTS} />);
            act(() => {
                jest.advanceTimersByTime(SPEC_PREVIEW_DELAY_MS);
            });

            expect(screen.getByRole('heading', { name: 'AsyncAPI: Order events' })).toBeInTheDocument();
        });

        it('keeps the last preview while the content does not parse, saying why it is out of date', async () => {
            const { rerender } = render(<AsyncApiPreview content={PAYMENT_EVENTS} />);
            await screen.findByRole('heading', { name: 'AsyncAPI: Payment events' });

            rerender(<AsyncApiPreview content={BROKEN} />);
            act(() => {
                jest.advanceTimersByTime(SPEC_PREVIEW_DELAY_MS);
            });

            expect(screen.getByRole('alert')).toHaveTextContent(/^Preview not updated: YAML error on line 4: /);
            expect(screen.getByRole('heading', { name: 'AsyncAPI: Payment events' })).toBeInTheDocument();
        });
    });

    it('explains why a document that never parsed cannot be previewed, without loading the viewer', () => {
        render(<AsyncApiPreview content={'openapi: 3.0.3\n'} />);

        expect(screen.getByRole('alert')).toHaveTextContent(
            "This doesn't look like an AsyncAPI document: it needs an asyncapi field at the top level.",
        );
        expect(mockLoadAsyncApiViewer).not.toHaveBeenCalled();
    });

    describe('when the viewer fails', () => {
        let consoleError: jest.SpyInstance;

        beforeEach(() => {
            consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        });

        afterEach(() => {
            consoleError.mockRestore();
        });

        it('shows an error when the viewer cannot be loaded', async () => {
            const failure = new Error('chunk failed');
            mockLoadAsyncApiViewer.mockRejectedValue(failure);

            render(<AsyncApiPreview content={PAYMENT_EVENTS} />);

            expect(await screen.findByRole('alert')).toHaveTextContent('The preview could not be loaded.');
            expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('AsyncApiPreview'), failure);
        });

        it('shows an error instead of breaking the screen when the viewer throws', async () => {
            const Throwing = (() => {
                throw new Error('cannot render');
            }) as unknown as AsyncApiViewer;
            mockLoadAsyncApiViewer.mockResolvedValue(Throwing);

            render(<AsyncApiPreview content={PAYMENT_EVENTS} />);

            expect(await screen.findByRole('alert')).toHaveTextContent('The preview could not be loaded.');
        });
    });
});
