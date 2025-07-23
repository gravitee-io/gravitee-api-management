import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { GraviteeMarkdownViewerComponent } from './gravitee-markdown-viewer.component';

@NgModule({
  declarations: [],
  imports: [CommonModule, GraviteeMarkdownViewerComponent],
  exports: [GraviteeMarkdownViewerComponent],
})
export class GraviteeMarkdownViewerModule {}
