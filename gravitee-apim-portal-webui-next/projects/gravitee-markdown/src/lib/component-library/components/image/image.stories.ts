import type { Meta, StoryObj } from '@storybook/angular';
import { ImageComponent } from './image.component';

const meta: Meta<ImageComponent> = {
  title: 'Components/Image',
  component: ImageComponent,
  parameters: {
    layout: 'centered',
  },
  tags: ['autodocs'],
  argTypes: {
    src: {
      control: 'text',
      description: 'The source URL of the image',
    },
    alt: {
      control: 'text',
      description: 'Alternative text for accessibility',
    },
    centered: {
      control: 'boolean',
      description: 'Whether the image should be centered',
    },
    rounded: {
      control: { type: 'select' },
      options: ['none', 'sm', 'md', 'lg', 'full'],
      description: 'Border radius for the image corners',
    },
    maxWidth: {
      control: 'text',
      description: 'Maximum width of the image (e.g., "300px", "50%")',
    },
    maxHeight: {
      control: 'text',
      description: 'Maximum height of the image (e.g., "200px", "100vh")',
    },
    width: {
      control: 'text',
      description: 'Fixed width of the image',
    },
    height: {
      control: 'text',
      description: 'Fixed height of the image',
    },
  },
};

export default meta;
type Story = StoryObj<ImageComponent>;

export const Default: Story = {
  args: {
    src: 'https://picsum.photos/400/300',
    alt: 'Random placeholder image',
  },
  render: (args) => ({
    props: {
      ...args,
      generateCode: () => {
        const attributes: string[] = [];
        
        if (args.src) {
          attributes.push(`src="${args.src}"`);
        }
        if (args.alt) {
          attributes.push(`alt="${args.alt}"`);
        }
        if (args.centered) {
          attributes.push('centered');
        }
        if (args.rounded && args.rounded !== 'none') {
          attributes.push(`rounded="${args.rounded}"`);
        }
        if (args.maxWidth) {
          attributes.push(`maxWidth="${args.maxWidth}"`);
        }
        if (args.maxHeight) {
          attributes.push(`maxHeight="${args.maxHeight}"`);
        }
        if (args.width) {
          attributes.push(`width="${args.width}"`);
        }
        if (args.height) {
          attributes.push(`height="${args.height}"`);
        }
        
        const attributesStr = attributes.length > 0 ? ' ' + attributes.join(' ') : '';
        return `<app-image${attributesStr}></app-image>`;
      }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 20px; align-items: center;">
        <app-image 
          [src]="src" 
          [alt]="alt"
          [centered]="centered"
          [rounded]="rounded"
          [maxWidth]="maxWidth"
          [maxHeight]="maxHeight"
          [width]="width"
          [height]="height"
        ></app-image>
        
        <div style="margin-top: 20px; padding: 16px; background: #f5f5f5; border-radius: 8px; font-family: monospace; font-size: 14px; max-width: 600px; width: 100%;">
          <div style="margin-bottom: 8px; font-weight: bold; color: #333;">Generated Code:</div>
          <pre style="margin: 0; white-space: pre-wrap; word-break: break-all;">{{ generateCode() }}</pre>
        </div>
      </div>
    `,
  }),
};

export const Centered: Story = {
  args: {
    src: 'https://picsum.photos/400/300',
    alt: 'Centered image',
    centered: true,
  },
};

export const RoundedSmall: Story = {
  args: {
    src: 'https://picsum.photos/400/300',
    alt: 'Image with small rounded corners',
    rounded: 'sm',
  },
};

export const RoundedMedium: Story = {
  args: {
    src: 'https://picsum.photos/400/300',
    alt: 'Image with medium rounded corners',
    rounded: 'md',
  },
};

export const RoundedLarge: Story = {
  args: {
    src: 'https://picsum.photos/400/300',
    alt: 'Image with large rounded corners',
    rounded: 'lg',
  },
};

export const Circular: Story = {
  args: {
    src: 'https://picsum.photos/200/200',
    alt: 'Circular image',
    rounded: 'full',
    maxWidth: '200px',
    maxHeight: '200px',
  },
};

export const CustomSize: Story = {
  args: {
    src: 'https://picsum.photos/600/400',
    alt: 'Image with custom size constraints',
    maxWidth: '400px',
    maxHeight: '300px',
  },
};

export const FixedSize: Story = {
  args: {
    src: 'https://picsum.photos/300/200',
    alt: 'Image with fixed dimensions',
    width: '300px',
    height: '200px',
  },
};

export const Responsive: Story = {
  args: {
    src: 'https://picsum.photos/800/600',
    alt: 'Responsive image',
    maxWidth: '100%',
    maxHeight: '400px',
  },
};

export const Avatar: Story = {
  args: {
    src: 'https://picsum.photos/100/100',
    alt: 'User avatar',
    rounded: 'full',
    maxWidth: '100px',
    maxHeight: '100px',
  },
};

export const Banner: Story = {
  args: {
    src: 'https://picsum.photos/1200/300',
    alt: 'Banner image',
    centered: true,
    maxWidth: '100%',
    maxHeight: '300px',
  },
};

export const Thumbnail: Story = {
  args: {
    src: 'https://picsum.photos/150/150',
    alt: 'Thumbnail',
    rounded: 'sm',
    maxWidth: '150px',
    maxHeight: '150px',
  },
};

export const AllRoundedVariants: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-image src="https://picsum.photos/150/150" alt="No rounded corners" rounded="none" maxWidth="150px" maxHeight="150px"></app-image>
        <app-image src="https://picsum.photos/150/150" alt="Small rounded corners" rounded="sm" maxWidth="150px" maxHeight="150px"></app-image>
        <app-image src="https://picsum.photos/150/150" alt="Medium rounded corners" rounded="md" maxWidth="150px" maxHeight="150px"></app-image>
        <app-image src="https://picsum.photos/150/150" alt="Large rounded corners" rounded="lg" maxWidth="150px" maxHeight="150px"></app-image>
        <app-image src="https://picsum.photos/150/150" alt="Circular" rounded="full" maxWidth="150px" maxHeight="150px"></app-image>
      </div>
    `,
    imports: [ImageComponent],
  }),
};

export const DifferentSizes: Story = {
  render: () => ({
    template: `
      <div style="display: flex; gap: 16px; flex-wrap: wrap; align-items: center;">
        <app-image src="https://picsum.photos/100/100" alt="Small" maxWidth="100px" maxHeight="100px"></app-image>
        <app-image src="https://picsum.photos/200/200" alt="Medium" maxWidth="200px" maxHeight="200px"></app-image>
        <app-image src="https://picsum.photos/300/300" alt="Large" maxWidth="300px" maxHeight="300px"></app-image>
      </div>
    `,
    imports: [ImageComponent],
  }),
};

export const CenteredVsLeftAligned: Story = {
  render: () => ({
    template: `
      <div style="display: flex; flex-direction: column; gap: 32px;">
        <div>
          <h4>Left Aligned (Default)</h4>
          <app-image src="https://picsum.photos/300/200" alt="Left aligned image"></app-image>
        </div>
        <div>
          <h4>Centered</h4>
          <app-image src="https://picsum.photos/300/200" alt="Centered image" centered="true"></app-image>
        </div>
      </div>
    `,
    imports: [ImageComponent],
  }),
};

export const ErrorHandling: Story = {
  args: {
    src: 'https://invalid-url-that-will-fail.com/image.jpg',
    alt: 'This image will fail to load',
  },
}; 