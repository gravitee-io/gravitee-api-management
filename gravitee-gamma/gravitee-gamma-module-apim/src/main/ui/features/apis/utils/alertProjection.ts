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
import type { AlertPropertyProjection } from '../types';

export function propertyProjection(property: string): AlertPropertyProjection {
    return { type: 'PROPERTY', property };
}

export function projectionPropertyKey(projections: unknown[] | undefined): string | undefined {
    const first = projections?.[0];
    if (!first || typeof first !== 'object' || Array.isArray(first)) {
        return undefined;
    }
    const property = (first as { property?: unknown }).property;
    return typeof property === 'string' && property.length > 0 ? property : undefined;
}
