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
import { PORTAL_SUBSCRIPTION_FORMS_STORE_NAME, runTransaction } from '../../portals/storage/db';
import { normalizeFormField, type FormField, type MappedApi, type SubscriptionForm } from '../types';

function createFormId(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID();
    }
    return `form-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeSubscriptionForm(form: SubscriptionForm): SubscriptionForm {
    return {
        ...form,
        description: form.description ?? '',
        mappedApis: form.mappedApis ?? [],
        fields: (form.fields ?? []).map(normalizeFormField),
    };
}

export async function getSubscriptionFormsByPortalId(portalId: string): Promise<SubscriptionForm[]> {
    const forms = await runTransaction<SubscriptionForm[]>(
        PORTAL_SUBSCRIPTION_FORMS_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('portalId');
            return index.getAll(portalId);
        },
    );

    return forms.map(normalizeSubscriptionForm).sort((a, b) => a.name.localeCompare(b.name));
}

export async function getSubscriptionForm(id: string): Promise<SubscriptionForm | undefined> {
    const form = await runTransaction<SubscriptionForm | undefined>(
        PORTAL_SUBSCRIPTION_FORMS_STORE_NAME,
        'readonly',
        store => store.get(id),
    );
    return form ? normalizeSubscriptionForm(form) : undefined;
}

export async function saveSubscriptionForm(form: SubscriptionForm): Promise<void> {
    await runTransaction(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME, 'readwrite', store =>
        store.put(normalizeSubscriptionForm(form)),
    );
}

export async function createSubscriptionForm(
    portalId: string,
    input: { name: string; description?: string },
): Promise<SubscriptionForm> {
    const form: SubscriptionForm = {
        id: createFormId(),
        portalId,
        name: input.name.trim(),
        description: (input.description ?? '').trim(),
        createdAt: Date.now(),
        mappedApis: [],
        fields: [],
    };
    await saveSubscriptionForm(form);
    return form;
}

export async function updateSubscriptionForm(
    formId: string,
    patch: Partial<Pick<SubscriptionForm, 'name' | 'description' | 'mappedApis' | 'fields'>>,
): Promise<SubscriptionForm | undefined> {
    const existing = await getSubscriptionForm(formId);
    if (!existing) {
        return undefined;
    }

    const updated = normalizeSubscriptionForm({
        ...existing,
        ...patch,
        mappedApis: patch.mappedApis !== undefined ? [...patch.mappedApis] : existing.mappedApis,
        fields: patch.fields !== undefined ? (patch.fields as FormField[]) : existing.fields,
    });
    await saveSubscriptionForm(updated);
    return updated;
}

export async function setSubscriptionFormMappedApis(
    formId: string,
    mappedApis: readonly MappedApi[],
): Promise<SubscriptionForm | undefined> {
    return updateSubscriptionForm(formId, { mappedApis });
}

export async function deleteSubscriptionForm(id: string): Promise<void> {
    await runTransaction(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME, 'readwrite', store => store.delete(id));
}

export async function deleteSubscriptionFormsForPortal(portalId: string): Promise<void> {
    const forms = await getSubscriptionFormsByPortalId(portalId);
    await Promise.all(forms.map(form => deleteSubscriptionForm(form.id)));
}
