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

import { passwordMatchValidator } from './password-match.validator';

describe('passwordMatchValidator', () => {
  it('should return an error when passwords do not match', () => {
    const formGroup = new FormGroup(
      {
        password: new FormControl('password'),
        confirmedPassword: new FormControl('wrong password'),
      },
      { validators: passwordMatchValidator('password', 'confirmedPassword') },
    );

    expect(formGroup.controls.confirmedPassword.errors).toEqual({ passwordMismatch: true });
  });

  it.each`
    password      | confirmedPassword
    ${null}       | ${'password'}
    ${undefined}  | ${'password'}
    ${'password'} | ${null}
    ${'password'} | ${undefined}
  `('should not return an error when passwords are falsy or match', ({ password, confirmedPassword }) => {
    const formGroup = new FormGroup(
      {
        password: new FormControl(password),
        confirmedPassword: new FormControl(confirmedPassword),
      },
      { validators: passwordMatchValidator('password', 'confirmedPassword') },
    );

    expect(formGroup.controls.confirmedPassword.errors).toBeNull();
  });
});
