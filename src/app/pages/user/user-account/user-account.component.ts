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
import { User, UserService } from '@gravitee/ng-portal-webclient';
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
        display_name: new FormControl( { value: this.displayName, disabled: true }, Validators.required),
        email: new FormControl({ value: this.email, disabled: true }, Validators.required),
        avatar: new FormControl(this.avatar, Validators.required)
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
    this.userForm.markAsPristine();
  }

  submit() {
    const UserInput = { id:  this.currentUser.id, avatar: this.userForm.get('avatar').value };
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
