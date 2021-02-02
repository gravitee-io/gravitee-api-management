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
module.exports = {
  preset: 'ts-jest',
  testMatch: ['<rootDir>/src/**/?(*.)+(spec|test).ts'],
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
  verbose: true,
  moduleFileExtensions: [
    'js',
    'ts',
    'html'
  ],
  moduleNameMapper: {
    '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': '<rootDir>/__mocks__/fileMock.js',
    '\\.(css|scss)$': '<rootDir>/__mocks__/styleMock.js'
  },
  transform: {
    '^.+\\.ts$': 'ts-jest',
    '\\.js$': 'babel-jest',
    '^.+\\.html$': '<rootDir>/__mocks__/rawLoader.js'
  },
  transformIgnorePatterns: [
    // Ensure these packages are transformed with babel as they contains some ES6+ features (as `await import` and spread operator)
    '/node_modules/(?!(@gravitee/ui-components|lit-element|lit-html|resize-observer-polyfill)).+\\.js$'
  ]
};
