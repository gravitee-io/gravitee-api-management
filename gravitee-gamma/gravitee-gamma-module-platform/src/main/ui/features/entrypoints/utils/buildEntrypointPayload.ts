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
import type { EntrypointTarget, NewEntrypointPayload, OrgEnvironment, OrgTag, UpdateEntrypointPayload } from '../types/entrypoint';

export interface SelectOption {
    id: string;
    name: string;
}

export function toTagOptions(tags: OrgTag[]): SelectOption[] {
    return tags.map(tag => ({ id: tag.key, name: tag.name || tag.key }));
}

export function toEnvironmentOptions(environments: OrgEnvironment[]): SelectOption[] {
    return environments.map(env => ({ id: env.id, name: env.name || env.id }));
}

export interface SheetSelectOptions {
    tagOptions: SelectOption[];
    environmentOptions: SelectOption[];
}

export function buildSheetSelectOptions(tags: OrgTag[], environments: OrgEnvironment[]): SheetSelectOptions {
    return { tagOptions: toTagOptions(tags), environmentOptions: toEnvironmentOptions(environments) };
}

export function submitEntrypointForm(
    canSubmit: boolean,
    onSubmit: (data: NewEntrypointPayload | UpdateEntrypointPayload) => void,
    existingId: string | undefined,
    target: EntrypointTarget,
    value: string,
    tags: string[],
    environmentIds: string[],
): void {
    if (!canSubmit) return;
    onSubmit(resolveEntrypointPayload(existingId, target, value, tags, environmentIds));
}

export function buildNewEntrypointPayload(
    target: EntrypointTarget,
    value: string,
    tags: string[],
    environmentIds: string[],
): NewEntrypointPayload {
    return { target, value, tags, environmentIds };
}

export function buildUpdateEntrypointPayload(
    id: string,
    target: EntrypointTarget,
    value: string,
    tags: string[],
    environmentIds: string[],
): UpdateEntrypointPayload {
    return { id, target, value, tags, environmentIds };
}

export function resolveEntrypointPayload(
    existingId: string | undefined,
    target: EntrypointTarget,
    value: string,
    tags: string[],
    environmentIds: string[],
): NewEntrypointPayload | UpdateEntrypointPayload {
    if (existingId) {
        return buildUpdateEntrypointPayload(existingId, target, value, tags, environmentIds);
    }
    return buildNewEntrypointPayload(target, value, tags, environmentIds);
}
