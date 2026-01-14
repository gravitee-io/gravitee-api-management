import { Component } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AnalyticsTemplateDialogComponent } from './analytics-template-dialog/analytics-template-dialog.component';

export interface Dashboard {
  id: string;
  name: string;
  createdAt: number;
  lastModificationAt: number;
  labels: string[];
}

const ELEMENT_DATA: Dashboard[] = [
  { id: '1', name: 'Global Traffic', createdAt: 1672531200000, lastModificationAt: 1672617600000, labels: ['traffic', 'http'] },
  { id: '2', name: 'Error Analysis', createdAt: 1672704000000, lastModificationAt: 1672790400000, labels: ['errors', '5xx', '4xx'] },
  { id: '3', name: 'Latency Report', createdAt: 1672876800000, lastModificationAt: 1672963200000, labels: ['latency', 'performance'] },
];

@Component({
  selector: 'analytics-overview',
  templateUrl: './analytics-overview.component.html',
  styleUrl: './analytics-overview.component.scss',
  standalone: false,
})
export class AnalyticsOverviewComponent {
  displayedColumns: string[] = ['name', 'createdAt', 'lastModificationAt', 'labels', 'actions'];
  dataSource = ELEMENT_DATA;

  constructor(private readonly dialog: MatDialog) { }

  public onFromTemplateClick(): void {
    const dialogRef = this.dialog.open(AnalyticsTemplateDialogComponent, {
      width: '80vw',
      maxWidth: '80vw',
      height: '80vh',
      maxHeight: '80vh',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // eslint-disable-next-line no-console
        console.log('Selected template:', result);
      }
    });
  }
}
