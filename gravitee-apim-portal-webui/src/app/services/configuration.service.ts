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
import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { applyTheme } from '@gravitee/ui-components/src/lib/theme';

import { FeatureEnum } from '../model/feature.enum';

@Injectable({
  providedIn: 'root',
})
export class ConfigurationService {
  private http = inject(HttpClient);
  private document = inject(DOCUMENT);

  private config: any;
  private bootstrapFailed = false;

  public get(key: string, defaultValue?: any) {
    const value = key.split('.').reduce((prev, curr) => prev && prev[curr], this.config);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }

  public load() {
    return new Promise(resolve => {
      this.http.get('./assets/config.json').subscribe((configJson: any) => {
        document.documentElement.style.setProperty('--gv-theme-loader', `url('${configJson.loaderURL}')`);

        const baseURL = this._sanitizeBaseURLs(configJson);
        const enforcedEnvironmentId = this._getEnforcedEnvironmentId(configJson);
        let bootstrapUrl: string;
        if (enforcedEnvironmentId) {
          bootstrapUrl = `${baseURL}/ui/bootstrap?environmentId=${enforcedEnvironmentId}`;
        } else {
          bootstrapUrl = `${baseURL}/ui/bootstrap`;
        }
        this.http
          .get(`${bootstrapUrl}`)
          .toPromise()
          .then((bootstrapResponse: any) => {
            this.bootstrapFailed = false;
            const environmentBaseUrl = `${bootstrapResponse.baseURL}/environments/${bootstrapResponse.environmentId}`;
            this.config = {};
            this.config.baseURL = environmentBaseUrl;
            this.http
              .get(`${this.config.baseURL}/theme?type=PORTAL`)
              .toPromise()
              .then(theme => {
                applyTheme(theme);
              });

            this.http.get(`${this.config.baseURL}/configuration`).subscribe(
              configPortal => {
                this.config = this._deepMerge(configJson, configPortal);
                this.config.baseURL = environmentBaseUrl;
                resolve(true);
              },
              () => resolve(false),
            );
          })
          .catch(error => {
            // Without the bootstrap response there is no API to talk to: show an error instead of running on defaults
            this.bootstrapFailed = true;
            this.showBootstrapError(error);
            resolve(false);
          });
      });
    });
  }

  public hasBootstrapFailed(): boolean {
    return this.bootstrapFailed;
  }

  private showBootstrapError(error: HttpErrorResponse) {
    const errorElement = this.document.getElementById('gravitee-bootstrap-error');
    if (errorElement) {
      errorElement.style.display = 'flex';
    }
    const maintenanceError = error?.error?.errors?.find(e => e.code === 'errors.maintenance.mode');
    const messageElement = this.document.getElementById('gravitee-bootstrap-error-message');
    if (maintenanceError && messageElement) {
      messageElement.textContent = maintenanceError.message;
    }
    const retryElement = this.document.getElementById('gravitee-bootstrap-error-retry');
    if (retryElement) {
      // Through DOCUMENT rather than the window global, which is not replaceable under jsdom 26.
      retryElement.onclick = () => this.document.location.reload();
    }
    const loaderElement = this.document.getElementById('loader');
    if (loaderElement) {
      loaderElement.style.display = 'none';
    }
  }

  _sanitizeBaseURLs(config: any): string {
    let baseURL = config.baseURL;
    if (config.baseURL.endsWith('/')) {
      baseURL = config.baseURL.slice(0, -1);
    }
    const envIndex = baseURL.indexOf('/environments');
    if (envIndex >= 0) {
      baseURL = baseURL.substr(0, envIndex);
    }
    return baseURL;
  }

  _getEnforcedEnvironmentId(config: any): string | undefined {
    let environmentId;
    if (config.environmentId) {
      environmentId = config.environmentId;
    } else {
      const baseURL = config.baseURL;
      const orgIndex = baseURL.indexOf('/environments/');
      if (orgIndex >= 0) {
        const subPathWithOrga = baseURL.substr(orgIndex, baseURL.length);
        const splitArr = subPathWithOrga.split('/');
        if (splitArr.length >= 3) {
          environmentId = splitArr[2];
        }
      }
    }
    return environmentId;
  }

  public hasFeature(feature: FeatureEnum): boolean {
    return this.get(feature);
  }

  _deepMerge(target, source) {
    for (const key of Object.keys(source)) {
      if (source[key] instanceof Object && key in target) {
        Object.assign(source[key], this._deepMerge(target[key], source[key]));
      }
    }
    Object.assign(target || {}, source);
    return target;
  }
}
