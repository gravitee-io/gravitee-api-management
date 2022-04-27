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
import faker from 'faker';

describe('Users', () => {
  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit(`${Cypress.env('managementUI')}/#!/organization/settings/users`);
  });

  it('should create a new user', () => {
    const firstName = faker.name.firstName();
    const lastName = faker.name.lastName();
    const email = faker.internet.email(firstName, lastName);

    cy.getByDataTestId('add-user').click();
    cy.contains('Pre-register a user');

    cy.getByDataTestId('firstName').type(firstName);
    cy.getByDataTestId('lastName').type(lastName);
    cy.getByDataTestId('email').type(email);

    // FIXME:
    // Clicking in the save bar -> need test id on Particles components
    cy.contains('Create').click();

    cy.contains('Users');

    // FIXME:
    // Search isn't working with numbers or special characters for now, @Yann will handle it
    // cy.getByDataTestId('search').type(email).wait(500);

    cy.contains(firstName);
    cy.contains(lastName);
    cy.contains(email)
      .parent()
      .within(() => cy.getByDataTestId('delete-user').click());

    cy.get('mat-dialog-actions').within(() => {
      cy.contains('Delete').click();
    });

    cy.contains(email).should('not.exist');
  });

  it('should create a new service account user', () => {
    const serviceAccountName = faker.commerce.productName();
    const email = faker.internet.email(serviceAccountName);

    cy.getByDataTestId('add-user').click();
    cy.contains('Pre-register a user');

    cy.contains('Service Account').click();

    // FIXME:
    // Search isn't working with numbers or special characters for now, @Yann will handle it
    // cy.getByDataTestId('search').type(email).wait(500);

    cy.getByDataTestId('lastName').type(serviceAccountName);
    cy.getByDataTestId('email').type(email);

    cy.contains('Create').click();

    cy.contains('Users');
    cy.contains(serviceAccountName);
    cy.contains(email)
      .parent()
      .within(() => cy.getByDataTestId('delete-user').click());

    cy.get('mat-dialog-actions').within(() => {
      cy.contains('Delete').click();
    });

    cy.contains(email).should('not.exist');
  });
});
