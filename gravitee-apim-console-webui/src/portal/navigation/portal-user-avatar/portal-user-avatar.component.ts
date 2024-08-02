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
import { GioAvatarModule } from '@gravitee/ui-particles-angular';
import { MatDivider } from '@angular/material/divider';
import { MatIcon } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';
import { MatMenu, MatMenuItem, MatMenuTrigger } from '@angular/material/menu';
import { AsyncPipe } from '@angular/common';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

import { User } from '../../../entities/user/user';
import { CurrentUserService } from '../../../services-ngx/current-user.service';
import { AuthService } from '../../../auth/auth.service';

interface UserVM {
  user: User;
  shortName: string;
  picture: string;
}

@Component({
  selector: 'portal-user-avatar',
  standalone: true,
  imports: [GioAvatarModule, MatDivider, MatIcon, MatIconButton, MatMenu, MatMenuItem, AsyncPipe, MatMenuTrigger],
  templateUrl: './portal-user-avatar.component.html',
  styleUrl: './portal-user-avatar.component.scss',
})
export class PortalUserAvatarComponent implements OnInit {
  userData$: Observable<UserVM> = of();

  constructor(
    private currentUserService: CurrentUserService,
    public readonly authService: AuthService,
  ) {}

  ngOnInit() {
    this.userData$ = this.currentUserService.current().pipe(
      map((user) => ({
        user,
        picture: this.currentUserService.getUserPictureUrl(user),
        shortName: this.getUserShortName(user),
      })),
    );
  }

  signOut() {
    this.authService.logout().subscribe();
  }

  private getUserShortName(user: User): string {
    if (user.firstname && user.lastname) {
      const capitalizedFirstName = user.firstname[0].toUpperCase() + user.firstname.slice(1);
      const shortLastName = user.lastname[0].toUpperCase();
      return `${capitalizedFirstName} ${shortLastName}.`;
    }
    return user.displayName;
  }
}
