import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-deployment-compare-current',
  templateUrl: './api-history-v4-deployment-compare.component.html',
  styleUrls: ['./api-history-v4-deployment-compare.component.scss'],
})
export class ApiHistoryV4DeploymentCompareComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA)
    public data: {
      left: { apiDefinition: string; version: string };
      right: { apiDefinition: string; version: string };
    },
  ) {}
}
