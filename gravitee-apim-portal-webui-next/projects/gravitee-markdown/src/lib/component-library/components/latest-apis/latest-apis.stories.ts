import { applicationConfig, moduleMetadata, type Meta, type StoryObj } from '@storybook/angular';
import { LatestApisComponent } from './latest-apis.component';
import { ApiSearchFactory } from '../../../services/api-search.factory';
import { GRAVITEE_MARKDOWN_BASE_URL, GRAVITEE_MARKDOWN_MOCK_MODE } from '../../../services/configuration';
import { provideHttpClient } from '@angular/common/http';
import { GRAVITEE_MONACO_EDITOR_CONFIG } from '../../../gravitee-monaco-wrapper/data/gravitee-monaco-editor-config';

const meta: Meta<LatestApisComponent> = {
  title: 'Components/Latest APIs',
  component: LatestApisComponent,
  parameters: {
    layout: 'padded',
  },
  tags: ['autodocs'],
  argTypes: {
    title: {
      control: 'text',
      description: 'The main title for the component',
    },
    subtitle: {
      control: 'text',
      description: 'Optional subtitle text',
    },
    maxApis: {
      control: { type: 'number', min: 1, max: 20 },
      description: 'Maximum number of APIs to display',
    },
    category: {
      control: { type: 'select' },
      options: ['all', 'featured', 'payment', 'analytics', 'storage'],
      description: 'Category filter for APIs',
    },
    searchQuery: {
      control: 'text',
      description: 'Search query for filtering APIs',
    },
    cardElevation: {
      control: { type: 'select' },
      options: [0, 1, 2, 3, 4, 5],
      description: 'Card shadow elevation level',
    },
    cardBackgroundColor: {
      control: 'color',
      description: 'Custom background color for cards',
    },
    cardBorderRadius: {
      control: 'text',
      description: 'Custom border radius for cards',
    },
    actionButtonText: {
      control: 'text',
      description: 'Text for the action button',
    },
    actionButtonVariant: {
      control: { type: 'select' },
      options: ['filled', 'outlined', 'text'],
      description: 'Button style variant',
    },
    actionButtonType: {
      control: { type: 'select' },
      options: ['internal', 'external'],
      description: 'Button link type',
    },
  },
  decorators: [
    moduleMetadata({
        imports: [LatestApisComponent],
    }),
    applicationConfig({
        providers: [
          {
            provide: GRAVITEE_MONACO_EDITOR_CONFIG,
            useValue: {
              baseUrl: '..',
              theme: 'vs' as const,
            },
          },
          {
            provide: GRAVITEE_MARKDOWN_BASE_URL,
            useValue: 'https://gravitee.io',
          },
          {
            provide: GRAVITEE_MARKDOWN_MOCK_MODE,
            useValue: true,
          },
          provideHttpClient(),
        ],
      }),
  ],
};

export default meta;
type Story = StoryObj<LatestApisComponent>;

export const Default: Story = {
  args: {
    title: 'Latest APIs',
    maxApis: 5,
    category: 'all',
    cardElevation: 0,
    actionButtonText: 'View Details',
    actionButtonVariant: 'outlined',
    actionButtonType: 'internal',
  },
};

export const Featured: Story = {
  args: {
    title: 'Featured APIs',
    subtitle: 'Discover our most popular APIs',
    maxApis: 4,
    category: 'featured',
    cardElevation: 0,
    actionButtonText: 'Explore API',
    actionButtonVariant: 'outlined',
  },
};

export const SearchResults: Story = {
  args: {
    title: 'Search Results',
    subtitle: 'APIs matching your search criteria',
    maxApis: 6,
    searchQuery: 'payment',
    cardElevation: 0,
    actionButtonText: 'View API',
    actionButtonVariant: 'outlined',
  },
};

export const Minimal: Story = {
  args: {
    title: 'Available APIs',
    maxApis: 3,
    cardElevation: 0,
    actionButtonVariant: 'outlined',
    actionButtonText: 'Learn More',
  },
};

export const CustomStyled: Story = {
  args: {
    title: 'Custom Styled APIs',
    subtitle: 'With custom card styling',
    maxApis: 4,
    cardElevation: 0,
    cardBackgroundColor: '#f8f9fa',
    cardBorderRadius: '12px',
    actionButtonText: 'Explore API',
    actionButtonVariant: 'outlined',
  },
};

export const ExternalLinks: Story = {
  args: {
    title: 'External APIs',
    subtitle: 'APIs with external documentation',
    maxApis: 3,
    actionButtonText: 'Visit Documentation',
    actionButtonType: 'external',
    actionButtonVariant: 'outlined',
  },
};

export const LargeGrid: Story = {
  args: {
    title: 'All APIs',
    maxApis: 8,
    cardElevation: 0,
    actionButtonText: 'View Details',
  },
  parameters: {
    layout: 'fullscreen',
  },
};

export const AnalyticsCategory: Story = {
  args: {
    title: 'Analytics APIs',
    subtitle: 'Business intelligence and data visualization',
    maxApis: 4,
    category: 'analytics',
    cardElevation: 0,
    actionButtonText: 'View Analytics',
    actionButtonVariant: 'outlined',
  },
};

export const PaymentCategory: Story = {
  args: {
    title: 'Payment APIs',
    subtitle: 'Secure payment processing solutions',
    maxApis: 3,
    category: 'payment',
    cardElevation: 0,
    actionButtonText: 'Integrate Payment',
    actionButtonVariant: 'outlined',
  },
};

export const StorageCategory: Story = {
  args: {
    title: 'Storage APIs',
    subtitle: 'Cloud storage and file management',
    maxApis: 2,
    category: 'storage',
    cardElevation: 0,
    actionButtonText: 'Access Storage',
    actionButtonVariant: 'outlined',
  },
};

export const NoTitle: Story = {
  args: {
    title: '',
    maxApis: 4,
    cardElevation: 0,
    actionButtonText: 'View Details',
  },
};

export const LoadingState: Story = {
  args: {
    title: 'Loading APIs',
    maxApis: 5,
  },
  parameters: {
    docs: {
      description: {
        story: 'This story shows the loading state of the component. In a real scenario, this would be visible while the APIs are being fetched.',
      },
    },
  },
};

export const ErrorState: Story = {
  args: {
    title: 'Error Loading APIs',
    maxApis: 5,
  },
  parameters: {
    docs: {
      description: {
        story: 'This story shows the error state of the component. In a real scenario, this would be visible when there is an error fetching the APIs.',
      },
    },
  },
};

export const EmptyState: Story = {
  args: {
    title: 'No APIs Found',
    maxApis: 5,
    searchQuery: 'nonexistent',
  },
  parameters: {
    docs: {
      description: {
        story: 'This story shows the empty state of the component when no APIs match the search criteria.',
      },
    },
  },
};

export const ResponsiveGrid: Story = {
  args: {
    title: 'Responsive API Grid',
    subtitle: 'This grid adapts to different screen sizes',
    maxApis: 6,
    cardElevation: 0,
    actionButtonText: 'View Details',
  },
  parameters: {
    viewport: {
      defaultViewport: 'responsive',
    },
  },
};

export const DifferentButtonVariants: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 32px;">
        <div>
          <h3>Filled Buttons</h3>
          <app-latest-apis 
            title="Filled Buttons" 
            [maxApis]="2" 
            actionButtonVariant="filled"
            actionButtonText="View Details">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>Outlined Buttons (Default)</h3>
          <app-latest-apis 
            title="Outlined Buttons" 
            [maxApis]="2" 
            actionButtonVariant="outlined"
            actionButtonText="Explore API">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>Text Buttons</h3>
          <app-latest-apis 
            title="Text Buttons" 
            [maxApis]="2" 
            actionButtonVariant="text"
            actionButtonText="Learn More">
          </app-latest-apis>
        </div>
      </div>
    `,
    imports: [LatestApisComponent],
  }),
  parameters: {
    docs: {
      description: {
        story: 'This story demonstrates different button variants available in the latest-apis component.',
      },
    },
  },
};

export const DifferentCardElevations: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 32px;">
        <div>
          <h3>No Elevation (0) - Default</h3>
          <app-latest-apis 
            title="No Elevation" 
            [maxApis]="2" 
            [cardElevation]="0">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>Low Elevation (1)</h3>
          <app-latest-apis 
            title="Low Elevation" 
            [maxApis]="2" 
            [cardElevation]="1">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>Medium Elevation (2)</h3>
          <app-latest-apis 
            title="Medium Elevation" 
            [maxApis]="2" 
            [cardElevation]="2">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>High Elevation (3)</h3>
          <app-latest-apis 
            title="High Elevation" 
            [maxApis]="2" 
            [cardElevation]="3">
          </app-latest-apis>
        </div>
      </div>
    `,
    imports: [LatestApisComponent],
  }),
  parameters: {
    docs: {
      description: {
        story: 'This story demonstrates different card elevation levels available in the latest-apis component.',
      },
    },
  },
};

export const CustomColors: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 32px;">
        <div>
          <h3>Light Background</h3>
          <app-latest-apis 
            title="Light Background" 
            [maxApis]="2" 
            cardBackgroundColor="#f8f9fa"
            cardBorderRadius="12px">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>Dark Background</h3>
          <app-latest-apis 
            title="Dark Background" 
            [maxApis]="2" 
            cardBackgroundColor="#343a40"
            cardBorderRadius="8px">
          </app-latest-apis>
        </div>
        
        <div>
          <h3>Custom Border Radius</h3>
          <app-latest-apis 
            title="Custom Border Radius" 
            [maxApis]="2" 
            cardBorderRadius="20px">
          </app-latest-apis>
        </div>
      </div>
    `,
    imports: [LatestApisComponent],
  }),
  parameters: {
    docs: {
      description: {
        story: 'This story demonstrates custom styling options for the latest-apis component cards.',
      },
    },
  },
}; 