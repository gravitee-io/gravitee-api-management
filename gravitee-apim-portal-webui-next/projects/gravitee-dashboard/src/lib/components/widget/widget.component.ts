import { Component, Input } from '@angular/core';
import { Widget } from './widget';
import { DoughnutChartComponent } from './doughnut-chart/doughnut-chart.component';

@Component({
  selector: 'gd-widget',
  imports: [DoughnutChartComponent],
  templateUrl: './widget.component.html',
  styleUrl: './widget.component.scss',
})
export class WidgetComponent {
  @Input() item: Widget = {} as Widget;
}
