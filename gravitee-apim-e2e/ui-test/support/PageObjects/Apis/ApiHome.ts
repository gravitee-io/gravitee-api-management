class apiHome {

    addApi(){return cy.get('.mat-button-wrapper').contains('Add API')}

    searchApi(){return cy.get('[data-testid="search"]')}

    testApi(){return cy.get('[href]').contains('Test (1)')}

}

export default apiHome;