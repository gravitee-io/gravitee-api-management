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
import { ValidatorFn } from '@angular/forms';

import { ApiV4 } from '../../../entities/management-api-v2';
import { isUnique, isUniqueAndDoesNotMatchDefaultValue } from '../../../shared/utils';

export const isEndpointNameUnique = (api: ApiV4): ValidatorFn | null => {
  if (!api) {
    return null;
  }
  return isUnique(getExistingNames(api));
};

export const isEndpointNameUniqueAndDoesNotMatchDefaultValue = (api: ApiV4, defaultValue: string): ValidatorFn | null => {
  if (!api) {
    return null;
  }
  return isUniqueAndDoesNotMatchDefaultValue(getExistingNames(api), defaultValue);
};

function getExistingNames(api: ApiV4): any[] {
  if (!api.endpointGroups) {
    return [];
  }

  return api.endpointGroups.flatMap(endpointGroup => [endpointGroup.name, ...endpointGroup.endpoints.flatMap(endpoint => endpoint.name)]);
}
