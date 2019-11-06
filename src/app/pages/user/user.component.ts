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

import {UserService, User} from 'ng-portal-webclient/dist';
import { CurrentUserService } from '../../services/current-user.service';

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.css']
})

export class UserComponent implements OnInit {
  user: User;

  constructor(
    private userService: UserService,
    private currentUserService: CurrentUserService
  ) { }

  ngOnInit() {
    this.currentUserService.currentUser.subscribe(newCurrentUser => this.user = newCurrentUser);

    this.userService.getCurrentUser().subscribe(
      (user) => {
        const loggedUser = user;
        this.userService.getCurrentUserAvatar().subscribe(
          (avatar) => {
            const reader = new FileReader();
            reader.addEventListener('loadend', (e) => {
              loggedUser.avatar = reader.result.toString();
            });
            reader.readAsDataURL(avatar);
            this.currentUserService.changeUser(loggedUser);
          }
        );
      }
    );

  }

}
