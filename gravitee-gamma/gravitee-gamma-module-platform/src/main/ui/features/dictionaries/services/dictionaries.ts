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

import { apimFetchJsonV1Env } from '../../../shared/api/apimClient';
import type {
    Dictionary,
    DictionaryLifecycleAction,
    DictionaryListItem,
    NewDictionaryPayload,
    UpdateDictionaryPayload,
} from '../types/dictionary';

export async function listEnvironmentDictionaries(environmentId: string): Promise<DictionaryListItem[]> {
    return apimFetchJsonV1Env<DictionaryListItem[]>(environmentId, '/configuration/dictionaries');
}

export async function getEnvironmentDictionary(environmentId: string, dictionaryId: string): Promise<Dictionary> {
    return apimFetchJsonV1Env<Dictionary>(environmentId, `/configuration/dictionaries/${encodeURIComponent(dictionaryId)}`);
}

export async function createEnvironmentDictionary(environmentId: string, data: NewDictionaryPayload): Promise<Dictionary> {
    return apimFetchJsonV1Env<Dictionary>(environmentId, '/configuration/dictionaries', {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

export async function updateEnvironmentDictionary(
    environmentId: string,
    dictionaryId: string,
    data: UpdateDictionaryPayload,
): Promise<Dictionary> {
    return apimFetchJsonV1Env<Dictionary>(environmentId, `/configuration/dictionaries/${encodeURIComponent(dictionaryId)}`, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
}

export async function deleteEnvironmentDictionary(environmentId: string, dictionaryId: string): Promise<void> {
    return apimFetchJsonV1Env<void>(environmentId, `/configuration/dictionaries/${encodeURIComponent(dictionaryId)}`, {
        method: 'DELETE',
    });
}

export async function deployEnvironmentDictionary(environmentId: string, dictionaryId: string): Promise<Dictionary> {
    return apimFetchJsonV1Env<Dictionary>(environmentId, `/configuration/dictionaries/${encodeURIComponent(dictionaryId)}/_deploy`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

export async function undeployEnvironmentDictionary(environmentId: string, dictionaryId: string): Promise<Dictionary> {
    return apimFetchJsonV1Env<Dictionary>(environmentId, `/configuration/dictionaries/${encodeURIComponent(dictionaryId)}/_undeploy`, {
        method: 'POST',
        body: JSON.stringify({}),
    });
}

export async function startEnvironmentDictionary(environmentId: string, dictionaryId: string): Promise<Dictionary> {
    return changeDictionaryLifecycle(environmentId, dictionaryId, 'START');
}

export async function stopEnvironmentDictionary(environmentId: string, dictionaryId: string): Promise<Dictionary> {
    return changeDictionaryLifecycle(environmentId, dictionaryId, 'STOP');
}

async function changeDictionaryLifecycle(
    environmentId: string,
    dictionaryId: string,
    action: DictionaryLifecycleAction,
): Promise<Dictionary> {
    return apimFetchJsonV1Env<Dictionary>(
        environmentId,
        `/configuration/dictionaries/${encodeURIComponent(dictionaryId)}?action=${action}`,
        {
            method: 'POST',
            body: JSON.stringify({}),
        },
    );
}
