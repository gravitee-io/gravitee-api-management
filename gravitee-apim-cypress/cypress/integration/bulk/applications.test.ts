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

import { ADMIN_USER } from 'fixtures/fakers/users/users';
import { gio } from 'commands/gravitee.commands';
import { ApplicationFakers } from '../../fixtures/fakers/applications';

const bulkSize = 50;

function create() {
  gio
    .management(ADMIN_USER)
    .applications()
    .create(ApplicationFakers.application())
    .created()
    .should((createResponse) => {
      const appId = createResponse.body.id;
      expect(appId).not.undefined;
    });
}

describe('Bulk Applications', () => {
  it(`should create ${bulkSize} applications`, () => {
    // Useful to run in parallel
    for (let i = 0; i < bulkSize; i++) {
      create();
    }
  });
});
