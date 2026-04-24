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
import { useParams } from 'react-router-dom';

import type { Environment } from './environment.types';

export function useEnvHrid(): string {
    const { envHrid = '' } = useParams();
    return envHrid;
}

/**
 * First human-readable id for URLs, or technical id when no hrids exist.
 */
export function getPrimaryHrid(environment: Environment): string {
    const h = environment.hrids?.[0];
    return h ?? environment.id;
}

/**
 * Resolves an environment from the URL segment (may be id or hrid).
 * Tries id first, then hrids -- both case-insensitively.
 */
export function resolveEnvironmentFromSegment(environments: readonly Environment[], segment: string): Environment | null {
    if (!segment) return null;
    const lower = segment.toLowerCase();
    return (
        environments.find(e => e.id.toLowerCase() === lower) ??
        environments.find(e => e.hrids?.some(h => h.toLowerCase() === lower)) ??
        null
    );
}

/**
 * True when the segment in the URL is the environment's technical id (not a primary hrid), and the env has hrids
 * to canonicalize to.
 */
export function shouldRewriteIdToHrid(environment: Environment, segment: string): boolean {
    if (!segment || !environment.hrids?.length) return false;
    return environment.id.toLowerCase() === segment.toLowerCase();
}
