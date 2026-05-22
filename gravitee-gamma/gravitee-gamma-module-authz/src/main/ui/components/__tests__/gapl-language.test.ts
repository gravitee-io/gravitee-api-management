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

    it('the keyword set covers `permit`, `forbid`, `principal`, `action`, `resource`, `when`', () => {
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
