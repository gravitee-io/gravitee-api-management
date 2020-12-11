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
import { User, UserService } from '../../../../../projects/portal-webclient-sdk/src/lib';
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
  isSaving: boolean;

  constructor(
    private currentUserService: CurrentUserService,
    private userService: UserService,
    private notificationService: NotificationService,
    private formBuilder: FormBuilder,
    private eventService: EventService
  ) {
  }

  ngOnInit() {
    this.subscription = this.currentUserService.get().subscribe((user) => {
      this.currentUser = user;
      this.userForm = this.formBuilder.group({
        last_name: new FormControl( { value: this.lastName, disabled: !this.isProfileEditable }, Validators.required),
        first_name: new FormControl( { value: this.firstName, disabled: !this.isProfileEditable }, Validators.required),
        email: new FormControl({ value: this.email, disabled: !this.isProfileEditable }, Validators.email),
        avatar: new FormControl(this.avatar)
      });

      this.userForm.get('avatar').valueChanges.subscribe((avatar) => {
        this.eventService.dispatch(new GvEvent(AppComponent.UPDATE_USER_AVATAR, { data: avatar }));
      });
    });
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
    this.reset();
  }

  get avatar() {
    return this.currentUser._links ? this.currentUser._links.avatar : null;
  }

  get firstName() {
    if (this.currentUser) {
      return this.currentUser.first_name;
    }
    return '';
  }

  get lastName() {
    if (this.currentUser) {
      return this.currentUser.last_name;
    }
    return '';
  }

  get email() {
    if (this.currentUser) {
      return this.currentUser.email;
    }
    return '';
  }

  get isProfileEditable() {
    if (this.currentUser) {
      return this.currentUser.editable_profile;
    }
    return false;
  }

  reset() {
    this.userForm.get('avatar').patchValue(this.avatar);
    this.userForm.get('first_name').patchValue(this.firstName);
    this.userForm.get('last_name').patchValue(this.lastName);
    this.userForm.get('email').patchValue(this.email);
    this.userForm.markAsPristine();
  }

  submit() {
    let avatarValue = this.userForm.get('avatar').value;
    // if avatar start with "http", the avatar doesn't changed, do not 
    // send it to the REST API to avoid reset user avatar
    const UserInput = { 
      id:  this.currentUser.id, 
      avatar: avatarValue && avatarValue.startsWith("http") ? null : avatarValue, 
      first_name: this.userForm.get('first_name').value, 
      last_name: this.userForm.get('last_name').value, 
      email: this.userForm.get('email').value 
    };
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
