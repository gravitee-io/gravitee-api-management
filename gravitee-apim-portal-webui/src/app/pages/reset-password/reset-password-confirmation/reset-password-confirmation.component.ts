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
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ChangeUserPasswordInput, UsersService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { TokenService } from '../../../services/token.service';
import { GvValidators } from '../../../utils/gv-validators';
import { ReCaptchaService } from '../../../services/recaptcha.service';

@Component({
  selector: 'app-reset-password-confirmation',
  templateUrl: './reset-password-confirmation.component.html',
  styleUrls: ['./reset-password-confirmation.component.css'],
})
export class ResetPasswordConfirmationComponent implements OnInit {
  resetPasswordConfirmationForm: FormGroup;
  isSubmitted: boolean;
  token: string;
  userFromToken: any;
  isTokenExpired: boolean;

  constructor(
    private usersService: UsersService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private tokenService: TokenService,
    private reCaptchaService: ReCaptchaService,
  ) {}

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken);

    this.resetPasswordConfirmationForm = this.formBuilder.group({
      firstname: new FormControl({ value: this.userFromToken.firstname, disabled: true }),
      lastname: new FormControl({ value: this.userFromToken.lastname, disabled: true }),
      email: new FormControl({ value: this.userFromToken.email, disabled: true }),
      password: new FormControl('', Validators.required),
      confirmedPassword: new FormControl('', Validators.required),
    });

    this.resetPasswordConfirmationForm
      .get('confirmedPassword')
      .setValidators([Validators.required, GvValidators.sameValueValidator(this.resetPasswordConfirmationForm.get('password'))]);
    this.reCaptchaService.displayBadge();
  }

  onSubmitResetPasswordConfirmationForm() {
    if (this.resetPasswordConfirmationForm.valid && !this.isSubmitted) {
      const changeUserPasswordInput: ChangeUserPasswordInput = {
        token: this.token,
        password: this.resetPasswordConfirmationForm.value.password,
        firstname: this.userFromToken.firstname,
        lastname: this.userFromToken.lastname,
      };
      this.reCaptchaService.execute('reset_password_confirmation').then(() => {
        this.usersService
          .changeUserPassword({ changeUserPasswordInput })
          .toPromise()
          .then(() => (this.isSubmitted = true))
          .catch(() => {
            this.resetPasswordConfirmationForm.patchValue({ password: '', confirmedPassword: '' });
          });
      });
    }
  }
}
