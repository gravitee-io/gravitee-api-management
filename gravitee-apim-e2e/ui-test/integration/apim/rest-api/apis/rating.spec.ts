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
import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { ApiFakers } from '@fakers/apis';
import { Api, ApiDefinition, ApiLifecycleState, ApiRating, ApiRatingResponse, ApiState, ApiVisibility } from '@model/apis';
import { PortalSettings } from '@model/portal-settings';
import { ManagementError } from '@model/technical';
import { gio } from '@commands/gravitee.commands';
import { TechnicalErrorAssertions } from 'ui-test/assertions/error.assertion';
import { ApiAssertions } from 'ui-test/assertions/api.assertion';

context('API - Rating', () => {
  let createdApi: Api;

  describe('Prepare', () => {
    it('Should disable API rating', () => {
      const settings: PortalSettings = {
        portal: {
          rating: {
            enabled: false,
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
          expect(responseSettings.portal.rating.enabled).to.be.false;
        });
    });

    it('Should import API', () => {
      const api: ApiDefinition = ApiFakers.apiDefinition();
      gio
        .management(API_PUBLISHER_USER)
        .apis()
        .importApi(api)
        .ok()
        .should((response) => {
          ApiAssertions.assertThat(response)
            .hasState(ApiState.STOPPED)
            .hasVisibility(ApiVisibility.PRIVATE)
            .hasLifecycleState(ApiLifecycleState.CREATED);
        })
        .then((response) => {
          createdApi = response.body;
        });
    });
  });

  describe('Rate the api', () => {
    it('Should rate the API by API_PUBLISHER', () => {
      const rating: ApiRating = {
        rate: ApiFakers.apiRating(),
      };
      gio.management(API_PUBLISHER_USER).apisRating().addApiRating<ManagementError>(createdApi.id, rating).serviceUnavailable();
    });

    it('Should rate the API by ADMIN_USER', () => {
      const rating: ApiRating = {
        rate: ApiFakers.apiRating(),
      };
      gio.management(ADMIN_USER).apisRating().addApiRating<ManagementError>(createdApi.id, rating).serviceUnavailable();
    });
  });

  describe('Enable API rating service', () => {
    it('Should enable the api rating', () => {
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
  });

  describe('Rate the api', () => {
    it('Should rate the API by API_PUBLISHER', () => {
      const rating: ApiRating = {
        rate: ApiFakers.apiRating(),
      };
      gio
        .management(API_PUBLISHER_USER)
        .apisRating()
        .addApiRating(createdApi.id, rating)
        .ok()
        .should((response) => {
          const ratingResponse: ApiRatingResponse = response.body;
          expect(ratingResponse.rate).to.be.equal(rating.rate);
        });
    });

    it('Should rate the API by ADMIN_USER', () => {
      const rating: ApiRating = {
        rate: ApiFakers.apiRating(),
      };
      gio
        .management(ADMIN_USER)
        .apisRating()
        .addApiRating(createdApi.id, rating)
        .ok()
        .should((response) => {
          const ratingResponse: ApiRatingResponse = response.body;
          expect(ratingResponse.rate).to.be.equal(rating.rate);
        });
    });

    it('Should not rate again the API by API_PUBLISHER', () => {
      const rating: ApiRating = {
        rate: ApiFakers.apiRating(),
      };
      gio
        .management(API_PUBLISHER_USER)
        .apisRating()
        .addApiRating<ManagementError>(createdApi.id, rating)
        .badRequest()
        .should((response) => {
          TechnicalErrorAssertions.assertThat(response)
            .containsMessage(`Rating already exists for api [${createdApi.id}]`)
            .containsCode('rating.exists');
        });
    });

    it('Should not rate again the API by ADMIN_USER', () => {
      const rating: ApiRating = {
        rate: ApiFakers.apiRating(),
      };
      gio
        .management(ADMIN_USER)
        .apisRating()
        .addApiRating<ManagementError>(createdApi.id, rating)
        .badRequest()
        .should((response) => {
          TechnicalErrorAssertions.assertThat(response)
            .containsMessage(`Rating already exists for api [${createdApi.id}]`)
            .containsCode('rating.exists');
        });
    });
  });

  describe('Clean up', () => {
    it('Should delete the API', () => {
      gio.management(ADMIN_USER).apis().delete(createdApi.id).noContent();
    });

    it('Should disable API rating', () => {
      const settings: PortalSettings = {
        portal: {
          rating: {
            enabled: false,
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
          expect(responseSettings.portal.rating.enabled).to.be.false;
        });
    });
  });
});
