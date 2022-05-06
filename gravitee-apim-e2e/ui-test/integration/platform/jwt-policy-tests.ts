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
import { sign } from 'jsonwebtoken';
import { API_PUBLISHER_USER } from '@fakers/users/users';
import { deployApi, importCreateApi, startApi } from '@commands/management/api-management-commands';
import { publishPlan } from '@commands/management/api-plan-management-commands';
import { ApiImport } from '@model/api-imports';
import { ApiFakers } from '@fakers/apis';
import { am_createApplication, am_deleteApplication } from '@commands/am_management/am_application-management-commands';
import { am_createDomain, am_deleteDomain, am_enableDomain } from '@commands/am_management/am_domain-management-commands';
import { am_getApiToken } from '@commands/am_management/am_token-management-commands';
import { AM_ADMIN_USER } from '@fakers/users/am_users';
import { Application } from '@model/applications';
import faker from '@faker-js/faker';
import { requestGateway } from 'ui-test/support/common/http.commands';

context('Create and test JWT policy', () => {
  let hs256Api: ApiImport;
  let rs256Api: ApiImport;
  let noAuthPropApi: ApiImport;
  let jwksApi: ApiImport;
  const secret = faker.random.alpha({ count: 32 });
  let am_domainHrid: string;
  let am_jwksTestApplication: Application;
  let am_apiToken: string;
  let am_domainId: string;

  before(() => {
    cy.log('-----  Create an API that can read HS256 signed access tokens  -----');
    const jwtApiImport_hs256 = ApiFakers.jwtApi({ resolverParameter: secret });
    jwtApiImport_hs256.proxy.groups[0].endpoints[0].target = 'https://api.gravitee.io/echo';
    importCreateApi(API_PUBLISHER_USER, jwtApiImport_hs256)
      .ok()
      .its('body')
      .then((api) => {
        hs256Api = api;
        publishPlan(API_PUBLISHER_USER, hs256Api.id, hs256Api.plans[0].id).ok();
      })
      .then(() => {
        deployApi(API_PUBLISHER_USER, hs256Api.id).ok();
        startApi(API_PUBLISHER_USER, hs256Api.id).noContent();
      });

    cy.log('-----  Create an API that can read RS256 signed access tokens:  -----');
    cy.readFile('cypress/fixtures/keys/jwtRS256.key.pub').then((publicKey) => {
      const jwtApiImport_rs256 = ApiFakers.jwtApi({ resolverParameter: publicKey, signature: 'RSA_RS256' });
      importCreateApi(API_PUBLISHER_USER, jwtApiImport_rs256)
        .ok()
        .its('body')
        .then((api) => {
          rs256Api = api;
          publishPlan(API_PUBLISHER_USER, rs256Api.id, rs256Api.plans[0].id).ok();
        })
        .then(() => {
          deployApi(API_PUBLISHER_USER, rs256Api.id).ok();
          startApi(API_PUBLISHER_USER, rs256Api.id).noContent();
        });
    });

    cy.log("-----  Create an API that doesn't propagate Auth header  -----");
    const jwtApiImport_noAuthProp = ApiFakers.jwtApi({ resolverParameter: secret, propagateAuthHeader: false });
    jwtApiImport_noAuthProp.proxy.groups[0].endpoints[0].target = 'https://api.gravitee.io/echo';
    importCreateApi(API_PUBLISHER_USER, jwtApiImport_noAuthProp)
      .ok()
      .its('body')
      .then((api) => {
        noAuthPropApi = api;
        publishPlan(API_PUBLISHER_USER, noAuthPropApi.id, noAuthPropApi.plans[0].id).ok();
      })
      .then(() => {
        deployApi(API_PUBLISHER_USER, noAuthPropApi.id).ok();
        startApi(API_PUBLISHER_USER, noAuthPropApi.id).noContent();
      });

    cy.log('-----  Create an application for JWKS test in AM  -----');
    const am_domainName = `${faker.random.word()}${faker.datatype.number()}-Domain`.toLowerCase();
    const am_jwksApplicationName = `noScope-application_${faker.datatype.number()}`;

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
        am_createApplication(am_apiToken, am_jwksApplicationName, am_domainId)
          .its('body')
          .then((application: Application) => {
            am_jwksTestApplication = application;

            const fakeJwksApi = ApiFakers.jwtApi({
              publicKeyResolver: 'JWKS_URL',
              resolverParameter: `${Cypress.env('am_gatewayServer')}/auth/${am_domainHrid}/oidc/.well-known/jwks.json`,
              signature: 'RSA_RS256',
            });

            cy.log('-----  Create an API for JWKS test  -----');
            importCreateApi(API_PUBLISHER_USER, fakeJwksApi)
              .ok()
              .its('body')
              .then((createdApi) => {
                jwksApi = createdApi;
                publishPlan(API_PUBLISHER_USER, jwksApi.id, jwksApi.plans[0].id).ok();
                deployApi(API_PUBLISHER_USER, jwksApi.id).ok();
                startApi(API_PUBLISHER_USER, jwksApi.id).noContent();
              });
          });
      });
  });

  after(() => {
    cy.teardownApi(hs256Api);
    cy.teardownApi(rs256Api);
    cy.teardownApi(noAuthPropApi);
    am_deleteApplication(am_apiToken, am_domainId, am_jwksTestApplication.id);
    am_deleteDomain(am_apiToken, am_domainId);
  });

  describe('JWT Policy with HS256 signature (shared secret)', () => {
    it('should successfully call API endpoint when using JWT token', () => {
      const jwtToken_hs256 = sign({ exp: 1900000000 }, secret, { noTimestamp: true });
      requestGateway({
        url: `${Cypress.env('gatewayServer')}${hs256Api.context_path}`,
        auth: { bearer: jwtToken_hs256 },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('headers');
        expect(response.body).to.have.property('query_params');
      });
    });

    it('should fail to call API endpoint without JWT token', () => {
      requestGateway(
        { url: `${Cypress.env('gatewayServer')}${hs256Api.context_path}` },
        { validWhen: (response) => response.status === 401 },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });

    it('should fail to call API endpoint when using an expired JWT token', () => {
      const jwtToken_expired = sign({ exp: 1600000000 }, secret, { noTimestamp: true });
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${hs256Api.context_path}`,
          auth: { bearer: jwtToken_expired },
        },
        {
          validWhen: (response) => response.status === 401,
        },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });

    it('should fail to call API endpoint when using invalid JWT token', () => {
      const invalidToken = sign({ exp: 1900000000 }, secret, { noTimestamp: true }).slice(0, -1);
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${hs256Api.context_path}`,
          auth: { bearer: invalidToken },
        },
        {
          validWhen: (response) => response.status === 401,
        },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });
  });

  describe('JWT Policy with RS256 signature (private/public key pair)', () => {
    it('should successfully call API endpoint when using JWT token signed with RS256', () => {
      cy.readFile('cypress/fixtures/keys/jwtRS256.key').then((privateKey) => {
        const jwtToken_rs256 = sign({ exp: 1900000000 }, privateKey, { noTimestamp: true, algorithm: 'RS256' });
        requestGateway({
          url: `${Cypress.env('gatewayServer')}${rs256Api.context_path}`,
          auth: { bearer: jwtToken_rs256 },
        }).should((response: Cypress.Response<any>) => {
          expect(response.body).to.have.property('date');
          expect(response.body).to.have.property('timestamp');
        });
      });
    });

    it('should fail to call API endpoint when JWT token signed with RS512', () => {
      cy.readFile('cypress/fixtures/keys/jwtRS256.key').then((privateKey) => {
        const jwtToken_rs512 = sign({ exp: 1900000000 }, privateKey, { noTimestamp: true, algorithm: 'RS512' });
        requestGateway(
          {
            url: `${Cypress.env('gatewayServer')}${rs256Api.context_path}`,
            auth: { bearer: jwtToken_rs512 },
          },
          {
            validWhen: (response) => response.status === 401,
          },
        )
          .its('body')
          .should('have.property', 'message')
          .and('contain', 'Unauthorized');
      });
    });

    it('should fail to call API endpoint without JWT token', () => {
      requestGateway(
        { url: `${Cypress.env('gatewayServer')}${rs256Api.context_path}` },
        { validWhen: (response) => response.status === 401 },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });

    it('should fail to call API endpoint when JWT token lacks signature', () => {
      const jwtToken_noSignature = sign({ exp: 1900000000 }, null, { noTimestamp: true, algorithm: 'none' });
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${rs256Api.context_path}`,
          auth: { bearer: jwtToken_noSignature },
        },
        {
          validWhen: (response) => response.status === 401,
        },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });
  });

  describe('JWT Policy Authorization header propagation', () => {
    it('should send Authorization header to backend API if auth header propagation is switched on', () => {
      const jwtToken_hs256 = sign({ exp: 1900000000 }, secret, { noTimestamp: true });
      requestGateway({
        url: `${Cypress.env('gatewayServer')}${hs256Api.context_path}`,
        auth: { bearer: jwtToken_hs256 },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body.headers).to.have.property('Authorization', `Bearer ${jwtToken_hs256}`);
      });
    });

    it('should not send Authorization header to backend API if auth header propagation is switched off', () => {
      const jwtToken_hs256 = sign({ exp: 1900000000 }, secret, { noTimestamp: true });
      requestGateway({
        url: `${Cypress.env('gatewayServer')}${noAuthPropApi.context_path}`,
        auth: { bearer: jwtToken_hs256 },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('query_params');
        expect(response.body.headers).to.not.have.property('Authorization');
      });
    });
  });

  describe('JWT Policy using a JWKS endpoint (here: AM server)', () => {
    let jwksToken: string;
    before(() => {
      cy.log('-----  Retrieve access token from AM server  -----');
      requestGateway({
        method: 'POST',
        url: `${Cypress.env('am_gatewayServer')}/auth/${am_domainHrid}/oauth/token`,
        form: true,
        auth: {
          username: am_jwksTestApplication.settings.oauth.client_id,
          password: am_jwksTestApplication.settings.oauth.client_secret,
        },
        body: 'grant_type=client_credentials',
      }).then((response: Cypress.Response<any>) => {
        jwksToken = response.body.access_token;
      });
    });

    it('should successfully call API endpoint', () => {
      requestGateway({
        url: `${Cypress.env('gatewayServer')}${jwksApi.context_path}`,
        auth: { bearer: jwksToken },
      }).should((response: Cypress.Response<any>) => {
        expect(response.body).to.have.property('date');
        expect(response.body).to.have.property('timestamp');
      });
    });

    it('should fail to call API endpoint without access token', () => {
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${jwksApi.context_path}`,
        },
        {
          validWhen: (response) => response.status === 401,
        },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });

    it('should fail to call API endpoint using an invalid access token', () => {
      const wrongToken = sign({ exp: 1900000000 }, secret, { noTimestamp: true });
      requestGateway(
        {
          url: `${Cypress.env('gatewayServer')}${jwksApi.context_path}`,
          auth: { bearer: wrongToken },
        },
        {
          validWhen: (response) => response.status === 401,
        },
      )
        .its('body')
        .should('have.property', 'message')
        .and('contain', 'Unauthorized');
    });
  });
});
