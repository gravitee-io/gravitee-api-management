import { Component } from '@angular/core';
import { GraviteeMarkdownViewerComponent } from 'gravitee-markdown';
import { InnerLinkDirective } from '../../directives/inner-link.directive';

@Component({
  selector: 'app-homepage',
  imports: [GraviteeMarkdownViewerComponent, InnerLinkDirective],
  templateUrl: './homepage.component.html',
  styleUrl: './homepage.component.scss'
})
export class HomepageComponent {

  content = `<app-card centered="true">

  # Welcome to the Developer Portal
  Access all the APIs, documentation, and tools to build your next integration.

  <card-actions style="justify-content: center;">
      <app-button href="/catalog">Explore all APIs</app-button>
      <app-button href="/guides" variant="outlined">Get started</app-button>
  </card-actions>
</app-card>

<app-image
        src="https://media.istockphoto.com/id/1361595256/photo/front-view-of-a-workspace-modern-computer-blank-mockup-screen.jpg?s=612x612&w=0&k=20&c=hhPAxfzQ1Y7Wk6ZcTuxDPYlFSIMzgaCeSASZtP46CRw=" alt="Banner image" centered="true" maxWidth="100%" maxHeight="100%"
/>

<app-grid columns="3">  
  <app-grid-cell>

  ### API catalog
  Browse and test all available APIs in one place.

  </app-grid-cell>
  <app-grid-cell>

  ### Interactive docs
  Explore clear, structured documentation with code samples.

  </app-grid-cell>
  <app-grid-cell>

  ### Usage analytics
  Track API usage, error rates, and performance metrics.

  </app-grid-cell>
  <app-grid-cell>

  ### Secure access
  Use API keys or OAuth2 to manage access and credentials.

  </app-grid-cell>
  <app-grid-cell>

  ### Support & help
  Get help through chat, documentation, or our dev community.

  </app-grid-cell>
  <app-grid-cell>

  ### SDKs & tools
  Download SDKs, Postman collections, and CLI tools.

  </app-grid-cell>
</app-grid>


# Discover the latest APIs
content here


# See what you can build

<app-grid columns="3">
  <app-grid-cell>

  ### Payment integration

  <app-image src="https://media.istockphoto.com/id/1361595256/photo/front-view-of-a-workspace-modern-computer-blank-mockup-screen.jpg?s=612x612&w=0&k=20&c=hhPAxfzQ1Y7Wk6ZcTuxDPYlFSIMzgaCeSASZtP46CRw=" alt="User avatar" rounded="full" maxWidth="100px" maxHeight="100px"></app-image>
  
  Use API keys or OAuth2 to manage access and credentials.

  </app-grid-cell>
  <app-grid-cell>

  ### Custom dashboards
  Get help through chat, documentation, or our dev community.

  </app-grid-cell>
  <app-grid-cell>

  ### Notification systems
  Download SDKs, Postman collections, and CLI tools.

  </app-grid-cell>
</app-grid>


<app-card title="What will you build?" centered="true">

  You’re one step away from discovering everything our APIs can do.
  <card-actions style="justify-content: center;">
      <app-button href="/catalog">Explore all APIs</app-button>
  </card-actions>

</app-card>

# Common questions

<app-grid columns="3">

<app-grid-cell>

### How do I get my API key?
Go to your profile settings and create a new API key. Follow the auth guide for details.

</app-grid-cell>
<app-grid-cell>

### What are the rate limits?
Default limits are 1,000 requests per minute. Specific limits vary by API—see docs for details.

</app-grid-cell>
<app-grid-cell>

### Can I test APIs without real data?
Yes, use sandbox mode to run test calls with mock data. No impact on production systems.

</app-grid-cell>
<app-grid-cell>

### How do I access logs or analytics?
Visit the Analytics tab for usage stats and error logs. Filter by API, date, or status code.

</app-grid-cell>
<app-grid-cell>

### Where can I find SDKs?
SDKs are listed on each API’s page. You can also find global tools in the “Resources” section.

</app-grid-cell>
<app-grid-cell>

### How do I report a bug or ask for help?
Open a ticket from the Support tab or join our dev community for faster responses.
    
</app-grid-cell>

</app-grid>
  `;

}
