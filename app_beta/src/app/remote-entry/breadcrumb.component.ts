import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
    selector: 'app-breadcrumb',
    standalone: true,
    imports: [MatIconModule],
    template: `
        <nav class="breadcrumb" aria-label="Breadcrumb">
            <ol>
                <li><a href="/app-beta">Home</a></li>
                <li>
                    <mat-icon class="separator">chevron_right</mat-icon>
                </li>
                <li aria-current="page">Dashboard</li>
            </ol>
        </nav>
    `,
    styles: [
        `
            .breadcrumb {
                display: flex;
                align-items: center;
                height: 3rem;
                padding: 0 1rem;
                border-bottom: 1px solid var(--border);
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
export class BreadcrumbComponent {}
