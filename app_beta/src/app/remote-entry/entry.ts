import { Component, ViewEncapsulation } from '@angular/core';
import { SideNavComponent } from '../../shared/components/side-nav/side-nav.component';

@Component({
  imports: [SideNavComponent],
  selector: 'app-app_beta-entry',
  template: `<app-side-nav></app-side-nav>`,
  styleUrl: '../../styles.scss',
  encapsulation: ViewEncapsulation.None,
})
export class RemoteEntry {}
