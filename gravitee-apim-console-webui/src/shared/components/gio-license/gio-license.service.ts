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
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, shareReplay } from 'rxjs/operators';

import { Feature, FeatureInfoData, Pack } from './gio-license-features';
import { UTM_DATA, UTMMedium } from './gio-license-utm';

import { License } from '../../../entities/license/License';
import { Constants } from '../../../entities/Constants';
import { FeatureMoreInformation } from '../../../entities/feature/FeatureMoreInformation';

@Injectable({
  providedIn: 'root',
})
export class GioLicenseService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  private loadLicense$: Observable<License> = this.http.get<License>(`${this.constants.v2BaseURL}/license`).pipe(shareReplay(1));

  getLicense$(): Observable<License> {
    return this.loadLicense$;
  }

  isMissingPack$(pack: Pack): Observable<boolean> {
    return this.getLicense$().pipe(map((license) => license === null || !license.packs.includes(pack)));
  }

  isMissingFeature$(feature: string): Observable<boolean> {
    return this.getLicense$().pipe(map((license) => license == null || license.features.find((feat) => feat === feature) == null));
  }

  getFeatureMoreInformation(feature: Feature): FeatureMoreInformation {
    const featureMoreInformation = FeatureInfoData[feature];
    if (!featureMoreInformation) {
      throw new Error(`Unknown Feature value ${feature}. Expected one of ${Object.keys(FeatureInfoData)}`);
    }
    return FeatureInfoData[feature];
  }

  getTrialURL(medium: UTMMedium): string {
    return UTM_DATA[medium].buildURL('https://gravitee.io/self-hosted-trial');
  }
}
