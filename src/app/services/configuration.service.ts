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
import { HttpClient } from '@angular/common/http';
import { FeatureEnum } from '../model/feature.enum';

@Injectable({
  providedIn: 'root'
})
export class ConfigurationService {

  private config: object;

  constructor(private http: HttpClient) {}

  public get(key: string) {
    return key.split('.').reduce((prev, curr) => prev && prev[curr], this.config);
  }

  public load() {
    return new Promise((resolve) => {
      this.http.get('./assets/config.json').subscribe( (configJson: any) => {
        this.http.get(configJson.baseUrl + '/configuration').subscribe( (configPortal) => {
          this.config = { ...configJson, ...configPortal };
          resolve(true);
        });
      });
    });
  }

  public hasFeature(feature: FeatureEnum): boolean {
    return this.get(feature);
  }
}
