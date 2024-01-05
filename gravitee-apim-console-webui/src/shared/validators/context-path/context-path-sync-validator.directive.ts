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
import { FormControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { isEmpty } from 'lodash';

const PATH_PATTERN_REGEX = new RegExp(/^\/[/.a-zA-Z0-9-_]*$/);

export const contextPathSyncValidator: ValidatorFn = (formControl: FormControl): ValidationErrors | null => {
  const contextPath: string = formControl.value;
  if (isEmpty(contextPath)) {
    return {
      contextPath: 'Context path is required.',
    };
  } else if (contextPath.includes('//')) {
    return { contextPath: 'Context path is not valid.' };
  } else if (!PATH_PATTERN_REGEX.test(contextPath)) {
    return { contextPath: 'Context path is not valid.' };
  }
  return null;
};
