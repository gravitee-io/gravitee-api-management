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
const typescriptEslint = require('@typescript-eslint/eslint-plugin');
const typescriptParser = require('@typescript-eslint/parser');
const prettier = require('eslint-config-prettier');
const angularPlugin = require('eslint-plugin-angular');
const importPlugin = require('eslint-plugin-import');
const rxjs = require('eslint-plugin-rxjs-x');
const unusedImports = require('eslint-plugin-unused-imports');
const globals = require('globals');

// eslint-config-angular, plugin:import/typescript and plugin:storybook/recommended are only
// published in the eslintrc shape, so they go through the bridge. Storybook keeps its own `files`:
// its rules are meant for stories, not for every source file.
const compat = new FlatCompat({ baseDirectory: __dirname });
const angularConfig = compat.extends('angular');
const importTypescript = compat.extends('plugin:import/typescript');
const storybook = compat.extends('plugin:storybook/recommended');

const CONSOLE_ALLOWED = [
  'warn',
  'dir',
  'timeLog',
  'assert',
  'clear',
  'count',
  'countReset',
  'group',
  'groupEnd',
  'table',
  'dirxml',
  'error',
  'groupCollapsed',
  'Console',
  'profile',
  'profileEnd',
  'timeStamp',
  'context',
];

module.exports = [
  {
    // Anchored with **/ : a flat config's global ignores resolve against the directory ESLint runs
    // from, not against this file, where eslintrc's ignorePatterns did.
    ignores: ['**/dist/**', '**/coverage/**', '**/storybook-static/**', '**/*.js', '**/*.json', '**/*.html'],
  },
  ...storybook,
  ...angularConfig.map(config => ({ ...config, files: ['**/*.ts'] })),
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        project: ['./tsconfig.json', './.storybook/tsconfig.json'],
        tsconfigRootDir: __dirname,
        sourceType: 'module',
      },
      globals: { ...globals.browser, ...globals.node },
    },
    settings: {
      'import/resolver': {
        typescript: { project: [`${__dirname}/tsconfig.json`] },
      },
    },
    plugins: {
      '@typescript-eslint': typescriptEslint,
      angular: angularPlugin,
      import: importPlugin,
      'rxjs-x': rxjs,
      'unused-imports': unusedImports,
    },
    rules: {
      ...js.configs.recommended.rules,
      ...typescriptEslint.configs['flat/eslint-recommended'].rules,
      ...typescriptEslint.configs.recommended.rules,
      ...rxjs.configs.recommended.rules,
      ...importPlugin.configs.recommended.rules,
      ...prettier.rules,
      'angular/no-private-call': 'error',
      'angular/controller-as-route': 'error',
      'angular/controller-name': 'error',
      'angular/module-setter': 'error',
      'angular/log': 'error',
      'angular/on-watch': 'error',
      'angular/no-service-method': 'off',
      'angular/module-getter': 'off',
      'angular/definedundefined': 'off',
      'angular/document-service': 'off',
      'angular/json-functions': 'off',
      'angular/typecheck-array': 'off',
      'angular/typecheck-string': 'off',
      'angular/typecheck-function': 'off',
      'angular/window-service': 'off',
      'angular/interval-service': 'off',
      'angular/timeout-service': 'off',
      'no-bitwise': 'error',
      'no-redeclare': 'warn',
      'no-useless-escape': 'error',
      'no-prototype-builtins': 'warn',
      'no-cond-assign': 'warn',
      // ban-types was split up in typescript-eslint 8; these are the parts it became.
      // Naming a generic specialisation is what the interfaces flagged here are for.
      '@typescript-eslint/no-empty-object-type': ['error', { allowInterfaces: 'with-single-extends' }],
      '@typescript-eslint/no-unsafe-function-type': 'error',
      '@typescript-eslint/no-wrapper-object-types': 'error',
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
      '@typescript-eslint/ban-ts-comment': 'warn',
      // New in the typescript-eslint 8 recommended set. It flags 25 statement-position
      // ternaries here, several of them multi-line, which is a rewrite of its own.
      '@typescript-eslint/no-unused-expressions': 'off',
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      eqeqeq: ['error', 'smart'],
      'guard-for-in': 'warn',
      'id-blacklist': 'off',
      'id-match': 'off',
      'no-caller': 'error',
      'no-console': ['warn', { allow: CONSOLE_ALLOWED }],
      'no-debugger': 'error',
      'no-empty': 'off',
      'no-eval': 'error',
      'no-fallthrough': 'error',
      'no-new-wrappers': 'error',
      'no-underscore-dangle': 'off',
      'no-unused-labels': 'error',
      radix: 'error',
      'spaced-comment': ['warn', 'always', { markers: ['/'] }],
      'import/order': [
        'error',
        {
          groups: ['internal', 'external', 'builtin', 'object', 'type', 'index', 'sibling', 'parent'],
          'newlines-between': 'always',
        },
      ],
      'rxjs-x/no-sharereplay': 'off',
      'rxjs-x/no-subject-unsubscribe': 'off',
      'rxjs-x/no-implicit-any-catch': 'off',
      // rxjs-x recommends five rules eslint-plugin-rxjs did not carry. They are left off so the
      // bump keeps the same rule set; prefer-root-operators is auto-fixable if it is wanted.
      'rxjs-x/no-subscribe-in-pipe': 'off',
      'rxjs-x/no-topromise': 'off',
      'rxjs-x/prefer-observer': 'off',
      'rxjs-x/prefer-root-operators': 'off',
      'rxjs-x/throw-error': 'off',
    },
  },
  // After the block above, as in the config it replaces: plugin:import/typescript turns off the
  // rules TypeScript already enforces, import/named among them.
  ...importTypescript.map(config => ({ ...config, files: ['**/*.ts'] })),
];
