import { Component } from '@angular/core';
import { SideNavComponent } from './side-nav.component';

@Component({
    imports: [SideNavComponent],
    selector: 'app-app_beta-entry',
    template: `<app-side-nav></app-side-nav>`,
    styles: [
        `
            :host {
                display: block;
                height: 100%;
            }
        `,
    ],
})
export class RemoteEntry {}
