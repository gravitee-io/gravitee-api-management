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

import { IsA2aIntegration } from './is-a2a-integration.pipe';

describe('IsA2aIntegration', (): void => {
  it('is A2A integration', (): void => {
    const pipe: IsA2aIntegration = new IsA2aIntegration();
    const input = {
      id: '188c203c-f4c4-441c-8a15-66b299930268',
      name: 'My Integration',
      provider: 'A2A',
      description: 'string',
      groups: [],
    };

    expect(pipe.transform(input)).toBeTruthy();
  });

  it('is not A2A integration', (): void => {
    const pipe: IsA2aIntegration = new IsA2aIntegration();
    const input = {
      id: '188c203c-f4c4-441c-8a15-66b299930268',
      name: 'My Integration',
      provider: 'aws-api-gateway',
      description: 'string',
      groups: [],
    };

    expect(pipe.transform(input)).toBeFalsy();
  });
});
