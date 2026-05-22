import prettier from 'eslint-config-prettier';
import grapheneConfig from '@gravitee/graphene-core/eslint';

export default [
    { ignores: ['dist/', 'target/', 'rsbuild.config.ts', 'vitest.config.ts'] },
    ...grapheneConfig,
    prettier,
    /*
     * Story G1: forbid the literal `'DEFAULT'` env id from production source. It survives
     * only as the fallback inside `resolveEnvironmentId` (allowlisted) and as test
     * expectations in `__tests__`. Any new occurrence in `app/**` should fail CI.
     */
    {
        files: ['src/main/ui/app/**/*.{ts,tsx}'],
        ignores: [
            'src/main/ui/app/**/__tests__/**',
            // The hook + provider intentionally name the constant in JSDoc / error message.
            'src/main/ui/app/lib/env/EnvironmentContext.tsx',
        ],
        rules: {
            'no-restricted-syntax': [
                'error',
                {
                    selector: 'Literal[value="DEFAULT"]',
                    message:
                        "Don't hardcode 'DEFAULT' as the env id (Story G1). " + 'Read it from the host via `useEnvironment()` instead.',
                },
            ],
        },
    },
    /*
     * The fallback inside `resolveEnvironmentId` is the ONE place the literal is allowed
     * outside tests; named export `FALLBACK_ENVIRONMENT_ID` exists so callers reference
     * the symbol rather than re-typing the string.
     */
    {
        files: ['src/main/ui/app/lib/env/resolveEnvironmentId.ts'],
        rules: {
            'no-restricted-syntax': 'off',
        },
    },
];
