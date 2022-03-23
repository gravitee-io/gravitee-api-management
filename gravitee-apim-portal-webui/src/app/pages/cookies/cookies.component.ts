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
import { CookieEnum } from '../../model/cookie.enum';
import { CookieService } from 'ngx-cookie-service';
import { GoogleAnalyticsService } from '../../services/google-analytics.service';
import '@gravitee/ui-components/wc/gv-switch';
import { marker as i18n } from '@biesbjerg/ngx-translate-extract-marker';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-cookies',
  templateUrl: './cookies.component.html',
  styleUrls: ['./cookies.component.css'],
})
export class CookiesComponent implements OnInit {
  gaCookieEnabled: boolean;

  constructor(
    private googleAnalyticsService: GoogleAnalyticsService,
    private cookieService: CookieService,
    private notificationService: NotificationService,
  ) {}

  ngOnInit() {
    this.gaCookieEnabled = this.cookieService.get(CookieEnum.googleAnalytics) === '1';
  }

  _disableGA() {
    this.googleAnalyticsService.disableGA();
    this.cookieService.set(CookieEnum.googleAnalytics, '0', 365, '/');
    this.gaCookieEnabled = false;
    this.notificationService.success(i18n('cookies.success.disable'), { cookieName: 'Google Analytics' }, null, true);
  }

  _enableGA() {
    this.cookieService.set(CookieEnum.googleAnalytics, '1', 365, '/');
    this.googleAnalyticsService.enableGA();
    this.gaCookieEnabled = true;
    this.notificationService.success(i18n('cookies.success.enable'), { cookieName: 'Google Analytics' }, null, true);
  }

  displayGACookie() {
    this.gaCookieEnabled = this.cookieService.get(CookieEnum.googleAnalytics) === '1';
    return this.googleAnalyticsService.isLoaded();
  }

  toggleGACookie({ detail }) {
    if (detail) {
      this._enableGA();
    } else {
      this._disableGA();
    }
  }
}
