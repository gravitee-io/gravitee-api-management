import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GraviteeMarkdownViewerComponent } from './gravitee-markdown-viewer.component';

@NgModule({
  declarations: [GraviteeMarkdownViewerComponent],
  imports: [CommonModule],
  exports: [GraviteeMarkdownViewerComponent]
})
export class GraviteeMarkdownViewerModule { } 