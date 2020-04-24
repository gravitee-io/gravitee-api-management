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
import { Component, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import {
  AbstractControl,
  AbstractControlOptions, AsyncValidatorFn,
  FormBuilder,
  FormControl,
  FormGroup,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { unwrapLazyLoadHelperCall } from '@angular/localize/src/tools/src/translate/source_files/source_file_utils';
import { ActivatedRoute } from '@angular/router';
import { UsersService, FinalizeRegistrationInput } from '@gravitee/ng-portal-webclient';
import { NotificationService } from '../../../services/notification.service';
import { TokenService } from '../../../services/token.service';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { GvValidators } from '../../../utils/gv-validators';

@Component({
  selector: 'app-registration-confirmation',
  templateUrl: './registration-confirmation.component.html',
  styleUrls: ['./registration-confirmation.component.css']
})
export class RegistrationConfirmationComponent implements OnInit {

  registrationConfirmationForm: FormGroup;
  isSubmitted: boolean;
  token: string;
  userFromToken: any;
  isTokenExpired: boolean;

  constructor(
    private usersService: UsersService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private notificationService: NotificationService,
    private tokenService: TokenService,
  ) {
  }

  ngOnInit() {
    this.isSubmitted = false;
    this.token = this.route.snapshot.paramMap.get('token');
    this.userFromToken = this.tokenService.parseToken(this.token);
    this.isTokenExpired = this.tokenService.isParsedTokenExpired(this.userFromToken);

    this.registrationConfirmationForm = this.formBuilder.group({
      firstname: new FormControl({ value: this.userFromToken.firstname, disabled: true }),
      lastname: new FormControl({ value: this.userFromToken.lastname, disabled: true }),
      email: new FormControl({ value: this.userFromToken.email, disabled: true }),
      password: new FormControl('', Validators.required),
      confirmedPassword: new FormControl('', Validators.required)
    });

    this.registrationConfirmationForm.get('confirmedPassword')
      .setValidators([Validators.required, GvValidators.sameValueValidator(this.registrationConfirmationForm.get('password'))]);

    if (this.isTokenExpired) {
      this.notificationService.info(i18n('registrationConfirmation.tokenExpired'));
    }
  }

  onSubmitRegistrationConfirmationForm() {
    if (this.registrationConfirmationForm.valid && !this.isSubmitted) {
      const input: FinalizeRegistrationInput = {
        token: this.token,
        password: this.registrationConfirmationForm.value.password,
        firstname:this.userFromToken.firstname,
        lastname: this.userFromToken.lastname
      };
      this.usersService.finalizeUserRegistration({ FinalizeRegistrationInput: input }).subscribe(
        () => {
          this.notificationService.success(i18n('registrationConfirmation.notification.success'));
          this.isSubmitted = true;
        }
      );
    }
  }
}
