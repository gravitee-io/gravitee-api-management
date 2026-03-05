import { Component } from '@angular/core';
import { SideNavComponent } from '../../shared/components/side-nav/side-nav.component';

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
