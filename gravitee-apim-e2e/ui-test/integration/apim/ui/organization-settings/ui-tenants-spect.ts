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
import faker from '@faker-js/faker';

describe('Tenants', () => {
  const name = `${faker.lorem.word(10)} ${faker.lorem.word(10)}`.substring(0, 20);
  const description = `${name} datacenter`;
  const expectedId = faker.helpers.slugify(name).toLowerCase();

  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit(`${Cypress.env('managementUI')}/#!/organization/settings/tenants`);
  });

  it('should create a tenant', () => {
    cy.getByDataTestId('add-tenant').click();
    cy.contains('Create a tenant');
    cy.getByDataTestId('tenant-name').type(name);
    cy.getByDataTestId('tenant-description').type(description);
    cy.getByDataTestId('save-tenant').click();

    cy.contains(name);
    cy.contains(description);
    cy.contains(expectedId);
  });

  it('should edit the tenant', () => {
    const updatedName = `${faker.lorem.word(10)} ${faker.lorem.word(10)}`.substring(0, 20);
    const updatedDescription = `${updatedName} datacenter`;

    cy.contains(expectedId)
      .parent()
      .within(() => cy.getByDataTestId('edit-tenant').click());

    cy.focusOnDialog().within(() => {
      cy.getByDataTestId('tenant-name').should('have.value', name);
      cy.getByDataTestId('tenant-description').should('have.value', description);
      cy.getByDataTestId('tenant-name').clear().type(updatedName);
      cy.getByDataTestId('tenant-description').clear().type(updatedDescription);
      cy.getByDataTestId('save-tenant').click();
    });

    cy.contains(updatedName);
    cy.contains(updatedDescription);
    cy.contains(expectedId);
  });

  it('should delete the tenant', () => {
    cy.contains(expectedId)
      .parent()
      .within(() => cy.getByDataTestId('delete-tenant').click());

    cy.contains('Delete a tenant');
    cy.getByDataTestId('confirm-dialog').click();

    cy.getByDataTestId('search').type(expectedId).wait(500);
    cy.contains(expectedId).should('not.exist');
  });
});
