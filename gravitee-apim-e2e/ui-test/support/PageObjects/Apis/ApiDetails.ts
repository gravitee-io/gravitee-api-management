// gio-submenu-item__title and gio-submenu-item for Design

class apiDetails {

    design(){return cy.get('.gio-submenu-item').contains('Design')}

    general(){return cy.get('.gio-submenu-item__title').contains('General')}

    plans(){return cy.get('.gio-submenu-item__title').contains('Plans')}

    // General

    generalName(){return cy.get('[formcontrolname="name"]')}

    generalVersion(){return cy.get('[formcontrolname="version"]')}
    
    generalDescription(){return cy.get('[formcontrolname="description"]')}

    generalLabels(){return cy.get('[formcontrolname="labels"]')}

    generalCategories(){return cy.get('[formcontrolname="categories"]')}

    categories(){return cy.get('.mat-option-text')} 
    // above Step Def would write as ApiDetails.categories().contains('[CATEGORY]').click()

    saveDetails(){return cy.get('[type="submit"]').contains('Save')}


    // Plans

}

export default apiDetails;