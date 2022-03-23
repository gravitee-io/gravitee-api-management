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
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { CookieService } from 'ngx-cookie-service';
import { Router, NavigationEnd } from '@angular/router';

import { CookieEnum } from '../model/cookie.enum';
import { FeatureEnum } from '../model/feature.enum';

import { ConfigurationService } from './configuration.service';

declare let gtag: any;

@Injectable({
  providedIn: 'root',
})
export class GoogleAnalyticsService {
  private readonly isGAEnabled: BehaviorSubject<boolean>;
  private readonly isGALoaded: BehaviorSubject<boolean>;
  private readonly scriptGAId = 'GA_ID';
  private readonly scriptGtagId = 'Gtag_ID';
  private trackingId: string;

  constructor(private configurationService: ConfigurationService, private cookieService: CookieService, private router: Router) {
    this.isGAEnabled = new BehaviorSubject<boolean>(undefined);
    this.isGALoaded = new BehaviorSubject<boolean>(undefined);
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        if (this.isEnabled()) {
          this.sendTag(event.urlAfterRedirects);
        }
      }
    });
  }

  private _initScriptTags() {
    if (!document.getElementById(this.scriptGAId)) {
      const scriptGA = document.createElement('script');
      scriptGA.id = this.scriptGAId;
      scriptGA.async = true;
      scriptGA.src = `https://www.googletagmanager.com/gtag/js?id=${this.trackingId}`;
      document.head.appendChild(scriptGA);
    }

    if (!document.getElementById(this.scriptGtagId)) {
      const scriptGtag = document.createElement('script');
      scriptGtag.id = this.scriptGtagId;
      scriptGtag.async = true;
      scriptGtag.innerHTML = 'function gtag(){dataLayer.push(arguments)}window.dataLayer=window.dataLayer||[],gtag("js",new Date);';
      document.head.appendChild(scriptGtag);
    }
  }

  public load() {
    const gaEnabledInConfiguration = this.configurationService.hasFeature(FeatureEnum.googleAnalytics);
    this.trackingId = this.configurationService.get('portal.analytics.trackingId');
    if (gaEnabledInConfiguration && this.trackingId) {
      this._initScriptTags();
      const gaCookieExists: boolean = this.cookieService.check(CookieEnum.googleAnalytics);
      // new user or already known user who has accepted tp be tracked
      if (!gaCookieExists || this.cookieService.get('_ga.enabled') === '1') {
        this.isGAEnabled.next(true);
      } else {
        this.isGAEnabled.next(false);
      }
      this.isGALoaded.next(true);
    } else {
      this.isGALoaded.next(false);
    }
  }

  public enableGA() {
    this._initScriptTags();
    this.isGAEnabled.next(true);
  }

  public disableGA() {
    this.isGAEnabled.next(false);
    const scriptGA = document.getElementById(this.scriptGAId);
    if (scriptGA) {
      scriptGA.remove();
    }
    const scriptGtag = document.getElementById(this.scriptGtagId);
    if (scriptGtag) {
      scriptGtag.remove();
    }
    this._removeGACookies();
  }

  public sendTag(url: string) {
    gtag('config', this.trackingId, { page_path: url, cookieDomain: 'none' });
  }

  public isEnabled() {
    return this.isGAEnabled.getValue();
  }

  public isLoaded() {
    return this.isGALoaded.getValue();
  }

  /**
   * Remove cookies created by Google scripts
   */
  private _removeGACookies() {
    this.cookieService.delete('_ga');
    this.cookieService.delete('_gid');
    this.cookieService.delete('_gat_gtag_' + this.trackingId.replace(/-/g, '_'));
  }
}
