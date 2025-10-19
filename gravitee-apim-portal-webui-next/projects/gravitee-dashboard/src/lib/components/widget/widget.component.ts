import { Component, input } from '@angular/core';
import { Widget, WidgetType } from './widget';
import { DoughnutChartComponent } from './doughnut-chart/doughnut-chart.component';

@Component({
  selector: 'gd-widget',
  imports: [DoughnutChartComponent],
  templateUrl: './widget.component.html',
  styleUrl: './widget.component.scss',
})
export class WidgetComponent {
  item = input.required<Widget>();
  type = input<WidgetType>();
}
