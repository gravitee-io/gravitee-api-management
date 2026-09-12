import js from '@eslint/js';
import grapheneConfig from '@gravitee/graphene-core/eslint';
import reactPlugin from 'eslint-plugin-react';

/**
 * Shared ESLint config for Gamma UI packages. Pass each module's directory so the
 * TypeScript import resolver points at that package's tsconfig.
 */
export default projectDir => [
    // graphene's flat config carries neither of these, where the eslintrc one it replaces extended
    // both. Kept explicitly so the rule set does not quietly shrink.
    { files: ['**/*.{ts,tsx}'], ...js.configs.recommended },
    {
        files: ['**/*.{ts,tsx}'],
        ...reactPlugin.configs.flat.recommended,
        rules: {
            ...reactPlugin.configs.flat.recommended.rules,
            // Both were already off in the eslintrc config: the JSX runtime makes the import
            // unnecessary, and prop types are the TypeScript types here.
            'react/prop-types': 'off',
            'react/react-in-jsx-scope': 'off',
        },
    },
    ...grapheneConfig,
    {
        files: ['**/*.{ts,tsx}'],
        settings: {
            'import-x/resolver': {
                typescript: {
                    project: [projectDir + '/tsconfig.json'],
                    alwaysTryTypes: true,
                },
            },
        },
        rules: {
            // Gamma-specific import order: blank lines between groups + case-insensitive alphabetize.
            // Graphene's own config uses 'never' / case-sensitive — kept separate to avoid churning gamma's imports.
            'import-x/order': [
                'error',
                {
                    groups: ['builtin', 'external', 'internal', ['sibling', 'parent'], 'index', 'unknown'],
                    'newlines-between': 'always',
                    alphabetize: {
                        order: 'asc',
                        caseInsensitive: true,
                    },
                },
            ],
        },
    },
];
