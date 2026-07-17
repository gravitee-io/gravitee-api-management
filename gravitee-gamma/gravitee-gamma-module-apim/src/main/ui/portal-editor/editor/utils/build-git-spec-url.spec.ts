/*
 * Copyright (C) 2026 The Gravitee team (http://gravitee.io)
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
import { buildGitSpecUrl } from './build-git-spec-url';

describe('buildGitSpecUrl', () => {
    it('should build a GitHub raw content URL from a repository URL', () => {
        expect(
            buildGitSpecUrl('GITHUB', {
                repositoryUrl: 'https://github.com/gravitee-io/gravitee-api-management',
                branch: 'main',
                filepath: 'openapi/openapi.yaml',
            }),
        ).toBe('https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/main/openapi/openapi.yaml');
    });

    it('should build a GitHub raw content URL from a repository URL with .git suffix', () => {
        expect(
            buildGitSpecUrl('GITHUB', {
                repositoryUrl: 'https://github.com/gravitee-io/gravitee-api-management.git',
                branch: 'main',
                filepath: 'openapi/openapi.yaml',
            }),
        ).toBe('https://raw.githubusercontent.com/gravitee-io/gravitee-api-management/main/openapi/openapi.yaml');
    });

    it('should build a GitLab raw content URL from a repository URL', () => {
        expect(
            buildGitSpecUrl('GITLAB', {
                repositoryUrl: 'https://gitlab.com/gravitee-io/gravitee-api-management',
                branch: 'main',
                filepath: 'openapi/openapi.yaml',
            }),
        ).toBe('https://gitlab.com/gravitee-io/gravitee-api-management/-/raw/main/openapi/openapi.yaml');
    });

    it('should build a GitLab raw content URL for nested groups', () => {
        expect(
            buildGitSpecUrl('GITLAB', {
                repositoryUrl: 'https://gitlab.com/gravitee-io/platform/gravitee-api-management',
                branch: 'main',
                filepath: 'openapi/openapi.yaml',
            }),
        ).toBe('https://gitlab.com/gravitee-io/platform/gravitee-api-management/-/raw/main/openapi/openapi.yaml');
    });

    it('should return an empty string for an invalid repository URL', () => {
        expect(
            buildGitSpecUrl('GITHUB', {
                repositoryUrl: 'not-a-url',
                branch: 'main',
                filepath: 'openapi/openapi.yaml',
            }),
        ).toBe('');
    });
});
