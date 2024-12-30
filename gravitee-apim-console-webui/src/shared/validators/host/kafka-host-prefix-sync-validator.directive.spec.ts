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

import { kafkaHostPrefixSyncValidator } from './kafka-host-prefix-sync-validator.directive';

describe('KafkaHostPrefixSyncValidator', () => {
  it.each`
    key               | message                                            | domain       | host
    ${'format'}       | ${`Host is not valid`}                             | ${undefined} | ${'ThisIsALongHostNameWithMoreThan63CharactersWhichIsNotValidInOurCase'}
    ${'format'}       | ${`Host is not valid`}                             | ${undefined} | ${'ThisIsAHostNameWithUppercase'}
    ${'max'}          | ${`Max length is 241 characters`}                  | ${undefined} | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
    ${'max'}          | ${`Max length is 239 characters`}                  | ${'a'}       | ${'a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host.a-valid-sub-host'}
    ${'required'}     | ${`Host is required.`}                             | ${undefined} | ${''}
    ${'required'}     | ${`Host is required.`}                             | ${undefined} | ${null}
    ${'firstSegment'} | ${`First segment must be less than 50 characters`} | ${undefined} | ${'01234567890123456789012345678901234567890123456789.0'}
    ${'firstSegment'} | ${`First segment must be less than 50 characters`} | ${undefined} | ${'012345678901234567890123456789012345678901234567890'}
  `('should be invalid host: $host because $message', ({ key, message, domain, host }) => {
    const validatorFn = kafkaHostPrefixSyncValidator(domain);
    expect(validatorFn(new FormControl(host))).toEqual({ [key]: message });
  });

  it('should be valid host without domain', () => {
    const validatorFn = kafkaHostPrefixSyncValidator(undefined);
    expect(validatorFn(new FormControl('host'))).toBeNull();
  });

  it('should be valid host with domain', () => {
    const validatorFn = kafkaHostPrefixSyncValidator('kafka.value.dev');
    expect(validatorFn(new FormControl('host'))).toBeNull();
  });

  it('should be valid host with multiple segments', () => {
    const validatorFn = kafkaHostPrefixSyncValidator('kafka.value.dev');
    expect(validatorFn(new FormControl('host.dev-segment'))).toBeNull();
  });
});
