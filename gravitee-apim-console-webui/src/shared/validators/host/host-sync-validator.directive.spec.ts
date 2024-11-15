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
import { FormControl } from '@angular/forms';

import { hostSyncValidator } from './host-sync-validator.directive';

describe('HostSyncValidator', () => {
  it.each`
    key           | message                           | host
    ${'format'}   | ${`Host is not valid`}            | ${'ThisIsALongHostNameWithMoreThan63CharactersWhichIsNotValidInOurCase'}
    ${'max'}      | ${`Max length is 255 characters`} | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
    ${'required'} | ${`Host is required.`}            | ${''}
    ${'required'} | ${`Host is required.`}            | ${null}
  `('should be invalid host: $host because $message', ({ key, message, host }) => {
    expect(hostSyncValidator(new FormControl(host))).toEqual({ [key]: message });
  });

  it('should be valid host', () => {
    expect(hostSyncValidator(new FormControl('host'))).toBeNull();
  });
});
