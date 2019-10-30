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
