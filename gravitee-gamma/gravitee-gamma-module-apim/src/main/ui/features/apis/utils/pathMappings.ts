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

/** Same rule as classic console V4 and the Management API form check. */
export const PATH_MAPPING_PATTERN = /^\/[a-zA-Z0-9:/._-]+$/;

export class PathMappingsConflictError extends Error {
    constructor(
        readonly reason: 'missing-mapping' | 'no-http-listener',
        message: string,
    ) {
        super(message);
        this.name = 'PathMappingsConflictError';
    }
}

export function storedPathMappings(pathMappings: string[] | undefined | null): string[] {
    return pathMappings ? [...pathMappings] : [];
}

export function sortedPathMappings(pathMappings: string[] | undefined | null): string[] {
    return storedPathMappings(pathMappings).sort((a, b) => a.localeCompare(b));
}

export function samePathMappings(left: string[], right: string[]): boolean {
    return left.length === right.length && left.every((value, index) => value === right[index]);
}

export function replacePathMapping(mappings: string[], current: string, updated: string): string[] {
    return mappings.map(mapping => (mapping === current ? updated : mapping));
}

/**
 * Returns an error message when the value cannot be stored.
 * The form rule applies to every write, including an unchanged stored value.
 */
export function pathMappingValidationError(value: string, existing: string[], current?: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) {
        return 'Path mapping is required.';
    }
    if (!PATH_MAPPING_PATTERN.test(trimmed)) {
        return 'Path mapping must match /[a-zA-Z0-9:/._-]+';
    }
    const others = current === undefined ? existing : existing.filter(mapping => mapping !== current);
    if (others.includes(trimmed)) {
        return 'Path mapping already exists.';
    }
    return null;
}
