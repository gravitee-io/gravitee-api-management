/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const { FlatCompat } = require('@eslint/eslintrc');
const js = require('@eslint/js');
const angular = require('angular-eslint');
const typescriptEslint = require('@typescript-eslint/eslint-plugin');
const typescriptParser = require('@typescript-eslint/parser');
const importPlugin = require('eslint-plugin-import');
const unusedImports = require('eslint-plugin-unused-imports');
const prettier = require('eslint-config-prettier');

// plugin:import/typescript and plugin:storybook/recommended are only published in the eslintrc
// shape, and both carry `settings` on top of their rules, so they go through the bridge whole.
// Storybook keeps its own `files`: its rules are meant for stories, not for every source file.
const compat = new FlatCompat({ baseDirectory: __dirname });
const importTypescript = compat.extends('plugin:import/typescript');
const storybook = compat.extends('plugin:storybook/recommended');

const MEMBER_ORDERING = [
    'signature',
    'call-signature',
    'public-static-field',
    'protected-static-field',
    'private-static-field',
    'instance-field',
    'constructor',
    'public-static-method',
    'protected-static-method',
    'private-static-method',
    'public-instance-method',
    'protected-instance-method',
    'private-instance-method',
];

/**
 * Shared ESLint config for the Angular projects: portal-next and the three webui libraries, which
 * carried the same rules in two near-identical eslintrc files. Each project passes its own
 * directory, the tsconfigs its rules are type-aware against, and its selector prefix.
 */
module.exports = ({ projectDir, projects, prefix }) => [
    {
        ignores: ['**/dist/**', '**/coverage/**', '**/storybook-static/**', '**/.storybook/**'],
    },
    ...storybook,
    {
        files: ['**/*.ts'],
        languageOptions: {
            parser: typescriptParser,
            parserOptions: {
                project: projects,
                tsconfigRootDir: projectDir,
            },
        },
        settings: {
            'import/resolver': {
                typescript: {
                    project: projects.map(p => `${projectDir}/${p}`),
                    alwaysTryTypes: true,
                    noWarnOnMultipleProjects: true,
                },
            },
        },
        plugins: {
            '@typescript-eslint': typescriptEslint,
            '@angular-eslint': angular.tsPlugin,
            import: importPlugin,
            'unused-imports': unusedImports,
        },
        rules: {
            ...js.configs.recommended.rules,
            // TypeScript covers these 23 core rules itself, no-undef among them.
            ...typescriptEslint.configs['flat/eslint-recommended'].rules,
            ...typescriptEslint.configs.recommended.rules,
            ...angular.configs.tsRecommended.reduce((rules, config) => ({ ...rules, ...config.rules }), {}),
            ...importPlugin.configs.recommended.rules,
            ...prettier.rules,
            '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix, style: 'camelCase' }],
            '@angular-eslint/component-selector': ['error', { type: 'element', prefix, style: 'kebab-case' }],
            // TODO: enable this rule when all constructor injections are replaced by Angular injection
            '@angular-eslint/prefer-inject': 'off',
            // New in angular-eslint 22. Switching existing components to OnPush is a behaviour
            // change, not a lint fix, so it stays off until that is taken on deliberately.
            '@angular-eslint/prefer-on-push-component-change-detection': 'off',
            // Naming a generic specialisation is what these interfaces are for.
            '@typescript-eslint/no-empty-object-type': ['error', { allowInterfaces: 'with-single-extends' }],
            '@typescript-eslint/member-ordering': ['error', { default: MEMBER_ORDERING }],
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
                    alphabetize: { order: 'asc', caseInsensitive: true },
                },
            ],
        },
    },
    // After the block above, as in the config it replaces: plugin:import/typescript turns off the
    // rules TypeScript already enforces, import/named among them.
    ...importTypescript.map(config => ({ ...config, files: ['**/*.ts'] })),
    {
        files: ['**/*.html'],
        languageOptions: { parser: angular.templateParser },
        plugins: { '@angular-eslint/template': angular.templatePlugin },
        rules: {
            ...angular.configs.templateRecommended.reduce((rules, config) => ({ ...rules, ...config.rules }), {}),
            ...angular.configs.templateAccessibility.reduce((rules, config) => ({ ...rules, ...config.rules }), {}),
            '@angular-eslint/template/prefer-self-closing-tags': 'error',
        },
    },
];
