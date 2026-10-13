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
import type { JsonSchema } from '@gravitee/graphene-core';

import { fetcherLabel, stripLegacyAutoFetchFromSchema } from './fetcherSchema';

describe('stripLegacyAutoFetchFromSchema', () => {
    it('leaves only the plugin settings of the GitHub fetcher schema', () => {
        const stripped = stripLegacyAutoFetchFromSchema({
            type: 'object',
            title: 'github',
            properties: {
                githubUrl: { title: 'GitHub URL', type: 'string', default: 'https://api.github.com' },
                owner: { title: 'Repository Owner', type: 'string' },
                repository: { title: 'Repository', type: 'string' },
                personalAccessToken: { title: 'Personal Access Token', type: 'string' },
                autoFetch: { title: 'Auto Fetch', type: 'boolean', default: false },
                fetchCron: { title: 'Update frequency', type: 'string' },
            },
            required: ['githubUrl', 'owner', 'repository'],
            if: { properties: { autoFetch: { const: true } } },
            then: { required: ['fetchCron'] },
        });

        expect(stripped).toEqual({
            type: 'object',
            title: 'github',
            properties: {
                githubUrl: { title: 'GitHub URL', type: 'string', default: 'https://api.github.com' },
                owner: { title: 'Repository Owner', type: 'string' },
                repository: { title: 'Repository', type: 'string' },
                personalAccessToken: { title: 'Personal Access Token', type: 'string' },
            },
            required: ['githubUrl', 'owner', 'repository'],
        });
    });

    it('removes autoFetch and fetchCron from the properties and the required fields', () => {
        const stripped = stripLegacyAutoFetchFromSchema({
            type: 'object',
            properties: {
                repository: { type: 'string' },
                autoFetch: { type: 'boolean' },
                fetchCron: { type: 'string' },
            },
            required: ['repository', 'autoFetch', 'fetchCron'],
        });

        expect(stripped).toEqual({
            type: 'object',
            properties: { repository: { type: 'string' } },
            required: ['repository'],
        });
    });

    it('drops the required list when only the legacy fields were required', () => {
        const stripped = stripLegacyAutoFetchFromSchema({
            type: 'object',
            properties: { autoFetch: { type: 'boolean' }, url: { type: 'string' } },
            required: ['autoFetch'],
        });

        expect(stripped).not.toHaveProperty('required');
    });

    it('drops an if / then / else that depends on autoFetch', () => {
        const stripped = stripLegacyAutoFetchFromSchema({
            type: 'object',
            properties: { url: { type: 'string' }, autoFetch: { type: 'boolean' }, fetchCron: { type: 'string' } },
            if: { properties: { autoFetch: { const: true } } },
            then: { required: ['fetchCron'] },
            else: {},
        });

        expect(stripped).toEqual({ type: 'object', properties: { url: { type: 'string' } } });
    });

    it('keeps an if / then / else that does not depend on autoFetch', () => {
        const condition = {
            if: { properties: { useAuth: { const: true } } },
            then: { required: ['token'] },
            else: { required: [] },
        };

        const stripped = stripLegacyAutoFetchFromSchema({
            type: 'object',
            properties: { useAuth: { type: 'boolean' }, token: { type: 'string' } },
            ...condition,
        });

        expect(stripped).toMatchObject(condition);
    });

    it('keeps a schema without legacy fields as it is', () => {
        const schema: JsonSchema = { type: 'object', properties: { url: { type: 'string' } }, required: ['url'] };

        expect(stripLegacyAutoFetchFromSchema(schema)).toEqual(schema);
    });
});

describe('fetcherLabel', () => {
    it.each([
        ['github-fetcher', 'GitHub'],
        ['gitlab-fetcher', 'GitLab'],
        ['git-fetcher', 'Git'],
        ['bitbucket-fetcher', 'Bitbucket'],
        ['http-fetcher', 'HTTP'],
    ])('labels the known fetcher %s as %s', (id, label) => {
        expect(fetcherLabel(id)).toBe(label);
    });

    it('falls back to the id without its "-fetcher" suffix for an unknown fetcher', () => {
        expect(fetcherLabel('custom-fetcher')).toBe('custom');
    });

    it('keeps an unknown id that has no "-fetcher" suffix', () => {
        expect(fetcherLabel('custom')).toBe('custom');
    });
});
