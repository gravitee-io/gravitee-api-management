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

describe('Login Feature', () => {
  // Here we use a beforeEach and an after each to reset properly all the cookie we want
  // otherwise we are sometimes redirected or face XRCF issues
  beforeEach(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.visit('/');
  });

  it(`should launch the login page`, () => {
    cy.url().should('contain', 'login');
    cy.get('.card__header__title').should('be.visible');
    cy.get('.card__header__title').contains('Sign In');
  });

  it(`should be able to login`, () => {
    cy.getByDataTestId('username-input').type(ADMIN_USER.username);
    cy.getByDataTestId('password-input').type(ADMIN_USER.password);

    cy.getByDataTestId('sign-in-button').click();
    cy.url().should('contain', '/home/overview');
    cy.contains('Overview').should('be.visible');
    cy.contains('API Health Check').should('be.visible');
    cy.contains('Tasks').should('be.visible');
    cy.contains('h2', 'API Events').should('be.visible');
  });
});
