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
import { Component, HostListener, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { Router } from '@angular/router';
import { CurrentUserService } from '../../services/current-user.service';
import { RouteService, RouteType } from '../../services/route.service';
import { User } from '@gravitee/ng-portal-webclient';
import '@gravitee/ui-components/wc/gv-nav';
import '@gravitee/ui-components/wc/gv-user-menu';
import '@gravitee/ui-components/wc/gv-user-menu';
import { NotificationService } from '../../services/notification.service';
import { Notification } from '../../model/notification';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html'
})
export class LayoutComponent implements OnInit {

  public mainRoutes: object[];
  public userRoutes: object[];
  public currentUser: User;
  public notification: Notification;

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private router: Router,
    private currentUserService: CurrentUserService,
    private routeService: RouteService,
    private notificationService: NotificationService,
  ) {}

  ngOnInit() {
    this.currentUserService.currentUser.subscribe(newCurrentUser => this.currentUser = newCurrentUser);
    this.notificationService.notification.subscribe(notification => {
      if (notification && !notification.message) {
        this.translateService.get(i18n(notification.code), notification.parameters).subscribe((translatedMessage) => {
          notification.message = translatedMessage;
          this.notification = notification;
        });
      } else {
        this.notification = notification;
      }
    });
    this.mainRoutes = this.routeService.getRoutes(RouteType.main);
    this.userRoutes = this.routeService.getRoutes(RouteType.user);
  }

  showLogin() {
    return !this.currentUser && this.router.url !== '/login';
  }

  @HostListener(':gv-nav-link:click', ['$event.detail'])
  onNavChange(route: { path: any; }) {
    this.router.navigate([route.path]);
  }

}
