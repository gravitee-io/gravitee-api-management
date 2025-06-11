import { Component } from '@angular/core';
import { MatCard, MatCardContent, MatCardHeader, MatCardSubtitle, MatCardTitle } from "@angular/material/card";

@Component({
  selector: 'runtime-alert-history',
  imports: [
    MatCard,
    MatCardHeader,
    MatCardSubtitle,
    MatCardTitle,
    MatCardContent
  ],
  templateUrl: './runtime-alert-history.component.html',
  styleUrl: './runtime-alert-history.component.scss'
})
export class RuntimeAlertHistoryComponent {

}
