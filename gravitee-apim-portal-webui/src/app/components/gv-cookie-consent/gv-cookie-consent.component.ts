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
import '@gravitee/ui-components/wc/gv-button';
import '@gravitee/ui-components/wc/gv-message';
import { Component } from '@angular/core';
import { CookieService } from 'ngx-cookie-service';

import { CookieEnum } from '../../model/cookie.enum';
import { GoogleAnalyticsService } from '../../services/google-analytics.service';

@Component({
  selector: 'app-gv-cookie-consent',
  templateUrl: './gv-cookie-consent.component.html',
  styleUrls: ['./gv-cookie-consent.component.css'],
})
export class GvCookieConsentComponent {
  constructor(private googleAnalyticsService: GoogleAnalyticsService, private cookieService: CookieService) {}

  _disableGA() {
    this.googleAnalyticsService.disableGA();
    this.cookieService.set(CookieEnum.googleAnalytics, '0', 365, '/');
  }

  _dispose() {
    document.querySelector('.cookie__consent').remove();
  }

  _enableGA() {
    this.cookieService.set(CookieEnum.googleAnalytics, '1', 365, '/');
    this.googleAnalyticsService.enableGA();
  }

  displayCookieConsent() {
    const hasGACookie = this.cookieService.check(CookieEnum.googleAnalytics);
    return this.googleAnalyticsService.isLoaded() && !hasGACookie;
  }

  onAcceptClick() {
    this._enableGA();
    this._dispose();
  }

  onDeclineClick() {
    this._disableGA();
    this._dispose();
  }
}
