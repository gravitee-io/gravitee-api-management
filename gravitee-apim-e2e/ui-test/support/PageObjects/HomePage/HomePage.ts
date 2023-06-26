class homePage {

    dashboard(){return cy.get('.gio-menu-item__title').contains('Dashboard')}

    apis(){return cy.get('.gio-menu-item__title').contains('APIs')}

    applications(){return cy.get('.gio-menu-item__title').contains('Applications')}

    gateways(){return cy.get('.gio-menu-item__title').contains('Gateways')}

    audit(){return cy.get('.gio-menu-item__title').contains('Audit')}

    messages(){return cy.get('.gio-menu-item__title').contains('Messages')}

    analytics(){return cy.get('.gio-menu-item__title').contains('Analytics')}

    alerts(){return cy.get('.gio-menu-item__title').contains('Alerts')}

    settings(){return cy.get('.gio-menu-item__title').contains('Settings')}

    help(){return cy.get('.btn').contains('Display contextual documentation')}

    notifications(){return cy.get('.btn').contains('Open notifications')}

    // vvv Not sure about this one, but you get the idea... 
    userMenu(){return cy.get('.btn').contains('gio-user-menu')}
    // class = .gio-user-menu__button-avatar mat-icon-button mat-button-base ???

}

export default homePage;