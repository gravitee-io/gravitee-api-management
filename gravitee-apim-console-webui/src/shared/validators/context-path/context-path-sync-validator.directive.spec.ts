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

import { contextPathSyncValidator } from './context-path-sync-validator.directive';

describe('TcpHostSyncValidator', () => {
  it.each`
    message                         | contextPath
    ${`Context path is not valid.`} | ${'missingSlash'}
    ${`Context path is not valid.`} | ${'invalid/'}
    ${`Context path is required.`}  | ${''}
    ${`Context path is required.`}  | ${null}
  `('should be invalid context path: $contextPath because $message', ({ message, contextPath }) => {
    expect(contextPathSyncValidator(new FormControl(contextPath))).toEqual({ contextPath: message });
  });

  it('should be valid contextPath', () => {
    expect(contextPathSyncValidator(new FormControl('/contextPath'))).toBeNull();
  });
});
