import { Component, ViewEncapsulation } from '@angular/core';
import { BreadcrumbComponent } from '../../shared/components/breadcrumb/breadcrumb.component';

@Component({
    imports: [BreadcrumbComponent],
    selector: 'app-app_beta-entry',
    template: `
        <app-breadcrumb (toggleSidebar)="onToggleSidebar()"></app-breadcrumb>
        <div class="content">
            <h1>Dashboard</h1>
            <p>Welcome to App Beta.</p>
        </div>
    `,
    styles: [
        `
            :host {
                display: block;
                height: 100%;
            }
            .content {
                padding: var(--content-padding, 1.5rem);
            }
            .content h1 {
                font-size: 1.5rem;
                font-weight: 700;
                letter-spacing: -0.025em;
                color: var(--foreground);
            }
            .content p {
                margin-top: 0.5rem;
                color: var(--muted-foreground);
            }
        `,
    ],
    styleUrl: '../../styles.scss',
    encapsulation: ViewEncapsulation.None,
})
export class RemoteEntry {
    onToggleSidebar(): void {
        // No-op when sidebar is owned by Gamma host
    }
}
