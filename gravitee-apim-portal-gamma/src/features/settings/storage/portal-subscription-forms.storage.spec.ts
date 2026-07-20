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
import { clearPortalsDatabase } from '../../portals/storage/portals.storage.test-utils';
import {
    createSubscriptionForm,
    deleteSubscriptionFormsForPortal,
    getSubscriptionFormsByPortalId,
    setSubscriptionFormMappedApis,
    updateSubscriptionForm,
} from './portal-subscription-forms.storage';

describe('portal-subscription-forms.storage', () => {
    beforeEach(async () => {
        await clearPortalsDatabase();
    });

    afterEach(async () => {
        await clearPortalsDatabase();
    });

    it('should create and list forms for a portal', async () => {
        const form = await createSubscriptionForm('portal-1', {
            name: 'Enterprise Form',
            description: 'Extra fields for enterprise APIs',
        });

        expect(form).toMatchObject({
            portalId: 'portal-1',
            name: 'Enterprise Form',
            description: 'Extra fields for enterprise APIs',
            mappedApis: [],
            fields: [],
        });

        const listed = await getSubscriptionFormsByPortalId('portal-1');
        expect(listed).toHaveLength(1);
        expect(listed[0]?.id).toBe(form.id);
    });

    it('should update mapped APIs and fields', async () => {
        const form = await createSubscriptionForm('portal-1', { name: 'Default' });

        await setSubscriptionFormMappedApis(form.id, [{ id: 'api-1', name: 'Payments' }]);
        await updateSubscriptionForm(form.id, {
            fields: [
                {
                    id: 'field-1',
                    type: 'text',
                    label: 'Use case',
                    required: true,
                    options: [],
                    validation: '',
                    expression: '',
                },
            ],
        });

        const listed = await getSubscriptionFormsByPortalId('portal-1');
        expect(listed[0]).toMatchObject({
            mappedApis: [{ id: 'api-1', name: 'Payments' }],
            fields: [{ id: 'field-1', type: 'text', label: 'Use case', required: true }],
        });
    });

    it('should delete all forms for a portal', async () => {
        await createSubscriptionForm('portal-1', { name: 'A' });
        await createSubscriptionForm('portal-1', { name: 'B' });
        await createSubscriptionForm('portal-2', { name: 'Other' });

        await deleteSubscriptionFormsForPortal('portal-1');

        expect(await getSubscriptionFormsByPortalId('portal-1')).toEqual([]);
        expect(await getSubscriptionFormsByPortalId('portal-2')).toHaveLength(1);
    });
});
