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

describe('Get Gateway instance information as admin', () => {
  beforeEach(() => {
    cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
    cy.visit('/#!/environments/DEFAULT/instances/');
    cy.url().should('contain', 'instances');
  });

  it('should display all important UI elements', function () {
    cy.wait(1000);
    cy.get('h1').contains('API Gateway').should('be.visible');
    cy.getByDataTestId('instances_search-gateway-textfield').should((textfield) => {
      expect(textfield).to.have.attr('placeholder', 'Search Gateway instances...');
      expect(textfield).to.have.attr('type', 'text');
      expect(textfield).to.have.class('ng-empty');
    });
    cy.getByDataTestId('instances_instances-box').should((instancesBox) => {
      expect(instancesBox).to.have.length.greaterThan(0);
      expect(instancesBox[0]).to.contain('STARTED');
    });
  });

  it('should redirect to instance overview showing information of selected gateway', () => {
    cy.getByDataTestId('instances_instances-box').first().trigger('click');
    cy.url().should('contain', 'environment');
    cy.contains('Information');
    cy.contains('Plugins');
    cy.contains('System properties');
  });

  it('should display monitoring information of selected gateway', () => {
    cy.getByDataTestId('instances_instances-box').first().find('a').trigger('click');
    cy.getByDataTestId('instances-detail-monitoring').trigger('click');
    cy.getByDataTestId('instance-monitoring_jvm-box').contains('JVM').should('be.visible');
    cy.getByDataTestId('instance-monitoring_cpu-box').scrollIntoView().contains('CPU').should('be.visible');
    cy.getByDataTestId('instance-monitoring_process-box').contains('Process').should('be.visible');
    cy.getByDataTestId('instance-monitoring_thread-box').scrollIntoView().contains('Thread').should('be.visible');
    cy.getByDataTestId('instance-monitoring_gc-box').contains('Garbage collector').should('be.visible');
    cy.url().should('contain', 'monitoring');
  });
});

describe('Get Gateway instance information as non-admin', () => {
  beforeEach(() => {
    cy.loginInAPIM(API_PUBLISHER_USER.username, API_PUBLISHER_USER.password);
  });

  it('should not be able to call gateway instances', function () {
    cy.visit('/#!/environments/DEFAULT/instances/');
    cy.wait(1000);
    cy.url().should('not.contain', 'instances');
  });
});
