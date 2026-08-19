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

/** Classic `CorsUtil.defaultHttpHeaders`, used as CORS header autocomplete options. */
export const CORS_DEFAULT_HTTP_HEADERS = [
    '*',
    'Accept',
    'Accept-Charset',
    'Accept-Encoding',
    'Accept-Language',
    'Accept-Ranges',
    'Access-Control-Allow-Credentials',
    'Access-Control-Allow-Headers',
    'Access-Control-Allow-Methods',
    'Access-Control-Allow-Origin',
    'Access-Control-Expose-Headers',
    'Access-Control-Max-Age',
    'Access-Control-Request-Headers',
    'Access-Control-Request-Method',
    'Age',
    'Allow',
    'Authorization',
    'Cache-Control',
    'Connection',
    'Content-Disposition',
    'Content-Encoding',
    'Content-ID',
    'Content-Language',
    'Content-Length',
    'Content-Location',
    'Content-MD5',
    'Content-Range',
    'Content-Type',
    'Cookie',
    'Date',
    'ETag',
    'Expires',
    'Expect',
    'Forwarded',
    'From',
    'Host',
    'If-Match',
    'If-Modified-Since',
    'If-None-Match',
    'If-Unmodified-Since',
    'Keep-Alive',
    'Last-Modified',
    'Location',
    'Link',
    'Max-Forwards',
    'MIME-Version',
    'Origin',
    'Pragma',
    'Proxy-Authenticate',
    'Proxy-Authorization',
    'Proxy-Connection',
    'Range',
    'Referer',
    'Retry-After',
    'Server',
    'Set-Cookie',
    'Set-Cookie2',
    'TE',
    'Trailer',
    'Transfer-Encoding',
    'Upgrade',
    'User-Agent',
    'Vary',
    'Via',
    'Warning',
    'WWW-Authenticate',
    'X-Forwarded-For',
    'X-Forwarded-Proto',
    'X-Forwarded-Server',
    'X-Forwarded-Host',
] as const;

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
