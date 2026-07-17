import { createReactBlockSpec } from '@blocknote/react';
import { getGmdBlockHooks } from '../../editor/gmd/gmd-block-hooks';
import { EditableButtonBlock } from './EditableButtonBlock';

type ButtonAppearance = 'filled' | 'outlined' | 'text';

export const ButtonBlock = createReactBlockSpec(
  {
    type: 'graviteeButton' as const,
    propSchema: {
      label: { default: 'Get Started' },
      link: { default: '' },
      appearance: { default: 'filled' as ButtonAppearance },
      instanceStyle: { default: '{}' },
      pickLinkOpen: { default: 'false' },
    },
    content: 'none',
  },
  {
    ...getGmdBlockHooks('graviteeButton'),
    render: ({ block, editor }) => {
      const { label, link, appearance, instanceStyle, pickLinkOpen } = block.props;

      return (
        <EditableButtonBlock
          label={label}
          link={link}
          appearance={appearance}
          instanceStyleJson={instanceStyle}
          pickLinkOpen={pickLinkOpen}
          isEditable={editor.isEditable}
          onUpdate={patch => editor.updateBlock(block, { props: patch })}
        />
      );
    },
  },
);
