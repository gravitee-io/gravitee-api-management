import { Component } from '@angular/core';
import {
  SubscriptionsDetailsComponent
} from "../../api/api-details/api-tab-subscriptions/subscriptions-details/subscriptions-details.component";

@Component({
  selector: 'app-subscription-details-legacy',
  imports: [
    SubscriptionsDetailsComponent
  ],
  templateUrl: './subscription-details-legacy.component.html',
  styleUrl: './subscription-details-legacy.component.scss',
})
export default class SubscriptionDetailsLegacyComponent {

}
