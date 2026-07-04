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
export default {
    displayName: 'gravitee-gamma-portal-webui',
    testEnvironment: 'jest-fixed-jsdom',
    transformIgnorePatterns: ['/node_modules/(?!(@gravitee/graphene-core)/)'],
    moduleNameMapper: {
        '^react$': '<rootDir>/../../node_modules/react/index.js',
        '^react/jsx-runtime$': '<rootDir>/../../node_modules/react/jsx-runtime.js',
        '^react/jsx-dev-runtime$': '<rootDir>/../../node_modules/react/jsx-dev-runtime.js',
        '^react-dom$': '<rootDir>/../../node_modules/react-dom/index.js',
        '^react-dom/client$': '<rootDir>/../../node_modules/react-dom/client.js',
        '^@gravitee/graphene-core$': '<rootDir>/../../node_modules/@gravitee/graphene-core/dist/index.js',
        '^@gravitee/graphene-core/(.*)$': '<rootDir>/../../node_modules/@gravitee/graphene-core/dist/$1',
    },
    transform: {
        '^(?!.*\\.(js|jsx|ts|tsx|css|json)$)': '@nx/react/plugins/jest',
        '^.+\\.[tj]sx?$': ['babel-jest', { presets: ['@nx/react/babel'] }],
    },
    moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx'],
    coverageDirectory: '<rootDir>/coverage',
};
