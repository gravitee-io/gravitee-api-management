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
import type { SwaggerUIOptions } from 'swagger-ui';

import { OpenApiPreview } from './OpenApiPreview';
import { loadRedoc, loadSwaggerUi, type RedocLibrary } from './specViewers';
import { SPEC_PREVIEW_DELAY_MS } from '../../../hooks/useSpecPreview';
import type { PortalPageOpenApiConfiguration } from '../../../types/apiDocumentation';

jest.mock('./specViewers', () => ({ loadSwaggerUi: jest.fn(), loadRedoc: jest.fn() }));

const mockLoadSwaggerUi = jest.mocked(loadSwaggerUi);
const mockLoadRedoc = jest.mocked(loadRedoc);

type Spec = { info: { title: string }; servers?: { url: string }[] };

// The fakes draw what the real viewers would be given, so the specs read like what a user sees.
function draw(node: HTMLElement, viewer: string, spec: Spec) {
    const servers = spec.servers?.map(server => server.url).join(', ') ?? 'from the document';
    node.innerHTML = `<h2>${viewer}: ${spec.info.title}</h2><p>Servers: ${servers}</p>`;
}

const swaggerUi = jest.fn((options: SwaggerUIOptions) => {
    const node = options.domNode as HTMLElement;
    draw(node, 'Swagger UI', options.spec as Spec);
    return { specActions: { updateSpec: (text: string) => draw(node, 'Swagger UI', JSON.parse(text) as Spec) } };
});

// Redoc renders a store built from the document, which holds window listeners until it is disposed.
class FakeRedocStore {
    static created: FakeRedocStore[] = [];
    disposed = false;

    constructor(readonly spec: Spec) {
        FakeRedocStore.created.push(this);
    }

    dispose() {
        this.disposed = true;
    }
}

function FakeRedoc({ store }: { store: FakeRedocStore }) {
    const servers = store.spec.servers?.map(server => server.url).join(', ') ?? 'from the document';
    return (
        <>
            <h2>Redoc: {store.spec.info.title}</h2>
            <p>Servers: {servers}</p>
        </>
    );
}

const redoc = { AppStore: FakeRedocStore, Redoc: FakeRedoc } as unknown as RedocLibrary;

const PETSTORE = 'openapi: 3.0.3\ninfo:\n  title: Petstore\n  version: 1.0.0\nservers:\n  - url: https://petstore.example.com\npaths: {}\n';
const PAYMENTS = 'openapi: 3.0.3\ninfo:\n  title: Payments\n  version: 1.0.0\npaths: {}\n';
const BROKEN = 'openapi: 3.0.3\ninfo:\n  title: Payments\n   version: 1.0.0\n';

const SWAGGER: PortalPageOpenApiConfiguration = { viewer: 'SWAGGER' };
const REDOC: PortalPageOpenApiConfiguration = { viewer: 'REDOC' };

function wait(ms: number) {
    act(() => {
        jest.advanceTimersByTime(ms);
    });
}

describe('OpenApiPreview', () => {
    beforeEach(() => {
        swaggerUi.mockClear();
        FakeRedocStore.created = [];
        mockLoadSwaggerUi.mockReset().mockResolvedValue(swaggerUi);
        mockLoadRedoc.mockReset().mockResolvedValue(redoc);
    });

    describe('choosing the viewer as the portal does', () => {
        it('previews in Redoc when the page is configured for Redoc, the server default', async () => {
            render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);

            expect(await screen.findByRole('heading', { name: 'Redoc: Petstore' })).toBeInTheDocument();
            expect(mockLoadSwaggerUi).not.toHaveBeenCalled();
        });

        it('previews in Swagger UI when the page is configured for Swagger UI', async () => {
            render(<OpenApiPreview content={PETSTORE} configuration={SWAGGER} />);

            expect(await screen.findByRole('heading', { name: 'Swagger UI: Petstore' })).toBeInTheDocument();
            expect(mockLoadRedoc).not.toHaveBeenCalled();
        });

        it('previews in Swagger UI when the page has no configuration', async () => {
            render(<OpenApiPreview content={PETSTORE} />);

            expect(await screen.findByRole('heading', { name: 'Swagger UI: Petstore' })).toBeInTheDocument();
        });

        it('shows a loading status until the viewer is ready', async () => {
            render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);

            expect(screen.getByRole('status', { name: 'Loading preview' })).toBeInTheDocument();
            await screen.findByRole('heading', { name: 'Redoc: Petstore' });
            expect(screen.queryByRole('status')).not.toBeInTheDocument();
        });
    });

    describe('applying the configuration', () => {
        it('passes the Swagger UI options of the configuration', async () => {
            const configuration: PortalPageOpenApiConfiguration = {
                viewer: 'SWAGGER',
                docExpansion: 'list',
                displayOperationId: true,
                enableFiltering: true,
                showExtensions: true,
                showCommonExtensions: true,
                maxDisplayedTags: 3,
            };

            render(<OpenApiPreview content={PETSTORE} configuration={configuration} />);
            await screen.findByRole('heading', { name: 'Swagger UI: Petstore' });

            expect(swaggerUi).toHaveBeenCalledWith(
                expect.objectContaining({
                    docExpansion: 'list',
                    displayOperationId: true,
                    filter: true,
                    showExtensions: true,
                    showCommonExtensions: true,
                    maxDisplayedTags: 3,
                }),
            );
        });

        it('falls back to the Swagger UI defaults the portal editor uses', async () => {
            render(<OpenApiPreview content={PETSTORE} configuration={{ viewer: 'SWAGGER', maxDisplayedTags: -1 }} />);
            await screen.findByRole('heading', { name: 'Swagger UI: Petstore' });

            expect(swaggerUi).toHaveBeenCalledWith(
                expect.objectContaining({
                    docExpansion: 'none',
                    displayOperationId: false,
                    filter: false,
                    showExtensions: false,
                    showCommonExtensions: false,
                    maxDisplayedTags: undefined,
                }),
            );
        });

        it.each([
            ['Swagger UI', SWAGGER],
            ['Redoc', REDOC],
        ] as const)('points %s at the configured base URL', async (viewer, configuration) => {
            render(
                <OpenApiPreview
                    content={PETSTORE}
                    configuration={{ ...configuration, tryItURL: 'https://gateway.example.com/petstore' }}
                />,
            );

            expect(await screen.findByText('Servers: https://gateway.example.com/petstore')).toBeInTheDocument();
            expect(screen.getByRole('heading', { name: `${viewer}: Petstore` })).toBeInTheDocument();
        });

        it('keeps the servers of the document when Swagger UI uses the API entrypoints instead', async () => {
            render(
                <OpenApiPreview
                    content={PETSTORE}
                    configuration={{ viewer: 'SWAGGER', entrypointsAsServers: true, tryItURL: 'https://gateway.example.com/petstore' }}
                />,
            );

            expect(await screen.findByText('Servers: https://petstore.example.com')).toBeInTheDocument();
        });
    });

    describe('following the content', () => {
        beforeEach(() => {
            jest.useFakeTimers();
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('updates Swagger UI in place once typing has paused', async () => {
            const { rerender } = render(<OpenApiPreview content={PETSTORE} configuration={SWAGGER} />);
            await screen.findByRole('heading', { name: 'Swagger UI: Petstore' });

            rerender(<OpenApiPreview content={PAYMENTS} configuration={SWAGGER} />);
            wait(SPEC_PREVIEW_DELAY_MS);

            expect(screen.getByRole('heading', { name: 'Swagger UI: Payments' })).toBeInTheDocument();
            expect(swaggerUi).toHaveBeenCalledTimes(1);
        });

        it('renders Redoc again once typing has paused', async () => {
            const { rerender } = render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);
            await screen.findByRole('heading', { name: 'Redoc: Petstore' });

            rerender(<OpenApiPreview content={PAYMENTS} configuration={REDOC} />);
            wait(SPEC_PREVIEW_DELAY_MS);

            expect(await screen.findByRole('heading', { name: 'Redoc: Payments' })).toBeInTheDocument();
        });

        it('releases the previous Redoc document once the next one is shown', async () => {
            const { rerender } = render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);
            await screen.findByRole('heading', { name: 'Redoc: Petstore' });

            rerender(<OpenApiPreview content={PAYMENTS} configuration={REDOC} />);
            wait(SPEC_PREVIEW_DELAY_MS);
            await screen.findByRole('heading', { name: 'Redoc: Payments' });

            expect(FakeRedocStore.created.map(store => [store.spec.info.title, store.disposed])).toEqual([
                ['Petstore', true],
                ['Payments', false],
            ]);
        });

        it('keeps the last preview while the content does not parse, saying why it is out of date', async () => {
            const { rerender } = render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);
            await screen.findByRole('heading', { name: 'Redoc: Petstore' });

            rerender(<OpenApiPreview content={BROKEN} configuration={REDOC} />);
            wait(SPEC_PREVIEW_DELAY_MS);

            expect(screen.getByRole('alert')).toHaveTextContent(/^Preview not updated: YAML error on line 4: bad indentation/);
            expect(screen.getByRole('heading', { name: 'Redoc: Petstore' })).toBeInTheDocument();
        });
    });

    it('releases the Redoc document when the preview closes', async () => {
        const { unmount } = render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);
        await screen.findByRole('heading', { name: 'Redoc: Petstore' });

        unmount();

        expect(FakeRedocStore.created.every(store => store.disposed)).toBe(true);
    });

    describe('when there is nothing to show', () => {
        it('explains why a document that never parsed cannot be previewed', () => {
            render(<OpenApiPreview content={'asyncapi: 3.0.0\n'} configuration={REDOC} />);

            expect(screen.getByRole('alert')).toHaveTextContent(
                "This doesn't look like an OpenAPI document: it needs an openapi or swagger field at the top level.",
            );
            expect(mockLoadRedoc).not.toHaveBeenCalled();
        });

        it('says there is nothing to preview for an empty page', () => {
            render(<OpenApiPreview content="" configuration={REDOC} />);

            expect(screen.getByText('Nothing to preview yet.')).toBeInTheDocument();
            expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        });
    });

    describe('when the viewer fails', () => {
        let consoleError: jest.SpyInstance;

        beforeEach(() => {
            consoleError = jest.spyOn(console, 'error').mockImplementation(() => undefined);
        });

        afterEach(() => {
            consoleError.mockRestore();
        });

        it.each([
            ['Swagger UI', SWAGGER, mockLoadSwaggerUi],
            ['Redoc', REDOC, mockLoadRedoc],
        ] as const)('shows an error when %s cannot be loaded', async (_viewer, configuration, load) => {
            const failure = new Error('chunk failed');
            load.mockRejectedValue(failure);

            render(<OpenApiPreview content={PETSTORE} configuration={configuration} />);

            expect(await screen.findByRole('alert')).toHaveTextContent('The preview could not be loaded.');
            expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('OpenApiPreview'), failure);
        });

        it('shows an error when Redoc cannot read the document', async () => {
            const failure = new Error('Invalid reference token');
            mockLoadRedoc.mockResolvedValue({
                ...redoc,
                AppStore: class {
                    constructor() {
                        throw failure;
                    }
                },
            } as unknown as RedocLibrary);

            render(<OpenApiPreview content={PETSTORE} configuration={REDOC} />);

            expect(await screen.findByRole('alert')).toHaveTextContent('The preview could not be loaded.');
            expect(consoleError).toHaveBeenCalledWith(expect.stringContaining('OpenApiPreview'), failure);
        });
    });
});
