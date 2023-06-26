import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { ApisFaker } from '@gravitee/fixtures/management/ApisFaker';
import { PlansFaker } from '@gravitee/fixtures/management/PlansFaker';
import { ApiEntity, PlanStatus } from '../../../../../lib/management-webclient-sdk/src/lib/models';
import faker from '@faker-js/faker';

import homePage from "@pageobjects/HomePage/HomePage"
import apiHome from "@pageobjects/Apis/ApiHome"
import apiDetails from '@pageobjects/Apis/ApiDetails';

// This is purely for PageObject example and on hold for now until policy studio changes are completed.


    describe('API Plans Feature', () => {
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

        it('Navigate to Test (1) API', () => {
        cy.visit('/')
        HomePage.apis().click();
        ApiHome.searchApi().click().type("Test");
        ApiHome.testApi().click().click();
        cy.contains('Test (1)').should('be.visible');
        ApiDetails.design().should('be.visible');
        ApiDetails.general().should('be.visible');
        ApiDetails.plans().should('be.visible');
        });
        
        it('Verify Existing Plan', () => {
        cy.visit('/#!/environments/default/apis/?q=Test&page=1&size=10');
        ApiHome.testApi().click().click();
        ApiDetails.plans().click();
        cy.contains('STAGING').should('be.visible');
        cy.contains('PUBLISHED').should('be.visible');
        cy.contains('DEPRECATED').should('be.visible');
        cy.contains('CLOSED').should('be.visible');
        })
    });