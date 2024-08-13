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

import { IntegrationPreview, IntegrationPreviewApisState } from '../../management/integrations/integrations.model';

export function fakeDiscoveryPreview(attribute?: Partial<IntegrationPreview>): IntegrationPreview {
  const base: IntegrationPreview = {
    totalCount: 3,
    newCount: 3,
    updateCount: 0,
    apis: [
      {
        id: 'testit',
        name: 'test name',
        state: IntegrationPreviewApisState.NEW,
      },
      {
        id: 'testit2',
        name: 'test name2',
        state: IntegrationPreviewApisState.NEW,
      },
      {
        id: 'testit3',
        name: 'test name3',
        state: IntegrationPreviewApisState.NEW,
      },
    ],
  };

  return {
    ...base,
    ...attribute,
  };
}
