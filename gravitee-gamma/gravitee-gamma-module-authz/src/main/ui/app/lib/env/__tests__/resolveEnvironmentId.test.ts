import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { FALLBACK_ENVIRONMENT_ID, __resetFallbackWarnedForTests, resolveEnvironmentId } from '../resolveEnvironmentId';

describe('resolveEnvironmentId', () => {
    let warnSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        __resetFallbackWarnedForTests();
        warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    });

    afterEach(() => {
        warnSpy.mockRestore();
    });

    it('returns the host prop when provided and non-blank', () => {
        expect(resolveEnvironmentId({ hostProp: 'staging', url: '?env=other' })).toBe('staging');
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('trims surrounding whitespace from the host prop', () => {
        expect(resolveEnvironmentId({ hostProp: '  prod  ' })).toBe('prod');
    });

    it('falls through to URL ?env when host prop is blank', () => {
        expect(resolveEnvironmentId({ hostProp: '   ', url: '?env=urlenv' })).toBe('urlenv');
        expect(warnSpy).not.toHaveBeenCalled();
    });

    it('falls through to URL ?env when host prop is null', () => {
        expect(resolveEnvironmentId({ hostProp: null, url: '?env=fromurl' })).toBe('fromurl');
    });

    it('falls back to DEFAULT and WARNs once when nothing else is available', () => {
        expect(resolveEnvironmentId({ hostProp: undefined, url: '' })).toBe(FALLBACK_ENVIRONMENT_ID);
        expect(warnSpy).toHaveBeenCalledTimes(1);
        const msg = warnSpy.mock.calls[0]?.[0] as string;
        expect(msg).toContain("falling back to 'DEFAULT'");
    });

    it('only warns once per process even on repeated fallback', () => {
        expect(resolveEnvironmentId({ url: '' })).toBe(FALLBACK_ENVIRONMENT_ID);
        expect(resolveEnvironmentId({ url: '' })).toBe(FALLBACK_ENVIRONMENT_ID);
        expect(resolveEnvironmentId({ url: '' })).toBe(FALLBACK_ENVIRONMENT_ID);
        expect(warnSpy).toHaveBeenCalledTimes(1);
    });

    it('treats malformed URL as no override and falls back', () => {
        // URLSearchParams will not throw on this, but the explicit guard handles
        // anything that does. Any input lacking `env=` resolves to fallback.
        expect(resolveEnvironmentId({ hostProp: undefined, url: '?garbage' })).toBe(FALLBACK_ENVIRONMENT_ID);
    });

    it('accepts a URL string with embedded path before the query', () => {
        expect(resolveEnvironmentId({ url: '/some/path?env=branchA' })).toBe('branchA');
    });
});
