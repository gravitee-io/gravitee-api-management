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

import { ApiFakers } from '../../fakers/apis';
import { ADMIN_USER, API_PUBLISHER_USER, APPLICATION_USER, LOW_PERMISSION_USER } from '../../fakers/users/users';
import { gio } from '../../commands/gravitee.commands';
import { PortalSettings } from '../../model/portal-settings';
import { ManagementError } from '../../model/technical';

describe('Bulk Rate', () => {
  it('Should enable API rating', () => {
    const settings: PortalSettings = {
      portal: {
        rating: {
          enabled: true,
          comment: {
            mandatory: false,
          },
        },
      },
    };
    gio
      .management(ADMIN_USER)
      .portalSettings()
      .postPortalSettings(settings)
      .ok()
      .should((response) => {
        const responseSettings: PortalSettings = response.body;
        expect(responseSettings.portal.rating.enabled).to.be.true;
      });
  });

  it('Should rate the API by API_PUBLISHER', () => {
    gio
      .management(API_PUBLISHER_USER)
      .apis()
      .getAll()
      .ok()
      .should((response) => {
        let apis = response.body;

        apis.forEach((api, index) => {
          if (index % 2 == 0) {
            gio.management(LOW_PERMISSION_USER).apisRating().addApiRating<ManagementError>(api.id, {
              rate: ApiFakers.apiRating(),
            });

            gio.management(APPLICATION_USER).apisRating().addApiRating<ManagementError>(api.id, {
              rate: ApiFakers.apiRating(),
            });

            gio.management(API_PUBLISHER_USER).apisRating().addApiRating<ManagementError>(api.id, {
              rate: ApiFakers.apiRating(),
            });

            gio.management(ADMIN_USER).apisRating().addApiRating<ManagementError>(api.id, {
              rate: ApiFakers.apiRating(),
            });
          }
        });
      });
  });
});
