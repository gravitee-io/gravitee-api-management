import { NgModule } from '@angular/core';
import { RuntimeAlertCreateComponent } from './runtime-alert-create.component';
import { CommonModule } from '@angular/common';
import { GioGoBackButtonModule } from '../../../shared/components/gio-go-back-button/gio-go-back-button.module';
import { ReactiveFormsModule } from '@angular/forms';

@NgModule({
  declarations: [RuntimeAlertCreateComponent],
  exports: [RuntimeAlertCreateComponent],
  imports: [CommonModule, ReactiveFormsModule, GioGoBackButtonModule],
})
export class RuntimeAlertCreateModule {}
