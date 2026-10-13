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

const LEGACY_AUTO_FETCH_FIELDS = ['autoFetch', 'fetchCron'];

const FETCHER_LABELS: Record<string, string> = {
    'github-fetcher': 'GitHub',
    'gitlab-fetcher': 'GitLab',
    'git-fetcher': 'Git',
    'bitbucket-fetcher': 'Bitbucket',
    'http-fetcher': 'HTTP',
};

/**
 * Fetcher schemas still declare the classic pages' `autoFetch` and `fetchCron` fields. A portal source holds them as its
 * own fields instead, so they are taken out of the plugin's form, along with a condition that depends on them.
 * Mirrors the classic console's function of the same name.
 */
export function stripLegacyAutoFetchFromSchema(schema: JsonSchema): JsonSchema {
    const {
        if: ifClause,
        then: thenClause,
        else: elseClause,
        properties: originalProperties,
        required: originalRequired,
        ...rest
    } = schema as Record<string, unknown>;
    const properties = Object.fromEntries(
        Object.entries((originalProperties as Record<string, unknown> | undefined) ?? {}).filter(
            ([key]) => !LEGACY_AUTO_FETCH_FIELDS.includes(key),
        ),
    );
    const required = Array.isArray(originalRequired)
        ? (originalRequired as string[]).filter(key => !LEGACY_AUTO_FETCH_FIELDS.includes(key))
        : [];
    const conditionProperties = (ifClause as { properties?: Record<string, unknown> } | undefined)?.properties ?? {};
    const conditionOnAutoFetch = 'autoFetch' in conditionProperties;

    const condition = conditionOnAutoFetch
        ? {}
        : {
              ...(ifClause ? { if: ifClause } : {}),
              ...(thenClause ? { then: thenClause } : {}),
              ...(elseClause ? { else: elseClause } : {}),
          };

    return { ...rest, properties, ...(required.length > 0 ? { required } : {}), ...condition } as JsonSchema;
}

/** A source only stores its fetcher's id, so the screens that show a source name it from the id. */
export function fetcherLabel(fetcherId: string): string {
    if (Object.prototype.hasOwnProperty.call(FETCHER_LABELS, fetcherId)) {
        return FETCHER_LABELS[fetcherId];
    }
    return fetcherId.replace(/-fetcher$/, '');
}
