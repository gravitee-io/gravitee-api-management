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
import { ADMIN_USER } from '@fakers/users/users';
import { ApiFakers } from '@fakers/apis';
import { Api } from '@model/apis';
import { gio } from '@commands/gravitee.commands';

describe('API List feature', () => {
  let createdApi: Api;
  before(() => {
    gio
      .management(ADMIN_USER)
      .apis()
      .create(ApiFakers.api())
      .created()
      .then((response) => {
        createdApi = response.body;
        cy.log('Created api id:', createdApi.id);
      });
  });

  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
  });

  it(`Visit Home board`, () => {
    cy.visit(Cypress.env('managementUI'));
    cy.contains('Home board');
  });

  it(`Visit Search Apis`, () => {
    cy.visit(`${Cypress.env('managementUI')}/#!/environments/DEFAULT/apis/`);
    cy.contains(createdApi.name);
  });

  after(() => {
    gio.management(ADMIN_USER).apis().delete(createdApi.id).noContent();
  });
});
