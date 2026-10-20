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
import { act, renderHook } from '@testing-library/react';

import { SPEC_PREVIEW_DELAY_MS, useSpecPreview } from './useSpecPreview';

const V1 = 'openapi: 3.0.3\ninfo:\n  title: First\n';
const V2 = 'openapi: 3.0.3\ninfo:\n  title: Second\n';
const BROKEN = 'openapi: 3.0.3\ninfo:\n  title: First\n   version: 1\n';

function renderPreview(initial: string) {
    return renderHook(({ content }) => useSpecPreview('OPENAPI', content), { initialProps: { content: initial } });
}

function wait(ms: number) {
    act(() => {
        jest.advanceTimersByTime(ms);
    });
}

describe('useSpecPreview', () => {
    beforeEach(() => {
        jest.useFakeTimers();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    it('previews the content it opens with straight away', () => {
        const { result } = renderPreview(V1);

        expect(result.current).toEqual({ document: { openapi: '3.0.3', info: { title: 'First' } }, problem: undefined });
    });

    it('previews new content once typing has paused', () => {
        const { result, rerender } = renderPreview(V1);

        rerender({ content: V2 });
        wait(SPEC_PREVIEW_DELAY_MS - 1);
        expect(result.current.document).toEqual({ openapi: '3.0.3', info: { title: 'First' } });

        wait(1);
        expect(result.current.document).toEqual({ openapi: '3.0.3', info: { title: 'Second' } });
    });

    it('waits again after every change', () => {
        const { result, rerender } = renderPreview(V1);

        rerender({ content: BROKEN });
        wait(SPEC_PREVIEW_DELAY_MS - 100);
        rerender({ content: V2 });
        wait(SPEC_PREVIEW_DELAY_MS - 1);

        expect(result.current).toEqual({ document: { openapi: '3.0.3', info: { title: 'First' } }, problem: undefined });
    });

    it('keeps the last valid document while the content does not parse, with the reason', () => {
        const { result, rerender } = renderPreview(V1);

        rerender({ content: BROKEN });
        wait(SPEC_PREVIEW_DELAY_MS);

        expect(result.current).toEqual({
            document: { openapi: '3.0.3', info: { title: 'First' } },
            problem: { status: 'invalid', message: expect.stringMatching(/^YAML error on line 4: /) },
        });
    });

    it('drops the reason once the content parses again', () => {
        const { result, rerender } = renderPreview(V1);
        rerender({ content: BROKEN });
        wait(SPEC_PREVIEW_DELAY_MS);

        rerender({ content: V2 });
        wait(SPEC_PREVIEW_DELAY_MS);

        expect(result.current).toEqual({ document: { openapi: '3.0.3', info: { title: 'Second' } }, problem: undefined });
    });

    it('gives the reason and no document when the content has never parsed', () => {
        const { result } = renderPreview('asyncapi: 3.0.0\n');

        expect(result.current).toEqual({ document: undefined, problem: { status: 'invalid', message: expect.any(String) } });
    });

    it('previews nothing once the content is emptied, even after a valid document', () => {
        const { result, rerender } = renderPreview(V1);

        rerender({ content: '' });
        wait(SPEC_PREVIEW_DELAY_MS);

        expect(result.current).toEqual({ document: undefined, problem: { status: 'empty' } });
    });
});
