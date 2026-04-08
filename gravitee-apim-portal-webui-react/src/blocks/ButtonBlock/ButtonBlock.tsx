import { createReactBlockSpec } from '@blocknote/react';
import styles from './ButtonBlock.module.scss';

type ButtonAppearance = 'filled' | 'outlined' | 'text';

const appearanceClasses: Record<ButtonAppearance, string> = {
  filled: styles.filled,
  outlined: styles.outlined,
  text: styles.text,
};

const appearances: ButtonAppearance[] = ['filled', 'outlined', 'text'];

export const ButtonBlock = createReactBlockSpec(
  {
    type: 'graviteeButton' as const,
    propSchema: {
      label: { default: 'Get Started' },
      link: { default: '/catalog' },
      appearance: { default: 'filled' as ButtonAppearance },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { label, link, appearance } = block.props;
      const isEditable = editor.isEditable;

      const cycleAppearance = () => {
        const idx = appearances.indexOf(appearance as ButtonAppearance);
        editor.updateBlock(block, { props: { appearance: appearances[(idx + 1) % appearances.length] } });
      };

      return (
        <div className={styles.wrapper}>
          {isEditable ? (
            <div className={styles.editRow}>
              <input
                className={styles.labelInput}
                value={label}
                onChange={(e) => editor.updateBlock(block, { props: { label: e.target.value } })}
                placeholder="Button text"
              />
              <input
                className={styles.linkInput}
                value={link}
                onChange={(e) => editor.updateBlock(block, { props: { link: e.target.value } })}
                placeholder="Link"
              />
              <button className={styles.styleBtn} onClick={cycleAppearance} title="Change style" type="button">
                {appearance}
              </button>
            </div>
          ) : (
            <a
              href={link}
              className={`${styles.button} ${appearanceClasses[appearance as ButtonAppearance] ?? styles.filled}`}
            >
              {label}
            </a>
          )}
        </div>
      );
    },
  },
);
