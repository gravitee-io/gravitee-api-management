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
import { Component, Inject, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { Constants } from '../../entities/Constants';
import { User } from '../../entities/user/user';
import { CurrentUserService } from '../../services-ngx/current-user.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'gio-user-menu',
  templateUrl: './gio-user-menu.component.html',
  styleUrls: ['./gio-user-menu.component.scss'],
  standalone: false,
})
export class GioUserMenuComponent implements OnInit {
  @Input()
  public hasAlert = false;
  @Input()
  public userTaskCount = 0;

  public currentUser: User;
  public userShortName: string;
  public userPicture: string;
  public supportEnabled: boolean;
  public newsletterProposed: boolean;

  constructor(
    @Inject(Constants) public readonly constants: Constants,
    public readonly router: Router,
    public readonly activatedRoute: ActivatedRoute,
    public readonly currentUserService: CurrentUserService,
    public readonly authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.currentUserService.current().subscribe((user) => {
      this.currentUser = user;
      this.newsletterProposed =
        (this.currentUser && !this.currentUser.firstLogin) ||
        !!window.localStorage.getItem('newsletterProposed') ||
        !this.constants.org.settings.newsletter.enabled;
      this.userShortName = this.getUserShortName();
      this.userPicture = this.currentUserService.getUserPictureUrl(user);
      this.supportEnabled = this.constants.org.settings.management.support.enabled;
    });
  }

  goToMyAccount(): void {
    this.router.navigate(['my-account'], { relativeTo: this.activatedRoute });
  }

  goToSupport(): void {
    this.router.navigate(['support', 'tickets'], { relativeTo: this.activatedRoute });
  }

  goToTask(): void {
    this.router.navigate(['tasks'], { relativeTo: this.activatedRoute });
  }

  signOut(): void {
    this.authService.logout().subscribe();
  }

  private getUserShortName = (): string => {
    if (this.currentUser.firstname && this.currentUser.lastname) {
      const capitalizedFirstName = this.currentUser.firstname[0].toUpperCase() + this.currentUser.firstname.slice(1);
      const shotLastName = this.currentUser.lastname[0].toUpperCase();
      return `${capitalizedFirstName} ${shotLastName}.`;
    }
    return this.currentUser.displayName;
  };
}
