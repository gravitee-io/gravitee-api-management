import type { Meta, StoryObj } from '@storybook/react';
import { TopNavUser } from './TopNavUser';

const meta = {
  title: 'Layout/TopNavUser',
  component: TopNavUser,
  tags: ['autodocs'],
} satisfies Meta<typeof TopNavUser>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    user: { name: 'Jane Doe', email: 'jane.doe@gravitee.io' },
  },
};

export const WithAvatar: Story = {
  args: {
    user: {
      name: 'Jane Doe',
      email: 'jane.doe@gravitee.io',
      avatar: 'https://i.pravatar.cc/150?u=jane',
    },
  },
};
