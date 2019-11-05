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
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../environments/environment';

import '@gravitee/ui-components/wc/gv-nav';

import { Title } from '@angular/platform-browser';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { Router, NavigationEnd } from '@angular/router';
import { User } from 'ng-portal-webclient/dist';
import { CurrentUserService } from './services/currentUser.service';
import { routes } from './app-routing.module';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  private mainRoutes: Promise<any>[];
  private userRoutes: Promise<any>[];
  private currentUser: User;
  private withHeader: boolean;

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private router: Router,
    private currentUserService: CurrentUserService
    ) {
      this.withHeader = true;
  }

  ngOnInit() {
    this.currentUserService.currentUser.subscribe(newCurrentUser => this.currentUser = newCurrentUser);
    // avoid header for registration confirmation
    this.router.events.subscribe(
      (event) => {
        if (event instanceof NavigationEnd) {
          this.withHeader = !this.router.url.startsWith('/registration/confirm/');
        }
      }
    );

    this.translateService.addLangs(environment.locales);
    this.translateService.setDefaultLang(environment.locales[0]);
    const browserLang = this.translateService.getBrowserLang();
    this.translateService.use(browserLang.match(/en|fr/) ? browserLang : 'en');

    this.translateService.get(i18n('site.title')).subscribe((title) => {
      this.titleService.setTitle(title);
    });

    this.mainRoutes = routes
      .filter(({data}) => data && (data.navType === 'main'))
      .map(async ({path, data: {title}}) => {
        let isActive = false;
        if (`/${path}` === this.router.url) {
          isActive = true;
        }
        return {path, title: await this.translateService.get(title).toPromise(), isActive};
      });

    this.userRoutes = routes
      .filter(({data}) => data && (data.navType === 'user'))
      .map(async ({path, data: {title}}) => {
        return {path, title: await this.translateService.get(title).toPromise()};
      });
  }

  showLogin() {
    return !this.currentUser && this.router.url !== '/login';
  }

  @HostListener('gv-nav-link_click', ['$event.detail'])
  onNavChange(route: { path: any; }) {
    this.router.navigate([route.path]);
  }

}
