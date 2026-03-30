module.exports = {
    settings: {
        'import/resolver': {
            typescript: {
                project: [__dirname + '/tsconfig.json'],
                alwaysTryTypes: true,
            },
        },
        react: {
            version: 'detect',
        },
    },
    plugins: ['unused-imports'],
    ignorePatterns: ['dist', 'coverage'],
    overrides: [
        {
            files: ['*.ts', '*.tsx'],
            extends: [
                'eslint:recommended',
                'plugin:@typescript-eslint/recommended',
                'plugin:react/recommended',
                'plugin:react-hooks/recommended',
                'prettier',
                'plugin:import/recommended',
                'plugin:import/typescript',
            ],
            rules: {
                'react/react-in-jsx-scope': 'off',
                '@typescript-eslint/no-unused-vars': 'off',
                'unused-imports/no-unused-imports': 'error',
                'unused-imports/no-unused-vars': [
                    'error',
                    {
                        vars: 'all',
                        argsIgnorePattern: '^_',
                        varsIgnorePattern: '^_',
                        caughtErrorsIgnorePattern: '^_',
                        args: 'after-used',
                    },
                ],
                'import/no-unresolved': 'error',
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
        },
    ],
};
