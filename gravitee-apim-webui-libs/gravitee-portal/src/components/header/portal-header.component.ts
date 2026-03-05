import { Component, inject, InjectionToken, InputSignal, input } from '@angular/core';
import { GioBannerModule } from '@gravitee/ui-particles-angular';

export const PORTAL_TECH_PREVIEW_MESSAGE = new InjectionToken<string>('PORTAL_TECH_PREVIEW_MESSAGE', {
  providedIn: 'root',
  factory: () => 'This feature is in tech preview and may change in future releases.',
});

@Component({
  selector: 'portal-header',
  imports: [GioBannerModule],
  templateUrl: './portal-header.component.html',
  styleUrl: './portal-header.component.scss',
})
export class PortalHeaderComponent {
  title: InputSignal<string> = input.required();
  subtitle: InputSignal<string> = input('');
  showTechPreviewMessage: InputSignal<boolean> = input(true);
  showActions: InputSignal<boolean> = input(false);
  techPreviewMessage = inject(PORTAL_TECH_PREVIEW_MESSAGE);
}
