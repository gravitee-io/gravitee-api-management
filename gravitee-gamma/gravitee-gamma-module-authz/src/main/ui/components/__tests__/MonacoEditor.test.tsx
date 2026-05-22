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
import { render } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MonacoEditor } from '../MonacoEditor';

const capturedOnMount: { current: ((editor: unknown, monaco: unknown) => void) | null } = {
    current: null,
};

const capturedOptions: { current: Record<string, unknown> | null } = {
    current: null,
};

vi.mock('@monaco-editor/react', () => ({
    __esModule: true,
    default: (props: { onMount?: (editor: unknown, monaco: unknown) => void; options?: Record<string, unknown> }) => {
        capturedOnMount.current = props.onMount ?? null;
        capturedOptions.current = props.options ?? null;
        return <div data-testid="monaco-stub" />;
    },
}));

const registerSpy = vi.fn();
vi.mock('../gapl-language', () => ({
    GAPL_LANGUAGE_ID: 'gapl',
    registerGaplLanguage: (m: unknown) => registerSpy(m),
}));

describe('MonacoEditor', () => {
    it('renders the underlying Editor and registers GAPL on mount', () => {
        const onMount = vi.fn();
        const { getByTestId } = render(<MonacoEditor value="" onMount={onMount} />);

        expect(getByTestId('monaco-stub')).toBeInTheDocument();

        expect(capturedOnMount.current).toBeTypeOf('function');
        const fakeEditor = { id: 'editor' };
        const fakeMonaco = { id: 'monaco' };
        capturedOnMount.current!(fakeEditor, fakeMonaco);

        expect(registerSpy).toHaveBeenCalledWith(fakeMonaco);
        expect(onMount).toHaveBeenCalledWith(fakeEditor, fakeMonaco);
    });

    it('passes automaticLayout: true so Monaco recomputes layout on container reflow', () => {
        render(<MonacoEditor value="" />);
        expect(capturedOptions.current).not.toBeNull();
        expect(capturedOptions.current).toMatchObject({ automaticLayout: true });
    });

    it('lets caller-supplied options override defaults but keeps automaticLayout when not overridden', () => {
        render(<MonacoEditor value="" options={{ fontSize: 18 }} />);
        expect(capturedOptions.current).toMatchObject({
            automaticLayout: true,
            fontSize: 18,
        });
    });

    it('uses the latest onMount when the prop changes', () => {
        const first = vi.fn();
        const second = vi.fn();
        const { rerender } = render(<MonacoEditor value="" onMount={first} />);

        rerender(<MonacoEditor value="" onMount={second} />);

        expect(capturedOnMount.current).toBeTypeOf('function');
        capturedOnMount.current!({}, {});

        expect(second).toHaveBeenCalledTimes(1);
        expect(first).not.toHaveBeenCalled();
    });
});
