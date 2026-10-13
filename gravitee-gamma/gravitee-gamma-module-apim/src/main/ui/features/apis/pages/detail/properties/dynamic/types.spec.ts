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
import { fromDtoToFormState, fromFormStateToDto } from './types';
import type { DynamicPropertyConfig } from '../../../../types';

it('keeps a stored maxConcurrentConnections through a load and save', () => {
    const dto: DynamicPropertyConfig = {
        enabled: true,
        provider: 'HTTP',
        schedule: '*/30 * * * * *',
        configuration: {
            method: 'GET',
            url: 'https://api.example.com/properties',
            httpClientOptions: { maxConcurrentConnections: 10 },
        },
    };

    expect(fromFormStateToDto(fromDtoToFormState(dto)).configuration?.httpClientOptions?.maxConcurrentConnections).toBe(10);
});
