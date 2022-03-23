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
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

import { ConfigurationService } from './configuration.service';

declare let grecaptcha: any;

@Injectable({
  providedIn: 'root',
})
export class ReCaptchaService {
  private readonly headerName: string = 'X-Recaptcha-Token';
  private readonly scriptId: string = 'reCaptcha';
  private siteKey: string;
  private enabled = false;
  private loaded = false;
  private reCaptchaToken: string;
  private display = false;

  constructor(private configurationService: ConfigurationService, private router: Router) {
    this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      // Hide recaptcha badge by default (let each component decide whether it should display the recaptcha badge or not).
      this.hideBadge();
    });
  }

  load(): Promise<any> {
    // eslint-disable-next-line @typescript-eslint/no-this-alias
    const self = this;
    this.enabled = this.configurationService.get('recaptcha.enabled');
    this.siteKey = this.configurationService.get('recaptcha.siteKey');

    return new Promise((resolve, reject) => {
      if (this.enabled && !document.getElementById(this.scriptId)) {
        if (!this.siteKey) {
          reject('[reCaptchaService] Missing public site_key');
        } else {
          const script = document.createElement('script');
          script.id = this.scriptId;
          script.src = `https://www.google.com/recaptcha/api.js?render=${this.siteKey}`;
          script.async = true;
          script.onload = () =>
            grecaptcha.ready(() => {
              resolve({});
              self.loaded = true;
              self.displayOrHideBadge();
            });

          document.head.appendChild(script);
        }
      } else {
        resolve({});
      }
    });
  }

  async execute(action: string): Promise<string> {
    if (this.enabled) {
      if (!this.loaded) {
        await this.load();
      }
      return grecaptcha.execute(this.siteKey, { action }).then((ReCaptchaToken: string) => {
        this.reCaptchaToken = ReCaptchaToken;
      });
    }
    return Promise.resolve(null);
  }

  getCurrentToken(): string {
    return this.reCaptchaToken;
  }

  getHeaderName(): string {
    return this.headerName;
  }

  isEnabled(): boolean {
    return this.enabled;
  }

  displayBadge() {
    this.display = true;

    if (this.enabled && this.loaded) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      document.getElementsByClassName('grecaptcha-badge')[0].style.setProperty('visibility', 'initial');
    }
  }

  hideBadge() {
    this.display = false;

    if (this.enabled && this.loaded) {
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore
      document.getElementsByClassName('grecaptcha-badge')[0].style.setProperty('visibility', 'collapse', 'important');
    }
  }

  private displayOrHideBadge() {
    if (this.display === true) {
      this.displayBadge();
    } else {
      this.hideBadge();
    }
  }
}
