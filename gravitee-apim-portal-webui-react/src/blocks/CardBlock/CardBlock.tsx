import { createReactBlockSpec } from '@blocknote/react';
import styles from './CardBlock.module.scss';

type CardColor = 'white' | 'blue' | 'purple' | 'green' | 'orange';

const colorClasses: Record<CardColor, string> = {
  white: styles.white,
  blue: styles.blue,
  purple: styles.purple,
  green: styles.green,
  orange: styles.orange,
};

const colors: CardColor[] = ['white', 'blue', 'purple', 'green', 'orange'];

const iconMap: Record<string, string> = {
  book: 'M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20',
  rocket: 'M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09zM12 15l-3-3 8.5-8.5a2.12 2.12 0 0 1 3 3L12 15z',
  key: 'M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4',
  globe: 'M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10zM2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z',
};

const iconNames = Object.keys(iconMap);

export const CardBlock = createReactBlockSpec(
  {
    type: 'graviteeCard' as const,
    propSchema: {
      title: { default: 'Feature Card' },
      subtitle: { default: 'Describe your feature or category here.' },
      icon: { default: 'book' },
      color: { default: 'white' as CardColor },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { title, subtitle, icon, color } = block.props;
      const isEditable = editor.isEditable;

      const cycleColor = () => {
        const idx = colors.indexOf(color as CardColor);
        editor.updateBlock(block, { props: { color: colors[(idx + 1) % colors.length] } });
      };

      const cycleIcon = () => {
        const idx = iconNames.indexOf(icon);
        editor.updateBlock(block, { props: { icon: iconNames[(idx + 1) % iconNames.length] } });
      };

      const iconPath = iconMap[icon] || iconMap.book;

      return (
        <div className={`${styles.card} ${colorClasses[color as CardColor] ?? styles.white}`}>
          {isEditable && (
            <div className={styles.floatingControls}>
              <button className={styles.controlBtn} onClick={cycleIcon} title="Change icon" type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <path d="M12 3v18M3 12h18" />
                </svg>
              </button>
              <button className={styles.controlBtn} onClick={cycleColor} title="Change color" type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <circle cx="13.5" cy="6.5" r="2.5" />
                  <circle cx="6" cy="12" r="2.5" />
                  <circle cx="18" cy="12" r="2.5" />
                  <circle cx="12" cy="18" r="2.5" />
                </svg>
              </button>
            </div>
          )}

          <div className={styles.iconWrapper}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d={iconPath} />
            </svg>
          </div>

          {isEditable ? (
            <>
              <input
                className={styles.titleInput}
                value={title}
                onChange={(e) => editor.updateBlock(block, { props: { title: e.target.value } })}
                placeholder="Title..."
              />
              <input
                className={styles.subtitleInput}
                value={subtitle}
                onChange={(e) => editor.updateBlock(block, { props: { subtitle: e.target.value } })}
                placeholder="Description..."
              />
            </>
          ) : (
            <>
              <h4 className={styles.title}>{title}</h4>
              <p className={styles.subtitle}>{subtitle}</p>
            </>
          )}
        </div>
      );
    },
  },
);
