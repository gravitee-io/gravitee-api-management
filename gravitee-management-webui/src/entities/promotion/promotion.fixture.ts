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
import { Promotion } from './promotion';

export function fakePromotion(attributes?: Partial<Promotion>): Promotion {
  const defaultValue: Promotion = {
    id: 'promotion#1',
    apiDefinition: '',
    status: 'ACCEPTED',
    apiId: 'api#1',

    targetEnvCockpitId: 'env#2',
    targetEnvName: 'inst#2',
    sourceEnvCockpitId: 'env#1',
    sourceEnvName: 'inst#1',

    author: {
      userId: 'user#1',
      displayName: 'Gaetan Maisse',
      email: 'gm@gv.io',
      picture: '',
      source: 'github',
      sourceId: 'gm',
    },

    createdAt: new Date(),
    updatedAt: new Date(),
  };
  return {
    ...defaultValue,
    ...attributes,
  };
}
