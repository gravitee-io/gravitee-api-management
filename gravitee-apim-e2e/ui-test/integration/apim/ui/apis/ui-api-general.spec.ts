import { ADMIN_USER, API_PUBLISHER_USER } from '@fakers/users/users';
import { MAPIV2ApisFaker } from '../../../../../lib/fixtures/management/MAPIV2ApisFaker';
import { ApiV4 } from '../../../../../lib/management-v2-webclient-sdk/src/lib';
import faker from '@faker-js/faker';
import apiDetails from '@pageobjects/Apis/ApiDetails';


    describe('API General Page functionality', () => {
        beforeEach(() => {
          cy.loginInAPIM(ADMIN_USER.username, ADMIN_USER.password);
          cy.visit('/#!/environments/default/apis/');
          cy.url().should('include', '/apis/');
        });

        before(() => {
            v4api = [];
      
            Cypress._.times(noOfApis, () => {
              cy.log('Create v4 API');
              cy.request({
                method: 'POST',
                url: `${Cypress.env('managementApi')}/management/v2/environments/DEFAULT/apis`,
                auth: { username: API_PUBLISHER_USER.username, password: API_PUBLISHER_USER.password },
                body: MAPIV2ApisFaker.newApi({
                  listeners: [
                    MAPIV2ApisFaker.newSubscriptionListener({
                      entrypoints: [
                        {
                          type: 'webhook',
                        },
                      ],
                    }),
                  ],
                  endpointGroups: [
                    {
                      name: 'default-group',
                      type: 'mock',
                      endpoints: [
                        {
                          name: 'default',
                          type: 'mock',
                          configuration: {
                            messageInterval: 0,
                          },
                        },
                      ],
                    },
                  ],
                }),
              }).then((response) => {
                expect(response.status).to.eq(201);
                v4api.push(response.body);
              });
            });
          });

        const ApiDetails = new apiDetails()
        const apiName = faker.commerce.productName();
        const apiVersion = faker.datatype.number();
        const apiDescription = faker.commerce.productDescription();
        let v4api: ApiV4[];
        const noOfApis = 1;
                 
// Above may not be correct as struggling to resolve import ApiV4 module but it runs perfect with pre-created api through ui
        
        it('Verify General Page Elements', () => {
            cy.getByDataTestId('api_list_edit_button', { timeout: 5000 }).first().click();
            cy.contains('Policy Studio').should('be.visible');
            cy.contains('Info').should('be.visible');
            cy.contains('Plans').should('be.visible');
            ApiDetails.info().click();
            cy.getByDataTestId('api_general_namefield').should('be.visible');
            cy.getByDataTestId('api_general_versionfield').should('be.visible');
            cy.getByDataTestId('api_general_descriptionfield').should('be.visible');
            cy.getByDataTestId('api_general_labelsfield').should('be.visible');
            cy.getByDataTestId('api_general_categoriesdropdown').should('be.visible').click();
            cy.getByDataTestId('api_general_categorieslist-unavailable').should('be.visible');
        })

        it('Edit General Page and Verify changes', () => {
            cy.getByDataTestId('api_list_edit_button').first().click();
            ApiDetails.info().click();
        //  ^^ Sidebar dynamically generated, therefore, use page object.
            cy.getByDataTestId('api_general_namefield').should('be.visible');
            cy.getByDataTestId('api_general_namefield').clear().type(`${apiName}`);
            cy.getByDataTestId('api_general_versionfield').clear().type(`${apiVersion}`);
            cy.getByDataTestId('api_general_descriptionfield').clear().type(`${apiDescription}`);

            cy.getByDataTestId('api_general_savebar', { timeout: 5000 }).should('be.visible').contains('Save').click();
            cy.contains('Configuration successfully saved!').should('be.visible');
            cy.visit('/#!/environments/default/apis/');
            cy.contains(`${apiName}`).should('be.visible');
            })

        // Could possibly do with danger zone buttons but left out until proper end to end - 
        // - tests can be completed and I know it is finished and expected consequences 

    });