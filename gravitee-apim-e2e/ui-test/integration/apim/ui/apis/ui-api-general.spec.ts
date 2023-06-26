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

        // it('Navigate to Test (1) API', () => {
        //     cy.visit('/')
        //     HomePage.apis().click();
        //     ApiHome.searchApi().click().type("Test");
        //     cy.get('input').type('{enter}')
        //     ApiHome.testApi().click().click();
        //     cy.contains('Test (1)').should('be.visible');
        //     ApiDetails.design().should('be.visible');
        //     ApiDetails.general().should('be.visible');
        //     ApiDetails.plans().should('be.visible');
        // });
        
        it('Verify General Page Elements', () => {
            cy.visit('/#!/environments/default/apis/');
            ApiHome.searchApi().click().type("Test");
            cy.get('input').type('{enter}')
            ApiHome.testApi().click().click();
            cy.contains('Test (1)').should('be.visible');
            ApiDetails.design().should('be.visible');
            ApiDetails.general().should('be.visible');
            ApiDetails.plans().should('be.visible');
            ApiDetails.general().click();
            ApiDetails.generalName().should('be.visible');
            ApiDetails.generalVersion().should('be.visible');
            ApiDetails.generalDescription().should('be.visible');
            ApiDetails.generalLabels().should('be.visible');
            ApiDetails.generalCategories().should('be.visible');
            ApiDetails.generalCategories().click();
            ApiDetails.categories().should('be.visible');
        })

        it('Edit General Page and Verify changes', () => {
            cy.visit('/#!/environments/default/apis/');
            ApiHome.searchApi().click().type("Test");
            cy.get('input').type('{enter}')
            ApiHome.testApi().click().click();
            ApiDetails.general().click();
            ApiDetails.generalName().should('be.visible');
            ApiDetails.generalName().type(' EDIT');
            cy.wait(1000)
            ApiDetails.saveDetails().click();
            cy.contains('Configuration successfully saved!').should('be.visible');
            HomePage.apis().click();
            ApiHome.searchApi().click().type("Test");
            cy.get('input').type('{enter}');
            cy.contains('Test EDIT (1)').should('be.visible');
            cy.get('[href]').contains('Test EDIT (1)').click().click();
            // cy.contains('Test EDIT (1)').click().click();
            ApiDetails.generalName().should('be.visible');
            ApiDetails.generalName().clear().type('Test')
            cy.wait(1000)
            ApiDetails.saveDetails().click();
            cy.contains('Configuration successfully saved!').should('be.visible');
            })

    });