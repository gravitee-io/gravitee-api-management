import { Component, ViewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { BreadcrumbComponent } from '../breadcrumb/breadcrumb.component';

interface NavItem {
  label: string;
  icon: string;
  routerLink: string;
}

@Component({
  selector: 'app-side-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, MatSidenavModule, MatListModule, MatIconModule, BreadcrumbComponent],
  template: `
    <mat-sidenav-container class="sidenav-container">
      <mat-sidenav #sidenav mode="side" opened>
        <mat-nav-list>
          @for (item of navItems; track item.routerLink) {
            <a mat-list-item [routerLink]="item.routerLink" routerLinkActive="active-link">
              <mat-icon matListItemIcon [svgIcon]="item.icon"></mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <app-breadcrumb (toggleSidebar)="sidenav.toggle()"></app-breadcrumb>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [
    `
      :host {
        display: block;
        height: 100%;
      }
      .sidenav-container {
        height: 100%;
      }
      .active-link {
        background-color: rgba(0, 0, 0, 0.05);
      }
    `,
  ],
})
export class SideNavComponent {
  @ViewChild('sidenav') sidenav!: MatSidenav;

  readonly navItems: NavItem[] = [
    { label: 'Navigation', icon: 'gio:page', routerLink: 'portal/navigation' },
    { label: 'Theme', icon: 'gio:color-picker', routerLink: 'portal/theme' },
    { label: 'Homepage', icon: 'gio:box', routerLink: 'portal/homepage' },
    { label: 'Subscription Form', icon: 'gio:list-check', routerLink: 'portal/subscription-form' },
  ];
}
