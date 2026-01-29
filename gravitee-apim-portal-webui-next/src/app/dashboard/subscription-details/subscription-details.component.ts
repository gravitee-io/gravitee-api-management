import {Component, inject} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {MatProgressBar} from "@angular/material/progress-bar";
import {MatIcon} from "@angular/material/icon";
import {MatButton} from "@angular/material/button";

@Component({
  selector: 'app-subscription-details',
  imports: [
    MatProgressBar,
    MatIcon,
    MatButton,
  ],
  templateUrl: './subscription-details.component.html',
  styleUrl: './subscription-details.component.scss',
})
export default class SubscriptionDetailsComponent {
  private route = inject(ActivatedRoute);

  // Get the ID from the URL
  subscriptionId = this.route.snapshot.paramMap.get('subscriptionId');
}
