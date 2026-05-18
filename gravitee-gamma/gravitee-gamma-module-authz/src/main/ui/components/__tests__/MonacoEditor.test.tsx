import { render } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { MonacoEditor } from '../MonacoEditor';

// Capture the onMount handler the component hands to the underlying Editor so
// tests can invoke it manually (jsdom can't run the real canvas-based editor).
const capturedOnMount: { current: ((editor: unknown, monaco: unknown) => void) | null } = {
    current: null,
};

// Capture the latest options prop so tests can assert layout-critical defaults.
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

        // Simulate Monaco mounting.
        expect(capturedOnMount.current).toBeTypeOf('function');
        const fakeEditor = { id: 'editor' };
        const fakeMonaco = { id: 'monaco' };
        capturedOnMount.current!(fakeEditor, fakeMonaco);

        expect(registerSpy).toHaveBeenCalledWith(fakeMonaco);
        expect(onMount).toHaveBeenCalledWith(fakeEditor, fakeMonaco);
    });

    it('passes automaticLayout: true so Monaco recomputes layout on container reflow', () => {
        // Bug A: without this flag the editor collapses to ~20px after re-layouts
        // (e.g. revisiting /schema or after Validate). We assert the option here
        // because jsdom can't render the real editor to verify visually.
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

    it('uses the latest onMount when the prop changes (not frozen by useCallback)', () => {
        const first = vi.fn();
        const second = vi.fn();
        const { rerender } = render(<MonacoEditor value="" onMount={first} />);

        // Re-render with a different onMount before mount fires.
        rerender(<MonacoEditor value="" onMount={second} />);

        // Trigger the mount handler captured from the most recent render.
        expect(capturedOnMount.current).toBeTypeOf('function');
        capturedOnMount.current!({}, {});

        expect(second).toHaveBeenCalledTimes(1);
        // First handler should NOT be called for the post-rerender mount.
        expect(first).not.toHaveBeenCalled();
    });
});
