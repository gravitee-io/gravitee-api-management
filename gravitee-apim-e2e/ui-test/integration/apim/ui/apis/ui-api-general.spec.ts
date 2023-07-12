import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { ApiEntity, PlanStatus } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import faker from '@faker-js/faker';

import homePage from "@pageobjects/HomePage/HomePage"
import apiHome from "@pageobjects/Apis/ApiHome"
import apiDetails from '@pageobjects/Apis/ApiDetails';

// This is purely for PageObject example and on hold for now until policy studio changes are completed.


    describe('API General Page', () => {
        beforeEach(() => {
          cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
        });

        const HomePage = new homePage()
        const ApiHome = new apiHome()
        const ApiDetails = new apiDetails()

        it('Verify Home Page', () => {
            cy.visit('/')
            cy.wait(1000)
            cy.contains('Home board').should('be.visible');
        });
        
        it('Verify General Page Elements', () => {
            cy.visit('/#!/environments/default/apis/');
            cy.getByDataTestId('search').click().type("Test")
            cy.get('input').type('{enter}')
            cy.wait(1000)
            cy.getByDataTestId('api_list_edit_button').first().click();
            // cy.contains('Test (1)').should('be.visible');
            cy.contains('Design').should('be.visible');
            cy.contains('General').should('be.visible');
            cy.contains('Plans').should('be.visible');
            // ApiDetails.general().click();
            // ^ I cannot pinpoint the html element for this button.
            cy.getByDataTestId('generalName').should('be.visible');
            cy.getByDataTestId('generalVersion').should('be.visible');
            cy.getByDataTestId('generalDescription').should('be.visible');
            cy.getByDataTestId('generalLabels').should('be.visible');
            cy.getByDataTestId('generalCategories').should('be.visible');
            cy.getByDataTestId('generalCategories').click();
            cy.getByDataTestId('categoryList').should('be.visible');
        })

        it('Edit General Page and Verify changes', () => {
            cy.visit('/#!/environments/default/apis/');
            cy.getByDataTestId('search').click().type("Test");
            cy.get('input').type('{enter}')
            cy.wait(1000)
            cy.getByDataTestId('api_list_edit_button').first().click();
            ApiDetails.general().click();
            // ^ I cannot pinpoint the html element for this button. 
            // // ^ Page dynamically generated, therefore, use page object. 
            cy.getByDataTestId('generalName').should('be.visible');
            cy.getByDataTestId('generalName').type(' EDIT');
            cy.getByDataTestId('generalVersion').clear().type('1');
            cy.wait(500)
            // ^ ideally need to waitFor apiGeneralSaveBar, need to figure out correct command. 
            cy.getByDataTestId('apiGeneralSaveBar').contains('Save').click();
            cy.contains('Configuration successfully saved!').should('be.visible');
            cy.visit('/#!/environments/default/apis/');
            cy.getByDataTestId('search').type("EDIT");
            cy.contains('EDIT (1)').should('be.visible');
            // ^ for now, test stops here to verify edited api until i figure out faker to generate a random api name

            // cy.get('[href]').contains('EDIT (1)').click().click();
            // cy.getByDataTestId('generalName').should('be.visible');
            // cy.getByDataTestId('generalName').clear().type('Test');
            // // ^ Could probably do with faker here to generate random name so this test is able to execute over and over.
            // cy.wait(1000)
            // // ApiDetails.saveDetails().click();
            // cy.getByDataTestId('apiGeneralSaveDetails').click();
            // cy.contains('Configuration successfully saved!').should('be.visible');
            })

    });