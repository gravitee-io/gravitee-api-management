import base from '../eslint-gamma-base.mjs';

export default [
    ...base(import.meta.dirname),
    {
        files: ['**/*.{ts,tsx}'],
        rules: {
            // The gmd element bundle is built by `nx build-element markdown`, so it does not exist on a fresh
            // checkout, which is where lint runs. TypeScript still validates the import through global.d.ts.
            'import-x/no-unresolved': ['error', { ignore: ['^@gravitee/gravitee-markdown-element$'] }],
        },
    },
];
