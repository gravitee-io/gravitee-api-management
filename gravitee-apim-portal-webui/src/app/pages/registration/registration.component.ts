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
import { RegisterUserInput, UsersService, CustomUserFields } from '../../../../projects/portal-webclient-sdk/src/lib';
import { ReCaptchaService } from '../../services/recaptcha.service';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css'],
})
export class RegistrationComponent implements OnInit {
  isSubmitted: boolean;
  registrationForm: FormGroup;
  customUserFields: Array<CustomUserFields>;

  // boolean used to display the form only once the FormGroup is completed using the CustomUserFields.
  canDisplayForm = false;

  constructor(private usersService: UsersService, private formBuilder: FormBuilder, private reCaptchaService: ReCaptchaService) {
    this.isSubmitted = false;
  }

  ngOnInit() {
    const formDescriptor: any = {
      firstname: new FormControl('', Validators.required),
      lastname: new FormControl('', Validators.required),
      email: new FormControl('', [Validators.required, Validators.email]),
    };

    this.usersService
      .listCustomUserFields()
      .toPromise()
      .then(response => {
        this.customUserFields = response;

        if (this.customUserFields) {
          this.customUserFields.forEach(field => {
            formDescriptor[field.key] = new FormControl('', field.required ? Validators.required : null);
          });
        }

        this.registrationForm = this.formBuilder.group(formDescriptor);
      })
      .catch(() => {
        // in case of error load the minimal form
        // user will be able to complete information through the account page
        // or admin will reject the registration
        this.registrationForm = this.formBuilder.group(formDescriptor);
      })
      .finally(() => {
        this.canDisplayForm = true;
      });

    this.reCaptchaService.displayBadge();
  }

  onSubmitRegistration() {
    if (this.registrationForm.valid && !this.isSubmitted) {
      const registerUserInput: RegisterUserInput = {
        email: this.registrationForm.value.email,
        firstname: this.registrationForm.value.firstname,
        lastname: this.registrationForm.value.lastname,
        confirmation_page_url: window.location.href + '/confirm',
      };

      if (this.customUserFields) {
        registerUserInput.customFields = {};
        this.customUserFields.forEach(field => {
          registerUserInput.customFields[field.key] = this.registrationForm.get(field.key).value;
        });
      }

      this.reCaptchaService.execute('registration').then(() => {
        this.usersService
          .registerNewUser({ registerUserInput })
          .toPromise()
          .then(() => (this.isSubmitted = true))
          .catch(() => ({}));
      });
    }
  }
}
