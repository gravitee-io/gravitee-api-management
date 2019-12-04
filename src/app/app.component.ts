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
import { TranslateService } from '@ngx-translate/core';
import { environment } from '../environments/environment';
import { Title } from '@angular/platform-browser';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { UserService } from '@gravitee/ng-portal-webclient';
import { CurrentUserService } from './services/current-user.service';
import { NotificationService } from './services/notification.service';
import { ActivatedRoute, NavigationEnd, NavigationStart, Router } from '@angular/router';
import { RouteService } from './services/route.service';
import { addTranslations, setLanguage } from '@gravitee/ui-components/src/lib/i18n';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private userService: UserService,
    private currentUserService: CurrentUserService,
    private notificationService: NotificationService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private routeService: RouteService
  ) {

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.notificationService.reset();
      } else if (event instanceof NavigationEnd) {
        const currentRoute: ActivatedRoute = this.routeService.findCurrentRoute(this.activatedRoute);
        this._setBrowserTitle(currentRoute);
      }
    });
  }

  ngOnInit() {
    this.translateService.addLangs(environment.locales);
    this.translateService.setDefaultLang(environment.locales[0]);
    const browserLang = this.translateService.getBrowserLang();
    this.translateService.use(environment.locales.includes(browserLang) ? browserLang : 'en').subscribe((translations) => {
      setLanguage(this.translateService.currentLang);
      addTranslations(this.translateService.currentLang, translations);
    });
    this.translateService.get(i18n('site.title')).subscribe(title => this.titleService.setTitle(title));

    this.currentUserService.changeUser(this.userService.getCurrentUser());

  }

  private _setBrowserTitle(currentRoute: ActivatedRoute) {
    this.translateService.get(i18n('site.title')).subscribe((siteTitle) => {
      const data = currentRoute.snapshot.data;
      if (data && data.title) {
        this.translateService.get(data.title).subscribe((title) => this.titleService.setTitle(`${ siteTitle } - ${ title }`));
      } else {
        this.translateService.get(data.title).subscribe((title) => this.titleService.setTitle(siteTitle));
      }
    });
  }

}
