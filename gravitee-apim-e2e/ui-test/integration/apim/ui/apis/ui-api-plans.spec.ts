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
        
        it('Verify Existing Plan', () => {
            cy.visit('/#!/environments/default/apis/?q=Test&page=1&size=10');
            cy.getByDataTestId('search').click().type("Test");
            cy.get('input').type('{enter}')
            cy.wait(1000)
            cy.getByDataTestId('api_list_edit_button').first().click();
            cy.contains('Design').should('be.visible');
            cy.contains('General').should('be.visible');
            cy.contains('Plans').should('be.visible');
            ApiDetails.plans().click();
            cy.contains('STAGING').should('be.visible');
            cy.contains('PUBLISHED').should('be.visible');
            cy.contains('DEPRECATED').should('be.visible');
            cy.contains('CLOSED').should('be.visible');
            cy.contains('Default Keyless').should('be.visible')
            // I don't know if this is liable to change ... I am assuming we set up and look for / use same API every time.
        })

        it('Create a generic New Plan (API Key), verify and delete', () => {
            cy.visit('/#!/environments/default/apis/?q=Test&page=1&size=10');
            cy.getByDataTestId('api_list_edit_button').first().click();
            ApiDetails.plans().click();
            cy.contains('STAGING').should('be.visible');
            cy.contains('PUBLISHED').should('be.visible');
            cy.contains('DEPRECATED').should('be.visible');
            cy.contains('CLOSED').should('be.visible');

            cy.getByDataTestId('api_addPlan_button').click();
            cy.contains('API Key').click();

            cy.getByDataTestId('api_planName').type('TestApiKey')
            cy.getByDataTestId('api_planDescription').type('Testy Test McTest')
            // ^ ^ Could probably do with being faker'd
            cy.getByDataTestId('planNext').click()
            cy.contains('selection rule').should('be.visible')
            cy.getByDataTestId('planNext').click()
            cy.contains('Rate Limiting').should('be.visible')
            cy.contains('Quota').should('be.visible')
            cy.contains('Resource Filtering').should('be.visible')
            cy.contains('Create').as('btn').click()
            cy.contains('Configuration successfully saved!').should('be.visible');
            cy.contains('STAGING').as('btn').click()
            cy.contains('TestApiKey').should('be.visible')
            // ^ would need readjusting after faker
            cy.getByDataTestId('apiPlanClosePlan').first().click()
            cy.wait(1000)
            cy.get('[placeholder="TestApiKey"]').type('TestApiKey')
            cy.contains('Yes, delete this plan').as('btn').click()
            cy.contains('The plan TestApiKey has been closed with success.').should('be.visible')
            cy.wait(500)
            cy.contains('TestApiKey').should('not.exist')
        }),

        it('Create a generic New Plan (OAuth2), verify and delete', () => {
            cy.visit('/#!/environments/default/apis/?q=Test&page=1&size=10');
            cy.getByDataTestId('api_list_edit_button').first().click();
            ApiDetails.plans().click();
            cy.contains('STAGING').should('be.visible');
            cy.contains('PUBLISHED').should('be.visible');
            cy.contains('DEPRECATED').should('be.visible');
            cy.contains('CLOSED').should('be.visible');

            cy.getByDataTestId('api_addPlan_button').click();
            cy.contains('OAuth2').click();

            cy.getByDataTestId('api_planName').type('TestOAuth2')
            cy.getByDataTestId('api_planDescription').type('Testy Test McTest')
            // ^ ^ Could probably do with being faker'd
            cy.getByDataTestId('planNext').click()
            // cy.get('[role="combobox"]').contains('OAuth2 resource').type('Test')
            cy.get('#mat-input-7').type('Test')
            // ^ I can't find html element for this, this doesn't feel good but was best I could do as OAuth2 Resource a mandatory field 
            cy.contains('selection rule').should('be.visible')
            cy.getByDataTestId('planNext').click()
            cy.contains('Rate Limiting').should('be.visible')
            cy.contains('Quota').should('be.visible')
            cy.contains('Resource Filtering').should('be.visible')
            cy.contains('Create').as('btn').click()
            cy.contains('Configuration successfully saved!').should('be.visible');
            cy.contains('STAGING').as('btn').click()
            cy.contains('TestOAuth2').should('be.visible')
            // ^ would need readjusting after faker
            cy.getByDataTestId('apiPlanClosePlan').first().click()
            cy.wait(1000)
            cy.get('[placeholder="TestOAuth2"]').type('TestOAuth2')
            cy.contains('Yes, close this plan').as('btn').click()
            cy.contains('The plan TestOAuth2 has been closed with success.').should('be.visible')
            cy.wait(500)
            cy.contains('TestOAuth2').should('not.exist')
        }),

        it('Create a generic New Plan (JWT), verify and delete', () => {
            cy.visit('/#!/environments/default/apis/?q=Test&page=1&size=10');
            cy.getByDataTestId('api_list_edit_button').first().click();
            ApiDetails.plans().click();
            cy.contains('STAGING').should('be.visible');
            cy.contains('PUBLISHED').should('be.visible');
            cy.contains('DEPRECATED').should('be.visible');
            cy.contains('CLOSED').should('be.visible');

            cy.getByDataTestId('api_addPlan_button').click();
            cy.contains('JWT').click();

            cy.getByDataTestId('api_planName').type('TestJWT')
            cy.getByDataTestId('api_planDescription').type('Testy Test McTest')
            // ^ ^ Could probably do with being faker'd
            cy.getByDataTestId('planNext').click()
            cy.contains('selection rule').should('be.visible')
            cy.getByDataTestId('planNext').click()
            cy.contains('Rate Limiting').should('be.visible')
            cy.contains('Quota').should('be.visible')
            cy.contains('Resource Filtering').should('be.visible')
            cy.contains('Create').as('btn').click()
            cy.contains('Configuration successfully saved!').should('be.visible');
            cy.contains('STAGING').as('btn').click()
            cy.contains('TestJWT').should('be.visible')
            // ^ would need readjusting after faker
            cy.getByDataTestId('apiPlanClosePlan').first().click()
            cy.wait(1000)
            cy.get('[placeholder="TestJWT"]').type('TestJWT')
            cy.contains('Yes, close this plan').as('btn').click()
            cy.contains('The plan TestJWT has been closed with success.').should('be.visible')
            cy.wait(500)
            cy.contains('TestJWT').should('not.exist')
        }),

        it('Create a generic New Plan (Keyless), verify and delete', () => {
            cy.visit('/#!/environments/default/apis/?q=Test&page=1&size=10');
            cy.getByDataTestId('api_list_edit_button').first().click();
            ApiDetails.plans().click();
            cy.contains('STAGING').should('be.visible');
            cy.contains('PUBLISHED').should('be.visible');
            cy.contains('DEPRECATED').should('be.visible');
            cy.contains('CLOSED').should('be.visible');

            cy.getByDataTestId('api_addPlan_button').click();
            cy.contains('Keyless (public)').click();

            cy.getByDataTestId('api_planName').type('TestKeyless')
            cy.getByDataTestId('api_planDescription').type('Testy Test McTest')
            // ^ ^ Could probably do with being faker'd
            cy.getByDataTestId('planNext').click()
            cy.contains('Rate Limiting').should('be.visible')
            cy.contains('Quota').should('be.visible')
            cy.contains('Resource Filtering').should('be.visible')
            cy.contains('Create').as('btn').click()
            cy.contains('Configuration successfully saved!').should('be.visible');
            cy.contains('STAGING').as('btn').click()
            cy.contains('TestKeyless').should('be.visible')
            // ^ would need readjusting after faker
            cy.getByDataTestId('apiPlanClosePlan').first().click()
            cy.wait(1000)
            cy.get('[placeholder="TestKeyless"]').type('TestKeyless')
            cy.contains('Yes, close this plan').as('btn').click()
            cy.contains('The plan TestKeyless has been closed with success.').should('be.visible')
            cy.wait(500)
            cy.contains('TestKeyless').should('not.exist')
        })
    });