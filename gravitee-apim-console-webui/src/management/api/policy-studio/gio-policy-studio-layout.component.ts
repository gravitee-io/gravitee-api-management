import { Component } from '@angular/core';

@Component({
  selector: 'gio-policy-studio-layout',
  template: require('./gio-policy-studio-layout.component.html'),
  styles: [require('./gio-policy-studio-layout.component.scss')],
})
export class GioPolicyStudioLayoutComponent {
  policyStudioMenu = [
    { label: 'Design', uiSref: 'design', params: { psPage: 'design' } },
    { label: 'Config', uiSref: 'design', params: { psPage: 'settings' } },
    { label: 'Properties', uiSref: 'design', params: { psPage: 'properties' } },
    { label: 'Resources', uiSref: 'design', params: { psPage: 'resources' } },
    { label: 'Debug', uiSref: 'debug' },
  ];
  activeLink = this.policyStudioMenu[0];
}
