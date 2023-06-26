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

import { License } from '../../../entities/license/License';
import { Constants } from '../../../entities/Constants';
import { FeatureMoreInformation } from '../../../entities/feature/FeatureMoreInformation';

const featureMoreInformationData = {
  'apim-custom-roles': {
    image: 'assets/gio-ee-unlock-dialog/roles-customisation.png',
    description:
      'Custom Roles is part of Gravitee Enterprise. Custom Roles allows you to specify a wide range of permissions applied to different scopes, which can then be assigned to groups and users.',
  },
  'apim-openid-connect-sso': {
    image: 'assets/gio-ee-unlock-dialog/openid-connect.png',
    description:
      'OpenID Connect is part of Gravitee Enterprise. The OpenID Connect Provider allows users to authenticate to Gravitee using third-party providers like Okta, Keycloak and Ping.',
  },
  'apim-sharding-tags': {
    image: 'assets/gio-ee-unlock-dialog/sharding-tags.png',
    description:
      'Sharding Tags is part of Gravitee Enterprise. Sharding Tags allows you to federate across multiple Gateway deployments, and control which APIs should be deployed where, and by which groups.',
  },
  'apim-audit-trail': {
    image: 'assets/gio-ee-unlock-dialog/audit-trail.png',
    description:
      'Audit is part of Gravitee Enterprise. Audit gives you a complete understanding of events and their context to strengthen your security posture.',
  },
  'apim-debug-mode': {
    image: 'assets/gio-ee-unlock-dialog/debug-mode.png',
    description:
      'Debug Mode is part of Gravitee Enterprise. It provides detailed information about the behaviour of each policy in your flows and trace attributes and data values across execution.',
  },
  'apim-dcr-registration': {
    image: 'assets/gio-ee-unlock-dialog/dcr-providers.png',
    description:
      "Dynamic Client Registration (DCR) Provider is part of Gravitee Enterprise. DCR enhances your API's security by seamlessly integrating OAuth 2.0 and OpenID Connect.",
  },
};

@Injectable({
  providedIn: 'root',
})
export class GioLicenseService {
  constructor(private readonly http: HttpClient, @Inject('Constants') private readonly constants: Constants) {}

  private loadLicense$: Observable<License> = this.http.get<License>(`${this.constants.v2BaseURL}/license`).pipe(shareReplay(1));

  getLicense() {
    return this.loadLicense$;
  }

  notAllowed(feature: string) {
    return this.getLicense().pipe(map((license) => license == null || license.features.find((feat) => feat === feature) == null));
  }

  getFeatureMoreInformation(feature: string): FeatureMoreInformation {
    const featureMoreInformation = featureMoreInformationData[feature];
    if (!featureMoreInformation) {
      throw new Error(`No data defined for '${feature}', you must use one of ${Object.keys(featureMoreInformationData)}`);
    }
    return featureMoreInformation;
  }
}
