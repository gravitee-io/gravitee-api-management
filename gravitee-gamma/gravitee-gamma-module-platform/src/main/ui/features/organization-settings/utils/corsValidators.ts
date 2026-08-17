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

/** Classic `CorsUtil.httpMethods`. */
export const CORS_HTTP_METHODS = ['*', 'GET', 'DELETE', 'PATCH', 'POST', 'PUT', 'OPTIONS', 'TRACE', 'HEAD'] as const;

export type CorsHttpMethod = (typeof CORS_HTTP_METHODS)[number];

export const DEFAULT_CORS_MAX_AGE = 1728000;

/**
 * Port of Classic `CorsUtil.allowOriginValidator`.
 * `*` and plain origins pass; values that look like regexes must compile.
 */
export function getInvalidAllowOrigins(allowOrigin: readonly string[]): string[] {
    return allowOrigin.filter(origin => {
        if (origin === '*') {
            return false;
        }
        if (origin.includes('(') || origin.includes('[') || origin.includes('*')) {
            try {
                new RegExp(origin);
                return false;
            } catch {
                return true;
            }
        }
        return false;
    });
}
