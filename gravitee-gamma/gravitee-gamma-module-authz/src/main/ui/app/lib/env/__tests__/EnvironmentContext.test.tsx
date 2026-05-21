import { render, renderHook } from '@testing-library/react';
import { describe, expect, it, vi  } from 'vitest';
import { EnvironmentProvider, useEnvironment } from '../EnvironmentContext';

describe('EnvironmentProvider + useEnvironment', () => {
    it('exposes the value passed via the provider', () => {
        const { result } = renderHook(() => useEnvironment(), {
            wrapper: ({ children }) => <EnvironmentProvider environmentId="staging">{children}</EnvironmentProvider>,
        });
        expect(result.current).toBe('staging');
    });

    it('throws a clear error when the hook runs without a provider', () => {
        // Suppress React's noisy console.error for the expected throw.
        const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
        try {
            expect(() => renderHook(() => useEnvironment())).toThrow(/useEnvironment\(\) called outside EnvironmentProvider/);
        } finally {
            errorSpy.mockRestore();
        }
    });

    it('lets nested providers override the value (innermost wins)', () => {
        const useProbe = () => useEnvironment();
        const { result } = renderHook(useProbe, {
            wrapper: ({ children }) => (
                <EnvironmentProvider environmentId="outer">
                    <EnvironmentProvider environmentId="inner">{children}</EnvironmentProvider>
                </EnvironmentProvider>
            ),
        });
        expect(result.current).toBe('inner');
    });

    it('renders children unchanged', () => {
        const { getByText } = render(
            <EnvironmentProvider environmentId="x">
                <span>hello</span>
            </EnvironmentProvider>,
        );
        expect(getByText('hello')).toBeInTheDocument();
    });
});

