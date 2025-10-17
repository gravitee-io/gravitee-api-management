import { Component, Input } from '@angular/core';
import { Widget } from './widget';
import { GridsterItemComponent } from 'angular-gridster2';

@Component({
  selector: 'gd-widget',
  imports: [GridsterItemComponent],
  templateUrl: './widget.component.html',
  styleUrl: './widget.component.scss',
})
export class WidgetComponent {
  @Input() item: Widget = {} as Widget;
}
