import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { GioPermissionDirective } from './gio-permission.directive';

@NgModule({
  imports: [CommonModule],
  declarations: [GioPermissionDirective],
  exports: [GioPermissionDirective],
})
export class GioPermissionModule {}
