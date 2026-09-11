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
const js = require('@eslint/js');
const { FlatCompat } = require('@eslint/eslintrc');
const globals = require('globals');
const angular = require('@angular-eslint/eslint-plugin');
const angularTemplate = require('@angular-eslint/eslint-plugin-template');
const templateParser = require('@angular-eslint/template-parser');
const typescriptEslint = require('@typescript-eslint/eslint-plugin');
const typescriptParser = require('@typescript-eslint/parser');
const importPlugin = require('eslint-plugin-import');
const prettier = require('eslint-config-prettier');

// plugin:import/typescript is only published in the eslintrc shape, and it carries `settings` on top
// of its rules, so it goes through the bridge whole rather than having its rules picked out.
const compat = new FlatCompat({ baseDirectory: __dirname });
const importTypescript = compat.extends('plugin:import/typescript');

module.exports = [
  {
    ignores: ['projects/**/*', 'dist/**/*', 'coverage/**/*', '.angular/**/*'],
  },
  ...importTypescript.map(config => ({ ...config, files: ['src/**/*.ts'] })),
  {
    files: ['src/**/*.ts'],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        project: ['tsconfig.json'],
        tsconfigRootDir: __dirname,
      },
      globals: { ...globals.browser, ...globals.node, ...globals.jest },
    },
    plugins: {
      '@typescript-eslint': typescriptEslint,
      '@angular-eslint': angular,
      import: importPlugin,
    },
    rules: {
      ...js.configs.recommended.rules,
      ...typescriptEslint.configs.recommended.rules,
      ...angular.configs.recommended.rules,
      ...prettier.rules,
      '@angular-eslint/component-selector': [
        'error',
        {
          prefix: 'app',
          style: 'kebab-case',
          type: 'element',
        },
      ],
      '@angular-eslint/directive-selector': [
        'error',
        {
          prefix: 'gv',
          style: 'camelCase',
          type: 'attribute',
        },
      ],
      'no-unused-vars': ['off'],
      '@typescript-eslint/no-unused-vars': [
        'error',
        { ignoreRestSiblings: true, argsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' },
      ],
      '@typescript-eslint/no-explicit-any': ['warn', { ignoreRestArgs: true }],
      'import/order': [
        'error',
        {
          groups: ['external', 'builtin', 'internal', 'object', 'type', 'parent', 'index', 'sibling'],
          'newlines-between': 'always',
        },
      ],
    },
    processor: angularTemplate.processors['extract-inline-html'],
  },
  {
    files: ['src/**/*.html'],
    languageOptions: {
      parser: templateParser,
    },
    plugins: {
      '@angular-eslint/template': angularTemplate,
    },
    rules: {
      ...angularTemplate.configs.recommended.rules,
    },
  },
];
