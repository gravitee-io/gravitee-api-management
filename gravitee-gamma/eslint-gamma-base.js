const grapheneConfig = require('@gravitee/graphene-core/eslint-legacy');

/**
 * Shared ESLint config for Gamma UI packages. Pass each module's `__dirname` so the
 * TypeScript import resolver points at that package's tsconfig.
 */
module.exports = projectDir => ({
    ...grapheneConfig,
    settings: {
        ...grapheneConfig.settings,
        'import/resolver': {
            typescript: {
                project: [projectDir + '/tsconfig.json'],
                alwaysTryTypes: true,
            },
        },
    },
    rules: {
        ...grapheneConfig.rules,
        // Gamma-specific import/order: blank lines between groups + case-insensitive alphabetize.
        // Core legacy uses 'never' / case-sensitive — kept separate to avoid churning gamma's imports.
        'import/order': [
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
});
