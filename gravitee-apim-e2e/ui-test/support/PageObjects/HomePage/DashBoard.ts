class dashBoard {

    noOfApis(){return cy.get('.ng-binding').contains('Number of APIs')}

}

export default dashBoard;