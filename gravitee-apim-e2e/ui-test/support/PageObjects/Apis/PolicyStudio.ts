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

export default class PolicyStudio {
  static openPolicyStudio(apiId: string, isV4: boolean) {
    cy.visit(`/#!/default/apis/${apiId}${isV4 ? '/v4/' : '/v2/'}policy-studio`);
    cy.url().should('include', '/policy-studio');
    cy.contains('.list__flowsGroup__header__label', 'Common flows', { timeout: 60000 });
    return new PolicyStudio();
  }
  addCommonFlow() {
    cy.contains('.list__flowsGroup__header', 'Common flows').find('button').click();
    return this;
  }

  clickOnSaveButton() {
    cy.contains('button', 'Save').click();
    return this;
  }

  clickOnSaveButtonInDialog() {
    cy.get('[role="dialog"]').contains('button', 'Save').click();
    return this;
  }

  deployApiUsingUi(deployLabel?: string) {
    cy.contains('button', 'Deploy API')
      .click()
      .then(() => {
        if (deployLabel) {
          cy.get('input').click().type(deployLabel);
        }
      });
    cy.contains('button', /^Deploy$/).click();
    cy.contains('API successfully deployed');
  }

  enterName(flowName: string) {
    cy.get('[formcontrolname="name"]').click().clear().type(flowName);
    return this;
  }

  clickOnCreateButton() {
    cy.contains('button', 'Create').click();
    return this;
  }

  addResponsePhasePolicy() {
    cy.contains('gio-ps-flow-details-phase', 'Response phase').find('button').first().click();
    return this;
  }

  choosePolicy(policyName: string) {
    cy.contains('.policiesCatalog__list__policy', policyName).find('button').click();
    return this;
  }

  addOrUpdateHeaders(key: string, value: string) {
    cy.get('textarea[formcontrolname="key"]').click().clear().type(key);
    cy.get('textarea[formcontrolname="value"]').first().click().clear().type(value);
    return this;
  }

  clickOnAddPolicyButton() {
    cy.contains('button', 'Add policy').click();
    return this;
  }

  editFlowDetails(flowName: string) {
    cy.contains('.list__flowsGroup__flows__flow__left__name', flowName, { timeout: 60000 }).should('be.visible').click();
    cy.get('.header__configBtn__edit').click();
    return this;
  }

  setPath(flowPath: string) {
    cy.get('[formcontrolname="path"]').click().clear().type(flowPath);
    return this;
  }
}
