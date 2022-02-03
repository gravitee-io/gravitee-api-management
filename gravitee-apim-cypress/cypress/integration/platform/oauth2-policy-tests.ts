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

import {
  am_createApplication,
  am_deleteApplication,
  am_patchApplication,
} from '@commands/am_management/am_application-management-commands';
import { am_createDomain, am_deleteDomain, am_enableDomain } from '@commands/am_management/am_domain-management-commands';
import { am_getApiToken } from '@commands/am_management/am_token-management-commands';
import { deleteApi, deployApi, importCreateApi, startApi, stopApi } from '@commands/management/api-management-commands';
import { closePlan, publishPlan } from '@commands/management/api-plan-management-commands';
import { AM_ADMIN_USER } from '@fakers/users/am_users';
import { API_PUBLISHER_USER } from '@fakers/users/users';
import { Application } from '@model/am_applications';
import * as faker from 'faker';
import { requestGateway } from 'support/common/http.commands';
import { ApiFakers } from '@fakers/apis';
const jwt = require('jsonwebtoken');

context('Testing OAuth2 policy', () => {
  let am_apiToken: string;
  let am_domainId: string;
  let am_domainHrid: string;
  let noScopeApi;
  let oneScopeApi;
  let expiredTokenApi;
  let noScopeToken: string;
  let tokenWithScope: string;
  let expiredToken: string;
  const am_domainName = `${faker.random.word()}${faker.datatype.number()}-Domain`.toLowerCase();
  const am_noScopeApplicationName = `noScope-application_${faker.datatype.number()}`;
  const am_expiredTokenApplicationName = `expiredToken-application_${faker.datatype.number()}`;
  const am_oneScopeApplicationName = `oneScope-application_${faker.datatype.number()}`;
  let am_noScopeApplication: Application;
  let am_oneScopeApplication: Application;
  let am_expiredTokenApplication: Application;

  before(() => {
    am_getApiToken(AM_ADMIN_USER)
      .then((response) => (am_apiToken = response.body.access_token))
      .then(() => am_createDomain(am_apiToken, am_domainName))
      .created()
      .then((response) => {
        am_domainId = response.body.id;
        am_domainHrid = response.body.hrid;
      })
      .then(() => am_enableDomain(am_apiToken, am_domainId))
      .ok()
      .then(() => {
        am_createApplication(am_apiToken, am_noScopeApplicationName, am_domainId)
          .its('body')
          .then((application: Application) => {
            am_noScopeApplication = application;
            const oauth2Api = ApiFakers.oauth2Api(
              am_domainHrid,
              application.settings.oauth.clientId,
              application.settings.oauth.clientSecret,
            );
            importCreateApi(API_PUBLISHER_USER, oauth2Api)
              .ok()
              .its('body')
              .then((createdApi) => {
                noScopeApi = createdApi;
                publishPlan(API_PUBLISHER_USER, noScopeApi.id, noScopeApi.plans[0].id).ok();
                deployApi(API_PUBLISHER_USER, noScopeApi.id).ok();
                startApi(API_PUBLISHER_USER, noScopeApi.id).noContent();
              });
          });

        am_createApplication(am_apiToken, am_oneScopeApplicationName, am_domainId)
          .its('body')
          .then((application: Application) => {
            am_oneScopeApplication = application;
            const newTokenScopeSetting = {
              settings: {
                oauth: {
                  enhanceScopesWithUserPermissions: false,
                  scopeSettings: [
                    {
                      scope: 'scim',
                      defaultScope: true,
                    },
                  ],
                },
              },
            };
            am_patchApplication(am_apiToken, am_domainId, am_oneScopeApplication.id, newTokenScopeSetting).ok();
            const oauthConfig = { checkRequiredScopes: true, requiredScopes: ['scim'] };
            const oauth2Api = ApiFakers.oauth2Api(
              am_domainHrid,
              application.settings.oauth.clientId,
              application.settings.oauth.clientSecret,
              oauthConfig,
            );
            importCreateApi(API_PUBLISHER_USER, oauth2Api)
              .ok()
              .its('body')
              .then((createdApi) => {
                oneScopeApi = createdApi;
                publishPlan(API_PUBLISHER_USER, oneScopeApi.id, oneScopeApi.plans[0].id).ok();
                deployApi(API_PUBLISHER_USER, oneScopeApi.id).ok();
                startApi(API_PUBLISHER_USER, oneScopeApi.id).noContent();
              });
          });

        am_createApplication(am_apiToken, am_expiredTokenApplicationName, am_domainId)
          .its('body')
          .then((application: Application) => {
            am_expiredTokenApplication = application;
            const newTokenValiditySetting = {
              settings: {
                oauth: {
                  tokenCustomClaims: [],
                  accessTokenValiditySeconds: 1,
                  refreshTokenValiditySeconds: 14400,
                  idTokenValiditySeconds: 14400,
                },
              },
            };
            am_patchApplication(am_apiToken, am_domainId, am_expiredTokenApplication.id, newTokenValiditySetting).ok();

            const oauth2Api = ApiFakers.oauth2Api(
              am_domainHrid,
              application.settings.oauth.clientId,
              application.settings.oauth.clientSecret,
            );
            importCreateApi(API_PUBLISHER_USER, oauth2Api)
              .ok()
              .its('body')
              .then((createdApi) => {
                expiredTokenApi = createdApi;
                publishPlan(API_PUBLISHER_USER, expiredTokenApi.id, expiredTokenApi.plans[0].id).ok();
                deployApi(API_PUBLISHER_USER, expiredTokenApi.id).ok();
                startApi(API_PUBLISHER_USER, expiredTokenApi.id).noContent();
              });
          });
      });
  });

  describe('General access token tests', () => {
    before(() => {
      requestGateway({
        method: 'POST',
        url: `${Cypress.env('am_gatewayServer')}/auth/${am_domainHrid}/oauth/token`,
        form: true,
        auth: {
          username: am_noScopeApplication.settings.oauth.clientId,
          password: am_noScopeApplication.settings.oauth.clientSecret,
        },
        body: 'grant_type=client_credentials',
      }).then((response: Cypress.Response<any>) => {
        noScopeToken = response.body.access_token;
      });
    });

    it('should successfully call API endpoint when using access token', () => {
      requestGateway({
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${noScopeApi.context_path}`,
        auth: { bearer: noScopeToken },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('date');
        expect(response.body).to.have.property('timestamp');
      });
    });

    it('should fail to call API endpoint without using access token', () => {
      requestGateway(
        {
          method: 'GET',
          url: `${Cypress.env('gatewayServer')}${noScopeApi.context_path}`,
        },
        {
          validWhen: (response) => {
            return response.status === 401;
          },
        },
      ).should((response: Cypress.Response<any>) => {
        expect(response.headers)
          .to.have.property('www-authenticate')
          .and.to.include('error="invalid_request", error_description="No OAuth authorization header was supplied');
      });
    });

    it('should fail to call API endpoint using an invalid access token', () => {
      requestGateway(
        {
          method: 'GET',
          url: `${Cypress.env('gatewayServer')}${noScopeApi.context_path}`,
          auth: { bearer: 'invalid_token' },
        },
        {
          validWhen: (response) => {
            return response.status === 401;
          },
        },
      ).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('error', 'Invalid Access Token');
      });
    });
  });

  describe('Access token with scope(s)', () => {
    before(() => {
      requestGateway(
        {
          method: 'POST',
          url: `${Cypress.env('am_gatewayServer')}/auth/${am_domainHrid}/oauth/token`,
          form: true,
          auth: {
            username: am_oneScopeApplication.settings.oauth.clientId,
            password: am_oneScopeApplication.settings.oauth.clientSecret,
          },
          body: 'grant_type=client_credentials',
        },
        {
          validWhen: (response) => {
            const decodedJwtPayload = jwt.decode(response.body.access_token);
            return decodedJwtPayload && decodedJwtPayload.scope === 'scim';
          },
        },
      ).then((response: Cypress.Response<any>) => {
        tokenWithScope = response.body.access_token;
      });
    });

    it('should successfully call API when access token contains a scope that is not required in APIM (non-strict)', () => {
      requestGateway({
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${noScopeApi.context_path}`,
        auth: { bearer: tokenWithScope },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('date');
        expect(response.body).to.have.property('timestamp');
      });
    });

    it('should successfully call API when scope of access token matches the configured scopes in APIM (strict mode)', () => {
      requestGateway({
        method: 'GET',
        url: `${Cypress.env('gatewayServer')}${oneScopeApi.context_path}`,
        auth: { bearer: tokenWithScope },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('date');
        expect(response.body).to.have.property('timestamp');
      });
    });

    it("should fail to call API when scope of access token doesn't contain scope that is configured in APIM (strict mode)", () => {
      requestGateway(
        {
          method: 'GET',
          url: `${Cypress.env('gatewayServer')}${oneScopeApi.context_path}`,
          auth: { bearer: noScopeToken },
        },
        {
          validWhen: (response) => {
            return response.status === 401;
          },
        },
      ).should((response: Cypress.Response<any>) => {
        expect(response.headers)
          .to.have.property('www-authenticate')
          .and.to.include(
            'error="insufficient_scope", error_description="The request requires higher privileges than provided by the access token."',
          );
      });
    });
  });

  describe('Expired access token', () => {
    before(() => {
      requestGateway(
        {
          method: 'POST',
          url: `${Cypress.env('am_gatewayServer')}/auth/${am_domainHrid}/oauth/token`,
          form: true,
          auth: {
            username: am_expiredTokenApplication.settings.oauth.clientId,
            password: am_expiredTokenApplication.settings.oauth.clientSecret,
          },
          body: 'grant_type=client_credentials',
        },
        {
          validWhen: (response) => {
            return response.body.expires_in <= 1;
          },
        },
      ).then((response: Cypress.Response<any>) => {
        expiredToken = response.body.access_token;
      });
    });

    it('should fail to call API endpoint with an expired JWT access token', () => {
      requestGateway(
        {
          method: 'GET',
          url: `${Cypress.env('gatewayServer')}${expiredTokenApi.context_path}`,
          auth: { bearer: expiredToken },
        },
        {
          validWhen: (response) => {
            return response.status === 401;
          },
        },
      ).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('error', 'Invalid Access Token');
      });
    });
  });

  after(() => {
    am_deleteApplication(am_apiToken, am_domainId, am_noScopeApplication.id);
    am_deleteApplication(am_apiToken, am_domainId, am_oneScopeApplication.id);
    am_deleteApplication(am_apiToken, am_domainId, am_expiredTokenApplication.id);

    am_deleteDomain(am_apiToken, am_domainId);

    closePlan(API_PUBLISHER_USER, noScopeApi.id, noScopeApi.plans[0].id).ok();
    closePlan(API_PUBLISHER_USER, oneScopeApi.id, oneScopeApi.plans[0].id).ok();
    closePlan(API_PUBLISHER_USER, expiredTokenApi.id, expiredTokenApi.plans[0].id).ok();

    stopApi(API_PUBLISHER_USER, noScopeApi.id).noContent();
    stopApi(API_PUBLISHER_USER, oneScopeApi.id).noContent();
    stopApi(API_PUBLISHER_USER, expiredTokenApi.id).noContent();

    deleteApi(API_PUBLISHER_USER, noScopeApi.id).noContent();
    deleteApi(API_PUBLISHER_USER, oneScopeApi.id).noContent();
    deleteApi(API_PUBLISHER_USER, expiredTokenApi.id).noContent();
  });
});
