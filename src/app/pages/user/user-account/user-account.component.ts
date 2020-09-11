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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';
import '@gravitee/ui-components/wc/gv-file-upload';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { AppComponent } from '../../../app.component';
import { CurrentUserService } from '../../../services/current-user.service';
import { CustomUserFields, User, UserService, UsersService } from '@gravitee/ng-portal-webclient';
import { EventService, GvEvent } from '../../../services/event.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-user-account',
  templateUrl: './user-account.component.html',
  styleUrls: ['./user-account.component.css']
})

export class UserAccountComponent implements OnInit, OnDestroy {

  private subscription: any;
  public currentUser: User;
  public userForm: FormGroup;
  customUserFields: Array<CustomUserFields>;

  fields: any = {};

  isSaving: boolean;

  // boolean used to display the form only once the FormGroup is completed using the CustomUserFields.
  canDisplayForm = false;
  avatarHasChanged = false;

  constructor(
    private currentUserService: CurrentUserService,
    private userService: UserService,
    private usersService: UsersService,
    private notificationService: NotificationService,
    private formBuilder: FormBuilder,
    private eventService: EventService
  ) {
  }

  ngOnInit() {

    this.subscription = this.currentUserService.get().subscribe((user) => {
      this.currentUser = user;
      const formDescriptor: any = {
        display_name: new FormControl( { value: this.displayName, disabled: true }, Validators.required),
        email: new FormControl({ value: this.email, disabled: true }, Validators.required),
        avatar: new FormControl(this.avatar, Validators.required)
      };

      if (this.currentUser.customFields) {
        this.usersService.listCustomUserFields().toPromise().then((respo) => {
            this.customUserFields = respo;

            if (this.customUserFields) {
              this.customUserFields.forEach((field) => {
                const controlField = new FormControl('', field.required ? Validators.required : null);
                controlField.setValue(this.currentUser.customFields[field.key]);
                formDescriptor[field.key] = controlField;
              });
            }

            this.userForm = this.formBuilder.group(formDescriptor);

            this.userForm.get('avatar').valueChanges.subscribe((avatar) => {
              this.eventService.dispatch(new GvEvent(AppComponent.UPDATE_USER_AVATAR, { data: avatar }));
              this.avatarHasChanged = true;
            });

            this.canDisplayForm = true;

          }
        );
      } else {

        this.userForm = this.formBuilder.group(formDescriptor);

        this.userForm.get('avatar').valueChanges.subscribe((avatar) => {
          this.eventService.dispatch(new GvEvent(AppComponent.UPDATE_USER_AVATAR, { data: avatar }));
          this.avatarHasChanged = true;
        });

        this.canDisplayForm = true;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.reset();
  }

  get avatar() {
    return this.currentUser._links ? this.currentUser._links.avatar : null;
  }

  get displayName() {
    if (this.currentUser) {
      return this.currentUser.first_name ? `${this.currentUser.first_name} ${this.currentUser.last_name}` : this.currentUser.display_name;
    }
    return '';
  }

  get email() {
    if (this.currentUser) {
      return this.currentUser.email;
    }
    return '';
  }

  reset() {
    this.userForm.get('avatar').patchValue(this.avatar);

    if (this.customUserFields) {
      this.customUserFields.forEach((field) => {
        this.userForm.get(field.key).setValue(this.currentUser.customFields[field.key]);
      });
    }

    this.userForm.markAsPristine();
  }

  submit() {
    const UserInput: any = {
      id:  this.currentUser.id
    };

    if (this.avatarHasChanged) {
      const avatarProp = 'avatar';
      UserInput[avatarProp] = this.userForm.get(avatarProp).value;
    }

    if (this.customUserFields && this.customUserFields.length >0 ) {
      const customFields : any = {}
      this.customUserFields.forEach((field) => {
        customFields[field.key] = this.userForm.get(field.key).value;
      });
      const customFieldsProp = 'customFields';
      UserInput[customFieldsProp] = customFields;
    }

    this.isSaving = true;
    this.userService.updateCurrentUser({ UserInput })
      .toPromise()
      .then((user) => {
        this.currentUserService.set(user);
        this.notificationService.success(i18n('user.account.success'));
      })
      .finally(() => this.isSaving = false);
  }
}
