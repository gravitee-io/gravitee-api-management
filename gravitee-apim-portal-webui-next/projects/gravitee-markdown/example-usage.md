# Example Usage of Gravitee Markdown with Dynamic Components

This example demonstrates how to integrate `ngx-dynamic-hooks` with the `GraviteeMarkdownViewerComponent` to render custom Angular components within markdown content.

## Setup

1. **Install the library**:
   ```bash
   npm install @gravitee/gravitee-markdown
   ```

2. **Import the components and services**:
   ```typescript
   import { 
     GraviteeMarkdownViewerComponent,
     GraviteeMarkdownViewerRegistryService,
     CopyCodeComponent
   } from '@gravitee/gravitee-markdown';
   ```

3. **Register dynamic components**:
   ```typescript
   @Component({
     selector: 'app-documentation',
     template: `
       <gravitee-markdown-viewer
         [content]="documentationContent"
         [darkTheme]="false"
       ></gravitee-markdown-viewer>
     `,
     imports: [GraviteeMarkdownViewerComponent],
     standalone: true
   })
   export class DocumentationComponent implements OnInit {
     documentationContent = `
   # API Documentation
   
   Here's how to make a request:
   
   <app-copy-code text="curl -X GET https://api.example.com/v1/users"></app-copy-code>
   
   ## Response Format
   
   The API returns JSON responses.
     `;
   
     constructor(private registryService: GraviteeMarkdownViewerRegistryService) {}
   
     ngOnInit() {
       // Register the copy-code component
       this.registryService.registerComponent({
         selector: 'app-copy-code',
         component: CopyCodeComponent
       });
     }
   }
   ```

## How It Works

1. **Component Registration**: The `GraviteeMarkdownViewerRegistryService` maintains a registry of components that can be dynamically rendered.

2. **Markdown Processing**: When markdown content is processed, the service looks for custom HTML tags that match registered component selectors.

3. **Dynamic Rendering**: The `ngx-dynamic-hooks` library handles the actual rendering of the components within the markdown content.

4. **Component Integration**: Registered components are rendered with their inputs populated from the HTML attributes.

## Supported Features

- ✅ **Custom Components**: Register any Angular component for dynamic rendering
- ✅ **Input Binding**: Pass inputs to components via HTML attributes
- ✅ **Pure HTML**: Regular HTML content is also rendered
- ✅ **Markdown Syntax**: Full markdown support with syntax highlighting
- ✅ **Dark Theme**: Toggle between light and dark themes
- ✅ **Standalone Components**: All components are standalone for easy integration

## Example Components

### Copy Code Component
```typescript
@Component({
  selector: 'app-copy-code',
  standalone: true,
  template: `
    <div class="copy-code-container">
      <div class="code-content">
        <pre><code>{{ text() }}</code></pre>
      </div>
      <button class="copy-button" (click)="copyToClipboard()">
        Copy
      </button>
    </div>
  `
})
export class CopyCodeComponent {
  text = input<string>('');
  
  copyToClipboard(): void {
    navigator.clipboard.writeText(this.text());
  }
}
```

### Custom Alert Component
```typescript
@Component({
  selector: 'app-alert',
  standalone: true,
  template: `
    <div class="alert alert-{{ type() }}">
      <h4>{{ title() }}</h4>
      <p>{{ message() }}</p>
    </div>
  `
})
export class AlertComponent {
  type = input<'info' | 'warning' | 'error'>('info');
  title = input<string>('');
  message = input<string>('');
}
```

## Usage in Markdown

```markdown
# Documentation

<app-alert type="info" title="Note" message="This is an informational message"></app-alert>

Here's some code you can copy:

<app-copy-code text="npm install @gravitee/gravitee-markdown"></app-copy-code>

<app-alert type="warning" title="Warning" message="Make sure to register components before using them"></app-alert>
```

## Advanced Usage

### Multiple Component Registration
```typescript
ngOnInit() {
  this.registryService.registerComponents([
    {
      selector: 'app-copy-code',
      component: CopyCodeComponent
    },
    {
      selector: 'app-alert',
      component: AlertComponent
    },
    {
      selector: 'app-code-block',
      component: CodeBlockComponent
    }
  ]);
}
```

### Default Input Values
```typescript
this.registryService.registerComponent({
  selector: 'app-copy-code',
  component: CopyCodeComponent,
  inputs: {
    text: 'default code here'
  }
});
```

This integration provides a powerful way to extend markdown content with interactive Angular components while maintaining the simplicity of markdown syntax. 