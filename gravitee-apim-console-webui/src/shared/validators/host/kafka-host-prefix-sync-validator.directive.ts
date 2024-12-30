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
import { FormControl, ValidatorFn } from '@angular/forms';
import { isEmpty } from 'lodash';

import { HOST_PATTERN_REGEX } from '../../utils';

const MAX_BROKER_PREFIX_LENGTH: number = 'broker-main99-'.length; // Example of broker prefix added by Kafka Gateway to first segment in host prefix
const SEPARATOR_LENGTH: number = '.'.length; // Used between host prefix and domain

const FIRST_HOST_SEGMENT_REGEX_TOO_LONG = new RegExp(/^[^.]{50,}/); // The host is longer than 49 characters and contains no periods

export function kafkaHostPrefixSyncValidator(kafkaDomain?: string): ValidatorFn {
  return (formControl: FormControl) => {
    const host = formControl.value || '';
    if (isEmpty(host.trim())) {
      return { required: 'Host is required.' };
    }
    const maxHostPrefixLength = getMaxHostPrefixLength(kafkaDomain);
    if (host.length > maxHostPrefixLength) {
      return { max: `Max length is ${maxHostPrefixLength} characters` };
    }

    if (!HOST_PATTERN_REGEX.test(host)) {
      return { format: 'Host is not valid' };
    }

    if (FIRST_HOST_SEGMENT_REGEX_TOO_LONG.test(host)) {
      return { firstSegment: 'First segment must be less than 50 characters' };
    }

    return null;
  };
}

function getMaxHostPrefixLength(kafkaDomain?: string): number {
  if (kafkaDomain?.length) {
    return 255 - kafkaDomain.length - SEPARATOR_LENGTH - MAX_BROKER_PREFIX_LENGTH;
  }
  return 255 - MAX_BROKER_PREFIX_LENGTH;
}
