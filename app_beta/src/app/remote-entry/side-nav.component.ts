import { Component } from '@angular/core';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { BreadcrumbComponent } from './breadcrumb.component';

@Component({
    selector: 'app-side-nav',
    standalone: true,
    imports: [MatSidenavModule, MatListModule, MatIconModule, BreadcrumbComponent],
    template: `
        <mat-sidenav-container class="sidenav-container">
            <mat-sidenav mode="side" opened>
                <mat-nav-list>
                    <a mat-list-item>
                        <mat-icon matListItemIcon svgIcon="gio:home"></mat-icon>
                        <span matListItemTitle>Dashboard</span>
                    </a>
                    <a mat-list-item>
                        <mat-icon matListItemIcon svgIcon="gio:cloud-server"></mat-icon>
                        <span matListItemTitle>APIs</span>
                    </a>
                    <a mat-list-item>
                        <mat-icon matListItemIcon svgIcon="gio:settings"></mat-icon>
                        <span matListItemTitle>Settings</span>
                    </a>
                </mat-nav-list>
            </mat-sidenav>
            <mat-sidenav-content>
                <app-breadcrumb></app-breadcrumb>
                <div class="content">
                    <h1>Dashboard</h1>
                    <p>Welcome to App Beta.</p>
                </div>
            </mat-sidenav-content>
        </mat-sidenav-container>
    `,
    styles: [
        `
            .sidenav-container {
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
})
export class SideNavComponent {}
