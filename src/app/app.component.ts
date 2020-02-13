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
import { AfterViewInit, Component, HostListener, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { CurrentUserService } from './services/current-user.service';
import { NotificationService } from './services/notification.service';
import { ActivatedRoute, NavigationEnd, NavigationStart, Router } from '@angular/router';
import { INavRoute, NavRouteService } from './services/nav-route.service';
import '@gravitee/ui-components/wc/gv-theme';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html'
})
export class AppComponent implements AfterViewInit, OnDestroy {

  isPreview = false;

  constructor(
    private titleService: Title,
    private translateService: TranslateService,
    private currentUserService: CurrentUserService,
    private notificationService: NotificationService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private navRouteService: NavRouteService,
  ) {

    this.activatedRoute.queryParamMap.subscribe(params => {
      if (params.has('preview') && params.get('preview') === 'on') {
        sessionStorage.setItem('gvPreview', 'true');
      }
    });

    this.router.events.subscribe((event) => {
      if (event instanceof NavigationStart) {
        this.notificationService.reset();
      } else if (event instanceof NavigationEnd) {
        const currentRoute: ActivatedRoute = this.navRouteService.findCurrentRoute(this.activatedRoute);
        this._setBrowserTitle(currentRoute);
        const gvPreview = sessionStorage.getItem('gvPreview');
        if (gvPreview) {
          this.isPreview = true;
          this.notificationService.info('On preview mode');
        }
      }
    });

    this.notificationService.info('build');
  }

  @HostListener('window:beforeunload')
  async ngOnDestroy() {
    sessionStorage.removeItem('gvPreview');
  }

  ngAfterViewInit() {
    document.querySelector('#loader').remove();
  }

  @HostListener(':gv-theme:error', ['$event.detail'])
  onThemeError(detail) {
    this.notificationService.error(detail.message);
  }

  private _setBrowserTitle(currentRoute: ActivatedRoute) {
    this.translateService.get(i18n('site.title')).subscribe((siteTitle) => {
      const data = currentRoute.snapshot.data;
      if (data && data.title) {
        this.translateService.get(data.title).subscribe((title) => this.titleService.setTitle(`${ siteTitle } - ${ title }`));
      } else {
        this.titleService.setTitle(siteTitle);
      }
    });
  }
}
