import { useCallback, useRef, useState } from 'react';
import { createReactBlockSpec } from '@blocknote/react';
import { uploadFile } from '../../utils/upload';
import styles from './BannerBlock.module.scss';

type BannerVariant = 'none' | 'indigo' | 'dark' | 'gradient' | 'light' | 'image';

const variantMap: Record<BannerVariant, string> = {
  none: styles.none,
  indigo: styles.indigo,
  dark: styles.dark,
  gradient: styles.gradient,
  light: styles.light,
  image: styles.image,
};

const variants: BannerVariant[] = ['none', 'indigo', 'dark', 'gradient', 'light'];

interface BannerButton {
  label: string;
  link: string;
  variant: 'primary' | 'secondary' | 'outline';
}

const DEFAULT_BUTTONS: BannerButton[] = [
  { label: 'Browse APIs', link: '/catalog', variant: 'primary' },
  { label: 'Get Started', link: '/getting-started', variant: 'secondary' },
];

const buttonVariants: BannerButton['variant'][] = ['primary', 'secondary', 'outline'];

function parseButtons(json: string): BannerButton[] {
  try {
    return JSON.parse(json);
  } catch {
    return DEFAULT_BUTTONS;
  }
}

const buttonStyleMap: Record<string, string> = {
  primary: styles.ctaPrimary,
  secondary: styles.ctaSecondary,
  outline: styles.ctaOutline,
};

export const BannerBlock = createReactBlockSpec(
  {
    type: 'graviteeBanner' as const,
    propSchema: {
      title: { default: 'Welcome to the Developer Portal' },
      subtitle: { default: 'Explore our APIs, read the docs, and start building.' },
      buttons: { default: JSON.stringify(DEFAULT_BUTTONS) },
      variant: { default: 'indigo' as BannerVariant },
      backgroundImage: { default: '' },
      height: { default: '0' },
    },
    content: 'none',
  },
  {
    render: ({ block, editor }) => {
      const { title, subtitle, buttons: buttonsJson, variant, backgroundImage, height } = block.props;
      const isEditable = editor.isEditable;
      const buttons = parseButtons(buttonsJson);
      const [showImageInput, setShowImageInput] = useState(false);
      const fileInputRef = useRef<HTMLInputElement>(null);
      const bannerRef = useRef<HTMLDivElement>(null);
      const [dragging, setDragging] = useState(false);
      const [liveHeight, setLiveHeight] = useState<number>(0);

      const persistedHeight = Number(height) || 0;

      const hasImage = !!backgroundImage;
      const effectiveVariant = hasImage ? 'image' : variant;

      const onResizeStart = useCallback((e: React.MouseEvent) => {
        e.preventDefault();
        const startY = e.clientY;
        const startH = bannerRef.current?.getBoundingClientRect().height ?? 0;
        setDragging(true);

        const onMove = (ev: MouseEvent) => {
          const newH = Math.max(120, startH + (ev.clientY - startY));
          setLiveHeight(newH);
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

      const cycleVariant = () => {
        const idx = variants.indexOf(variant as BannerVariant);
        editor.updateBlock(block, { props: { variant: variants[(idx + 1) % variants.length] } });
      };

      const handleImageSet = (url: string) => {
        if (url.trim()) {
          editor.updateBlock(block, { props: { backgroundImage: url.trim() } });
        }
        setShowImageInput(false);
      };

      const handleFileUpload = async (file: File) => {
        const dataUrl = await uploadFile(file);
        editor.updateBlock(block, { props: { backgroundImage: dataUrl } });
        setShowImageInput(false);
      };

      const clearImage = () => {
        editor.updateBlock(block, { props: { backgroundImage: '' } });
      };

      const updateButton = (index: number, field: keyof BannerButton, value: string) => {
        const updated = [...buttons];
        updated[index] = { ...updated[index], [field]: value };
        editor.updateBlock(block, { props: { buttons: JSON.stringify(updated) } });
      };

      const cycleButtonVariant = (index: number) => {
        const updated = [...buttons];
        const currentIdx = buttonVariants.indexOf(updated[index].variant);
        updated[index] = { ...updated[index], variant: buttonVariants[(currentIdx + 1) % buttonVariants.length] };
        editor.updateBlock(block, { props: { buttons: JSON.stringify(updated) } });
      };

      const addButton = () => {
        const updated = [...buttons, { label: 'New Button', link: '/', variant: 'secondary' as const }];
        editor.updateBlock(block, { props: { buttons: JSON.stringify(updated) } });
      };

      const removeButton = (index: number) => {
        const updated = buttons.filter((_, i) => i !== index);
        editor.updateBlock(block, { props: { buttons: JSON.stringify(updated) } });
      };

      const variantClass = variantMap[effectiveVariant as BannerVariant] ?? styles.indigo;

      const activeHeight = dragging ? liveHeight : persistedHeight;
      const bannerStyle: React.CSSProperties = {
        ...(hasImage ? { backgroundImage: `url(${backgroundImage})`, backgroundSize: 'cover', backgroundPosition: 'center' } : {}),
        ...(activeHeight > 0 ? { minHeight: activeHeight } : {}),
      };

      return (
        <div ref={bannerRef} className={`${styles.banner} ${variantClass}`} style={bannerStyle}>
          {hasImage && <div className={styles.imageOverlay} />}
          {isEditable && (
            <div className={styles.floatingControls}>
              <button className={styles.controlBtn} onClick={cycleVariant} title="Change color style" type="button">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <circle cx="13.5" cy="6.5" r="2.5" />
                  <circle cx="6" cy="12" r="2.5" />
                  <circle cx="18" cy="12" r="2.5" />
                  <circle cx="12" cy="18" r="2.5" />
                </svg>
              </button>
              <button
                className={`${styles.controlBtn} ${hasImage ? styles.controlBtnActive : ''}`}
                onClick={() => setShowImageInput(!showImageInput)}
                title={hasImage ? 'Change background image' : 'Set background image'}
                type="button"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="8.5" cy="8.5" r="1.5" /><polyline points="21 15 16 10 5 21" />
                </svg>
              </button>
            </div>
          )}

          {isEditable && showImageInput && (
            <div className={styles.imageInputPopover}>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className={styles.fileInput}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) handleFileUpload(file);
                }}
              />
              <button
                className={styles.uploadBtn}
                onClick={() => fileInputRef.current?.click()}
                type="button"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
                {hasImage ? 'Replace image' : 'Upload image'}
              </button>
              <div className={styles.imageDivider}>
                <span>or</span>
              </div>
              <input
                className={styles.imageUrlInput}
                placeholder="Paste image URL..."
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleImageSet((e.target as HTMLInputElement).value);
                  if (e.key === 'Escape') setShowImageInput(false);
                }}
              />
              {hasImage && (
                <>
                  <div className={styles.imageDivider} />
                  <button
                    className={styles.removeImageBtn}
                    onClick={() => { clearImage(); setShowImageInput(false); }}
                    type="button"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <polyline points="3 6 5 6 21 6" /><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" /><path d="M10 11v6" /><path d="M14 11v6" />
                    </svg>
                    Remove image
                  </button>
                </>
              )}
            </div>
          )}

          <div className={styles.content}>
            {isEditable ? (
              <input
                className={styles.titleInput}
                style={{ fontSize: 56, fontWeight: 800, lineHeight: 1.15 }}
                value={title}
                onChange={(e) => editor.updateBlock(block, { props: { title: e.target.value } })}
                placeholder="Banner title..."
              />
            ) : (
              <h1 className={styles.title} style={{ fontSize: 56, fontWeight: 800, lineHeight: 1.15 }}>{title}</h1>
            )}

            {isEditable ? (
              <input
                className={styles.subtitleInput}
                style={{ fontSize: 18, lineHeight: 1.5 }}
                value={subtitle}
                onChange={(e) => editor.updateBlock(block, { props: { subtitle: e.target.value } })}
                placeholder="Subtitle..."
              />
            ) : (
              <p className={styles.subtitle} style={{ fontSize: 18, lineHeight: 1.5 }}>{subtitle}</p>
            )}

            <div className={styles.cta}>
              {isEditable ? (
                <div className={styles.ctaEditGroup}>
                  {buttons.map((btn, index) => (
                    <div className={styles.ctaEditRow} key={index}>
                      <input
                        className={styles.ctaInput}
                        value={btn.label}
                        onChange={(e) => updateButton(index, 'label', e.target.value)}
                        placeholder="Button label"
                      />
                      <input
                        className={styles.ctaLinkInput}
                        value={btn.link}
                        onChange={(e) => updateButton(index, 'link', e.target.value)}
                        placeholder="/path"
                      />
                      <button
                        className={styles.ctaVariantBtn}
                        onClick={() => cycleButtonVariant(index)}
                        title={`Style: ${btn.variant}`}
                        type="button"
                      >
                        {btn.variant === 'primary' ? '●' : btn.variant === 'secondary' ? '○' : '◌'}
                      </button>
                      <button
                        className={styles.ctaRemoveBtn}
                        onClick={() => removeButton(index)}
                        title="Remove button"
                        type="button"
                      >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M18 6L6 18M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  ))}
                  <button className={styles.ctaAddBtn} onClick={addButton} type="button">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M12 5v14M5 12h14" />
                    </svg>
                    Add button
                  </button>
                </div>
              ) : (
                buttons.length > 0 && (
                  <div className={styles.ctaButtons}>
                    {buttons.map((btn, index) => (
                      btn.label && (
                        <a
                          key={index}
                          href={btn.link}
                          className={buttonStyleMap[btn.variant] ?? styles.ctaPrimary}
                        >
                          {btn.label}
                        </a>
                      )
                    ))}
                  </div>
                )
              )}
            </div>
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
