import { Component, EventEmitter, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatIconButton } from '@angular/material/button';

@Component({
    selector: 'app-breadcrumb',
    standalone: true,
    imports: [RouterLink, MatIconModule, MatIconButton],
    template: `
        <nav class="breadcrumb" aria-label="Breadcrumb">
            <button mat-icon-button (click)="toggleSidebar.emit()" aria-label="Toggle sidebar">
                <mat-icon svgIcon="gio:sidebar"></mat-icon>
            </button>
            <span class="divider"></span>
            <ol>
                <li><a routerLink="/">Home</a></li>
                <li>
                    <mat-icon class="separator" svgIcon="gio:nav-arrow-right"></mat-icon>
                </li>
                <li aria-current="page">Portal</li>
            </ol>
        </nav>
    `,
    styles: [
        `
            .breadcrumb {
                display: flex;
                align-items: center;
                height: 3rem;
                padding: 0 0.25rem;
                border-bottom: 1px solid var(--border);
                gap: 0.25rem;
            }
            button {
                --mdc-icon-button-state-layer-size: 1.75rem;
                --mdc-icon-button-icon-size: 1rem;
            }
            .divider {
                width: 1px;
                height: 1rem;
                background-color: var(--border);
                flex-shrink: 0;
            }
            ol {
                display: flex;
                align-items: center;
                gap: 0.375rem;
                list-style: none;
                margin: 0;
                padding: 0;
                font-size: 0.875rem;
            }
            li {
                display: flex;
                align-items: center;
                color: var(--foreground);
            }
            a {
                color: var(--muted-foreground);
                text-decoration: none;
                transition: color 0.15s;
            }
            a:hover {
                color: var(--foreground);
            }
            .separator {
                font-size: 0.875rem;
                width: 0.875rem;
                height: 0.875rem;
                color: var(--muted-foreground);
            }
        `,
    ],
})
export class BreadcrumbComponent {
    @Output() toggleSidebar = new EventEmitter<void>();
}
