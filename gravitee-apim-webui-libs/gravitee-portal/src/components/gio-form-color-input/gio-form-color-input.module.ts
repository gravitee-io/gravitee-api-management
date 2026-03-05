import { A11yModule } from '@angular/cdk/a11y';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatRippleModule } from '@angular/material/core';
import { MatInputModule } from '@angular/material/input';

import { GioFormColorInputComponent } from './gio-form-color-input.component';

@NgModule({
  imports: [CommonModule, A11yModule, MatInputModule, ReactiveFormsModule, MatRippleModule],
  declarations: [GioFormColorInputComponent],
  exports: [GioFormColorInputComponent],
})
export class GioFormColorInputModule {}
