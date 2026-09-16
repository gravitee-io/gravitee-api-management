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
import { CUSTOM_ELEMENTS_SCHEMA, Component, OnInit, inject } from '@angular/core';
import { FormControl, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgIf } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

import { ChangeUserPasswordInput, UsersService } from '../../../../../projects/portal-webclient-sdk/src/lib';
import { TokenService } from '../../../services/token.service';
import { GvValidators } from '../../../utils/gv-validators';
import { ReCaptchaService } from '../../../services/recaptcha.service';
import { GvFormControlDirective } from '../../../directives/gv-form-control.directive';

type ResetPasswordConfirmationFormType = FormGroup<{
  firstname: FormControl<string>;
  lastname: FormControl<string>;
  email: FormControl<string>;
  password: FormControl<string>;
  confirmedPassword: FormControl<string>;
}>;

@Component({
  selector: 'app-reset-password-confirmation',
  templateUrl: './reset-password-confirmation.component.html',
  styleUrls: ['./reset-password-confirmation.component.css'],
  imports: [ReactiveFormsModule, NgIf, GvFormControlDirective, RouterLink, TranslatePipe],
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class ResetPasswordConfirmationComponent implements OnInit {
  private usersService = inject(UsersService);
  private route = inject(ActivatedRoute);
  private tokenService = inject(TokenService);
  private reCaptchaService = inject(ReCaptchaService);

  resetPasswordConfirmationForm: ResetPasswordConfirmationFormType;
  isSubmitted: boolean;
  token: string;
  userFromToken: any;
  isTokenExpired: boolean;

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken);

    this.resetPasswordConfirmationForm = new FormGroup({
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
