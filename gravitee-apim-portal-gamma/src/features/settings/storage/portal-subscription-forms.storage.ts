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

export async function deleteSubscriptionFormsForPortal(portalId: string): Promise<void> {
    const forms = await runTransaction<{ id: string; portalId: string }[]>(
        PORTAL_SUBSCRIPTION_FORMS_STORE_NAME,
        'readonly',
        store => {
            const index = store.index('portalId');
            return index.getAll(portalId);
        },
    );

    await Promise.all(
        forms.map(form =>
            runTransaction(PORTAL_SUBSCRIPTION_FORMS_STORE_NAME, 'readwrite', store => store.delete(form.id)),
        ),
    );
}
