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
import "cypress-axe";

describe('Accessibility: Login page', () => {
  // Here we use a beforeEach and an after each to reset properly all the cookie we want
  // otherwise we are sometimes redirected or face XRCF issues
  beforeEach(() => {
    cy.clearCookie('Auth-Graviteeio-APIM');
    cy.visit('/');
    cy.injectAxe();
  });

  it("Check entire page for a11y issues", () => {
    cy.checkA11y();
  });

});
