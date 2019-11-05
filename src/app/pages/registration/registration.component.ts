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
import { FormGroup, FormBuilder } from '@angular/forms';
import { UsersService, RegisterUserInput } from 'ng-portal-webclient/dist';
import { TranslateService } from '@ngx-translate/core';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent implements OnInit {
  isSubmitted: boolean;
  registrationForm: FormGroup;
  notification: {
    message: string;
    type: string;
  };

  constructor(
    private usersService: UsersService,
    private formBuilder: FormBuilder,
    private translateService: TranslateService
  ) {}

  ngOnInit() {
    this.registrationForm = this.formBuilder.group({
      firstname: '',
      lastname: '',
      email: ''
    });

    this.isSubmitted = false;
  }

  isFormValid() {
    return this.registrationForm.valid.valueOf();
  }

  registration() {
    if (this.isFormValid() && !this.isSubmitted) {
      let input: RegisterUserInput;
      input = {
        email: this.registrationForm.value.email,
        firstname: this.registrationForm.value.firstname,
        lastname: this.registrationForm.value.lastname,
        confirmation_page_url: window.location.href + '/confirm'
      };

      // call the register resource from the API.
      this.usersService.registerNewUser(input).subscribe(
        user => {
          this.translateService
            .get(i18n('registration.notification.success'), {
              email: user.email
            })
            .subscribe(translatedMessage => {
              this.notification = {
                message: translatedMessage,
                type: 'success'
              };
            });
          this.isSubmitted = true;
        },
        httpError => {
          this.notification = {
            message: httpError.error.errors[0].detail,
            type: 'error'
          };
          console.error(httpError);
        }
      );
    }
  }
}
