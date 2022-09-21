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

export enum UtmCampaign {
  API_DESIGNER = 'api_designer',
  API_PROMOTION = 'api_promotion',
  DISCOVER_COCKPIT = 'discover_cockpit',
}

@Injectable({
  providedIn: 'root',
})
export class CockpitService {
  addQueryParamsForAnalytics(cockpitUrl: string, utmCampaign: UtmCampaign, cockpitInstallationStatus?: string): string {
    let enhancedCockpitUrl = cockpitUrl;
    if (enhancedCockpitUrl.indexOf('?') > 0) {
      enhancedCockpitUrl += '&';
    } else {
      enhancedCockpitUrl += '?';
    }

    // common query parameters
    enhancedCockpitUrl += 'utm_source=apim&utm_medium=InApp';

    // add campaign param
    enhancedCockpitUrl += `&utm_campaign=${utmCampaign}`;

    // if DISCOVER_COCKPIT, add cockpit installation status
    if (utmCampaign === UtmCampaign.DISCOVER_COCKPIT || utmCampaign === UtmCampaign.API_DESIGNER) {
      const utmTerm = this.computeCockpitStatusForQueryParam(cockpitInstallationStatus);
      enhancedCockpitUrl += `&utm_term=${utmTerm}`;
    }

    return enhancedCockpitUrl;
  }

  private computeCockpitStatusForQueryParam(cockpitInstallationStatus: string): string {
    switch (cockpitInstallationStatus) {
      case 'PENDING':
        return 'pending';

      case 'ACCEPTED':
        return 'registered';

      case 'REJECTED':
        return 'rejected';

      case 'DELETED':
        return 'removed';

      default:
        return 'not_registered';
    }
  }
}
