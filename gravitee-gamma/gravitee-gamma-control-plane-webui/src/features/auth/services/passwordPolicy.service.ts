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
import { managementApi } from '../../../shared/api/api-client';
import type { PasswordPolicy } from '../../../shared/password-policy';

export async function getPasswordPolicy(): Promise<PasswordPolicy> {
    const policy = await managementApi.get<PasswordPolicy>('/configuration/password-policy');
    return {
        ...policy,
        rules: policy.rules ?? [],
    };
}
