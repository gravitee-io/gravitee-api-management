/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

import { Integration } from '../../management/integrations/integrations.model';

export function fakeIntegration(attribute?: Partial<Integration>) {
  const base: Integration = {
    id: 'test_id',
    name: 'test_name',
    description: 'test_description',
    provider: 'test_provider',
    owner: 'test_owner',
    status: 'test_status',
    agent: 'test_agent',
  };

  return {
    ...base,
    ...attribute,
  };
}
