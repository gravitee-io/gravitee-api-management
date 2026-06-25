import { useCallback, useRef, useState } from 'react';
import { createReactBlockSpec } from '@blocknote/react';
import styles from './SectionBlock.module.scss';

type SectionVariant = 'dark' | 'light' | 'gray' | 'accent' | 'none';

const variantMap: Record<SectionVariant, string> = {
  dark: styles.dark,
  light: styles.light,
  gray: styles.gray,
  accent: styles.accent,
  none: styles.noBackground,
};

const sectionVariants: SectionVariant[] = ['dark', 'light', 'gray', 'accent', 'none'];

type ContentWidth = 'auto' | 'narrow' | 'medium' | 'wide';

const contentWidthOptions: ContentWidth[] = ['auto', 'narrow', 'medium', 'wide'];

const contentWidthValues: Record<ContentWidth, string | null> = {
  auto: null,
  narrow: '760px',
  medium: '1080px',
  wide: '1400px',
};

const contentWidthLabels: Record<ContentWidth, string> = {
  auto: 'Auto',
  narrow: 'Narrow',
  medium: 'Medium',
  wide: 'Wide',
};

const iconMap: Record<string, string> = {
  code: 'M16 18l6-6-6-6M8 6l-6 6 6 6',
  terminal: 'M4 17l6-6-6-6M12 19h8',
  git: 'M6 3v12M18 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM18 9a9 9 0 0 1-9 9',
  database: 'M12 2C6.48 2 2 4.02 2 6.5v11C2 19.98 6.48 22 12 22s10-2.02 10-4.5v-11C22 4.02 17.52 2 12 2zM2 6.5C2 8.98 6.48 11 12 11s10-2.02 10-4.5M2 12c0 2.48 4.48 4.5 10 4.5s10-2.02 10-4.5',
  globe: 'M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10zM2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z',
  shield: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z',
  key: 'M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4',
  rocket: 'M4.5 16.5c-1.5 1.26-2 5-2 5s3.74-.5 5-2c.71-.84.7-2.13-.09-2.91a2.18 2.18 0 0 0-2.91-.09zM12 15l-3-3 8.5-8.5a2.12 2.12 0 0 1 3 3L12 15z',
  book: 'M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20',
  settings: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z',
  server: 'M2 4h20v6H2zM2 14h20v6H2zM6 7h.01M6 17h.01',
  cloud: 'M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z',
  monitor: 'M2 3h20v14H2zM8 21h8M12 17v4',
};

const iconNames = Object.keys(iconMap);

interface SectionItem {
  icon: string;
  title: string;
  description: string;
  buttonLabel?: string;
  buttonLink?: string;
}

const DEFAULT_ITEMS: SectionItem[] = [
  { icon: 'code', title: 'Full Code Control', description: 'Access all template files from the doctype to the footer.' },
  { icon: 'terminal', title: 'Local Development', description: 'Develop your site locally while working with your preferred tools.' },
  { icon: 'git', title: 'Git Built-in', description: 'Repositories are exposed via Git automatically for easy collaboration.' },
  { icon: 'database', title: 'Data Store', description: 'Query any page as JSON to access structured data via an API.' },
];

function parseItems(json: string): SectionItem[] {
  try {
    return JSON.parse(json);
  } catch {
    return DEFAULT_ITEMS;
  }
}

export const SectionBlock = createReactBlockSpec(
  {
    type: 'graviteeSection' as const,
    propSchema: {
      title: { default: '' },
      subtitle: { default: '' },
      variant: { default: 'dark' as SectionVariant },
      columns: { default: '4' },
      items: { default: JSON.stringify(DEFAULT_ITEMS) },
      height: { default: '0' },
      contentWidth: { default: 'auto' as ContentWidth },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { title, subtitle, variant, columns, items: itemsJson, height, contentWidth } = block.props;
      const isEditable = editor.isEditable;
      const items = parseItems(itemsJson);
      const cols = Number(columns) || 4;

      const sectionRef = useRef<HTMLDivElement>(null);
      const [dragging, setDragging] = useState(false);
      const [liveHeight, setLiveHeight] = useState<number>(0);
      const persistedHeight = Number(height) || 0;

      const onResizeStart = useCallback((e: React.MouseEvent) => {
        e.preventDefault();
        const startY = e.clientY;
        const startH = sectionRef.current?.getBoundingClientRect().height ?? 0;
        setDragging(true);

        const onMove = (ev: MouseEvent) => {
          setLiveHeight(Math.max(120, startH + (ev.clientY - startY)));
        };
        const onUp = (ev: MouseEvent) => {
          const finalH = Math.max(120, startH + (ev.clientY - startY));
          editor.updateBlock(block, { props: { height: String(Math.round(finalH)) } });
          setLiveHeight(0);
          setDragging(false);
          document.removeEventListener('mousemove', onMove);
          document.removeEventListener('mouseup', onUp);
        };
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
      }, [block, editor]);

      const activeHeight = dragging ? liveHeight : persistedHeight;

      const cycleVariant = () => {
        const idx = sectionVariants.indexOf(variant as SectionVariant);
        editor.updateBlock(block, { props: { variant: sectionVariants[(idx + 1) % sectionVariants.length] } });
      };

      const cycleColumns = () => {
        const options = [2, 3, 4];
        const idx = options.indexOf(cols);
        editor.updateBlock(block, { props: { columns: String(options[(idx + 1) % options.length]) } });
      };

      const cycleContentWidth = () => {
        const idx = contentWidthOptions.indexOf(contentWidth as ContentWidth);
        editor.updateBlock(block, { props: { contentWidth: contentWidthOptions[(idx + 1) % contentWidthOptions.length] } });
      };

      const cw = contentWidth as ContentWidth;
      const cwValue = contentWidthValues[cw];

      const updateItem = (index: number, field: keyof SectionItem, value: string) => {
        const updated = [...items];
        updated[index] = { ...updated[index], [field]: value };
        editor.updateBlock(block, { props: { items: JSON.stringify(updated) } });
      };

      const cycleItemIcon = (index: number) => {
        const updated = [...items];
        const currentIdx = iconNames.indexOf(updated[index].icon);
        updated[index] = { ...updated[index], icon: iconNames[(currentIdx + 1) % iconNames.length] };
        editor.updateBlock(block, { props: { items: JSON.stringify(updated) } });
      };

      const addItem = () => {
        const updated = [...items, { icon: 'rocket', title: 'New Feature', description: 'Describe this feature here.' }];
        editor.updateBlock(block, { props: { items: JSON.stringify(updated) } });
      };

      const removeItem = (index: number) => {
        const updated = items.filter((_, i) => i !== index);
        editor.updateBlock(block, { props: { items: JSON.stringify(updated) } });
      };

      const variantClass = variantMap[variant as SectionVariant] ?? styles.dark;

      const sectionStyle: React.CSSProperties = {
        ...(activeHeight > 0 ? { minHeight: activeHeight } : {}),
      };

      return (
        <div ref={sectionRef} className={`${styles.section} ${variantClass}`} style={sectionStyle}>
          {isEditable && (
            <div className={styles.floatingControls}>
              <button className={styles.controlBtn} onClick={cycleVariant} title="Change background" type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <circle cx="13.5" cy="6.5" r="2.5" />
                  <circle cx="6" cy="12" r="2.5" />
                  <circle cx="18" cy="12" r="2.5" />
                  <circle cx="12" cy="18" r="2.5" />
                </svg>
              </button>
              <button className={styles.controlBtn} onClick={cycleColumns} title={`Columns: ${cols}`} type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  {cols === 2 && (
                    <>
                      <rect x="3" y="3" width="8" height="18" rx="1" />
                      <rect x="13" y="3" width="8" height="18" rx="1" />
                    </>
                  )}
                  {cols === 3 && (
                    <>
                      <rect x="2" y="3" width="5.5" height="18" rx="1" />
                      <rect x="9.25" y="3" width="5.5" height="18" rx="1" />
                      <rect x="16.5" y="3" width="5.5" height="18" rx="1" />
                    </>
                  )}
                  {cols === 4 && (
                    <>
                      <rect x="1.5" y="3" width="4" height="18" rx="1" />
                      <rect x="7.17" y="3" width="4" height="18" rx="1" />
                      <rect x="12.83" y="3" width="4" height="18" rx="1" />
                      <rect x="18.5" y="3" width="4" height="18" rx="1" />
                    </>
                  )}
                </svg>
              </button>
              <button className={styles.controlBtn} onClick={cycleContentWidth} title={`Width: ${contentWidthLabels[cw]}`} type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  {cw === 'auto' && (
                    <>
                      <rect x="4" y="4" width="16" height="16" rx="1" />
                      <line x1="12" y1="8" x2="12" y2="16" />
                      <line x1="8" y1="12" x2="16" y2="12" />
                    </>
                  )}
                  {cw === 'narrow' && (
                    <>
                      <line x1="5" y1="12" x2="9" y2="12" />
                      <line x1="15" y1="12" x2="19" y2="12" />
                      <polyline points="9 8 5 12 9 16" />
                      <polyline points="15 8 19 12 15 16" />
                    </>
                  )}
                  {cw === 'medium' && (
                    <>
                      <line x1="3" y1="12" x2="8" y2="12" />
                      <line x1="16" y1="12" x2="21" y2="12" />
                      <polyline points="8 8 3 12 8 16" />
                      <polyline points="16 8 21 12 16 16" />
                    </>
                  )}
                  {cw === 'wide' && (
                    <>
                      <line x1="1" y1="12" x2="7" y2="12" />
                      <line x1="17" y1="12" x2="23" y2="12" />
                      <polyline points="7 8 1 12 7 16" />
                      <polyline points="17 8 23 12 17 16" />
                    </>
                  )}
                </svg>
              </button>
            </div>
          )}

          <div className={styles.content} style={cwValue ? { maxWidth: cwValue } : undefined}>
            {isEditable ? (
              <div className={styles.headerEdit}>
                <input
                  className={styles.titleInput}
                  value={title}
                  onChange={(e) => editor.updateBlock(block, { props: { title: e.target.value } })}
                  placeholder="Section title (optional)"
                />
                <input
                  className={styles.subtitleInput}
                  value={subtitle}
                  onChange={(e) => editor.updateBlock(block, { props: { subtitle: e.target.value } })}
                  placeholder="Section subtitle (optional)"
                />
              </div>
            ) : (
              (title || subtitle) && (
                <div className={styles.header}>
                  {title && <h2 className={styles.title}>{title}</h2>}
                  {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
                </div>
              )
            )}

            <div className={styles.grid} style={{ gridTemplateColumns: `repeat(${cols}, 1fr)` }}>
              {items.map((item, index) => (
                <SectionItemCard
                  key={index}
                  item={item}
                  index={index}
                  isEditable={isEditable}
                  onUpdate={updateItem}
                  onCycleIcon={cycleItemIcon}
                  onRemove={removeItem}
                />
              ))}
            </div>

            {isEditable && (
              <button className={styles.addItemBtn} onClick={addItem} type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <path d="M12 5v14M5 12h14" />
                </svg>
                Add item
              </button>
            )}
          </div>

          {isEditable && (
            <div
              className={`${styles.resizeHandle} ${dragging ? styles.resizeActive : ''}`}
              onMouseDown={onResizeStart}
            >
              <div className={styles.resizeBar} />
            </div>
          )}
        </div>
      );
    },
  },
);

interface SectionItemCardProps {
  item: SectionItem;
  index: number;
  isEditable: boolean;
  onUpdate: (index: number, field: keyof SectionItem, value: string) => void;
  onCycleIcon: (index: number) => void;
  onRemove: (index: number) => void;
}

function SectionItemCard({ item, index, isEditable, onUpdate, onCycleIcon, onRemove }: SectionItemCardProps) {
  const [hovered, setHovered] = useState(false);
  const iconPath = iconMap[item.icon] || iconMap.code;
  const hasButton = !!(item.buttonLabel && item.buttonLink);

  return (
    <div
      className={styles.item}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {isEditable && hovered && (
        <div className={styles.itemControls}>
          <button className={styles.itemControlBtn} onClick={() => onCycleIcon(index)} title="Change icon" type="button">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 3v18M3 12h18" />
            </svg>
          </button>
          <button className={`${styles.itemControlBtn} ${styles.removeBtn}`} onClick={() => onRemove(index)} title="Remove" type="button">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M18 6L6 18M6 6l12 12" />
            </svg>
          </button>
        </div>
      )}

      <div className={styles.itemIcon}>
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
          <path d={iconPath} />
        </svg>
      </div>

      {isEditable ? (
        <>
          <input
            className={styles.itemTitleInput}
            value={item.title}
            onChange={(e) => onUpdate(index, 'title', e.target.value)}
            placeholder="Title..."
          />
          <textarea
            className={styles.itemDescInput}
            value={item.description}
            onChange={(e) => onUpdate(index, 'description', e.target.value)}
            placeholder="Description..."
            rows={2}
          />
          <div className={styles.itemBtnEdit}>
            <input
              className={styles.itemBtnLabelInput}
              value={item.buttonLabel || ''}
              onChange={(e) => onUpdate(index, 'buttonLabel', e.target.value)}
              placeholder="Button label (optional)"
            />
            <input
              className={styles.itemBtnLinkInput}
              value={item.buttonLink || ''}
              onChange={(e) => onUpdate(index, 'buttonLink', e.target.value)}
              placeholder="/path"
            />
          </div>
        </>
      ) : (
        <>
          <h4 className={styles.itemTitle}>{item.title}</h4>
          <p className={styles.itemDesc}>{item.description}</p>
          {hasButton && (
            <a href={item.buttonLink} className={styles.itemBtn}>
              {item.buttonLabel}
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M5 12h14M12 5l7 7-7 7" />
              </svg>
            </a>
          )}
        </>
      )}
    </div>
  );
}
