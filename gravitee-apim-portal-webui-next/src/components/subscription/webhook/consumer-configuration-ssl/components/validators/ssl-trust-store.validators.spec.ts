/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

import { FormControl, FormGroup } from '@angular/forms';

import { pathOrContentRequired } from './ssl-trust-store.validators';

describe('pathOrContentRequired', () => {
  let form: FormGroup;

  beforeEach(() => {
    form = new FormGroup(
      {
        path: new FormControl(),
        content: new FormControl(),
      },
      { validators: pathOrContentRequired('path', 'content') },
    );
  });

  it.each`
    path      | content
    ${'path'} | ${null}
    ${null}   | ${'content'}
  `('should be valid', ({ path, content }) => {
    form.patchValue({ path, content });
    expect(form.valid).toBeTruthy();
  });

  it.each`
    path         | content
    ${null}      | ${null}
    ${undefined} | ${undefined}
    ${'foo'}     | ${'bar'}
  `('should not be valid', ({ path, content }) => {
    form.patchValue({ path, content });
    expect(form.valid).toBeFalsy();
  });
});
