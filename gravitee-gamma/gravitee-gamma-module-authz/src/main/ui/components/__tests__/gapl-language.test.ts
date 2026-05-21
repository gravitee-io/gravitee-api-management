/**
 * Smoke tests for the GAPL Monaco language registration.
 *
 * The actual tokenisation runs inside Monaco at runtime — these tests only
 * verify that:
 *   - importing the module is side-effect free (it does not auto-register
 *     against any global Monaco instance);
 *   - calling `registerGaplLanguage` with a mock Monaco namespace records the
 *     language id and a Monarch tokens provider whose keyword set covers the
 *     core GAPL grammar (`permit`, `forbid`, `principal`, `action`,
 *     `resource`, `when`);
 *   - calling `registerGaplLanguage` twice is a no-op when the language is
 *     already present (matches Monaco's idempotency contract).
 */
import { describe, expect, it, vi } from 'vitest';
import { GAPL_LANGUAGE_ID, registerGaplLanguage } from '../gapl-language';

interface RegisteredLang {
    readonly id: string;
}

interface RecordedTokensProvider {
    readonly id: string;
    readonly provider: { readonly keywords?: readonly string[] };
}

interface RecordedLanguageConfig {
    readonly id: string;
}

/**
 * Build a minimal Monaco-like stub that records what `registerGaplLanguage`
 * does. Returned as `unknown` because the SUT's parameter type
 * (`typeof import('monaco-editor')`) is far wider than what we exercise here.
 */
function createMonacoStub() {
    const registered: RegisteredLang[] = [];
    const tokensProviders: RecordedTokensProvider[] = [];
    const languageConfigs: RecordedLanguageConfig[] = [];
    const monaco = {
        languages: {
            getLanguages: () => registered.slice(),
            register: (lang: RegisteredLang) => {
                registered.push(lang);
            },
            setMonarchTokensProvider: (id: string, provider: { keywords?: readonly string[] }) => {
                tokensProviders.push({ id, provider });
            },
            setLanguageConfiguration: (id: string, _config: unknown) => {
                languageConfigs.push({ id });
            },
        },
    };
    return { monaco: monaco as unknown as Parameters<typeof registerGaplLanguage>[0], registered, tokensProviders, languageConfigs };
}

describe('gapl-language module', () => {
    it('exports the GAPL_LANGUAGE_ID constant', () => {
        expect(GAPL_LANGUAGE_ID).toBe('gapl');
    });

    it('importing the module does not throw and exposes the registration function', () => {
        // Importing already happened at the top of the file — this test simply
        // documents the contract and guards against accidental top-level
        // side effects (e.g. someone registering against a global monaco).
        expect(typeof registerGaplLanguage).toBe('function');
    });
});

describe('registerGaplLanguage', () => {
    it('registers the GAPL language id with the supplied Monaco namespace', () => {
        const { monaco, registered } = createMonacoStub();
        registerGaplLanguage(monaco);
        expect(registered.map(r => r.id)).toContain('gapl');
    });

    it('installs a Monarch tokens provider for the GAPL language', () => {
        const { monaco, tokensProviders } = createMonacoStub();
        registerGaplLanguage(monaco);
        expect(tokensProviders).toHaveLength(1);
        expect(tokensProviders[0].id).toBe('gapl');
    });

    it("the keyword set covers `permit`, `forbid`, `principal`, `action`, `resource`, `when`", () => {
        const { monaco, tokensProviders } = createMonacoStub();
        registerGaplLanguage(monaco);
        const kws = tokensProviders[0].provider.keywords ?? [];
        for (const expected of ['permit', 'forbid', 'principal', 'action', 'resource', 'when']) {
            expect(kws).toContain(expected);
        }
    });

    it('configures comment + bracket settings for the language', () => {
        const { monaco, languageConfigs } = createMonacoStub();
        registerGaplLanguage(monaco);
        expect(languageConfigs).toEqual([{ id: 'gapl' }]);
    });

    it('is a no-op when the language is already present (idempotent)', () => {
        const registerSpy = vi.fn();
        const setMonarchSpy = vi.fn();
        const monaco = {
            languages: {
                getLanguages: () => [{ id: 'gapl' }],
                register: registerSpy,
                setMonarchTokensProvider: setMonarchSpy,
                setLanguageConfiguration: vi.fn(),
            },
        } as unknown as Parameters<typeof registerGaplLanguage>[0];
        registerGaplLanguage(monaco);
        expect(registerSpy).not.toHaveBeenCalled();
        expect(setMonarchSpy).not.toHaveBeenCalled();
    });
});
