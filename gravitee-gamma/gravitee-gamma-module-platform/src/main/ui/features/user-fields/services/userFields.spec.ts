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
import { createUserField, deleteUserField, listUserFields, updateUserField } from './userFields';
import { apimFetchJsonOrg } from '../../../shared/api/apimClient';

jest.mock('../../../shared/api/apimClient', () => ({
    apimFetchJsonOrg: jest.fn(),
}));

const mockApimFetchJsonOrg = jest.mocked(apimFetchJsonOrg);

const PAYLOAD = { key: 'department', label: 'Department', required: true, values: ['Engineering', 'Product'] };

describe('userFields service', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockApimFetchJsonOrg.mockResolvedValue(undefined);
    });

    it('lists fields from the organization configuration resource', async () => {
        await listUserFields();
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/custom-user-fields');
    });

    it('creates a field with a POST carrying the serialized payload', async () => {
        await createUserField(PAYLOAD);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/custom-user-fields', {
            method: 'POST',
            body: JSON.stringify(PAYLOAD),
        });
    });

    it('updates a field with a PUT on the keyed path, key repeated in the body', async () => {
        await updateUserField(PAYLOAD);
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/custom-user-fields/department', {
            method: 'PUT',
            body: JSON.stringify(PAYLOAD),
        });
    });

    it('deletes a field with a DELETE on the keyed path', async () => {
        await deleteUserField('department');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/custom-user-fields/department', { method: 'DELETE' });
    });

    it('URL-encodes the key in the path', async () => {
        await deleteUserField('a/b');
        expect(mockApimFetchJsonOrg).toHaveBeenCalledWith('/configuration/custom-user-fields/a%2Fb', { method: 'DELETE' });
    });
});
