import { useRef, useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Editor, EditorHandle, type PageWidth } from '../components/Editor';
import styles from './EditPage.module.scss';

const PAGE_WIDTH_STORAGE_KEY = 'gravitee-portal-page-width';

const widthOptions: { key: PageWidth; label: string; icon: React.ReactNode }[] = [
  {
    key: 'narrow',
    label: 'Narrow',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="6" y="3" width="12" height="18" rx="1" />
      </svg>
    ),
  },
  {
    key: 'medium',
    label: 'Medium',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="4" y="3" width="16" height="18" rx="1" />
      </svg>
    ),
  },
  {
    key: 'wide',
    label: 'Wide',
    icon: (
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <rect x="2" y="3" width="20" height="18" rx="1" />
      </svg>
    ),
  },
];

export function EditPage() {
  const editorRef = useRef<EditorHandle>(null);
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
  const [pageWidth, setPageWidth] = useState<PageWidth>(
    () => (localStorage.getItem(PAGE_WIDTH_STORAGE_KEY) as PageWidth) || 'narrow',
  );

  useEffect(() => {
    localStorage.setItem(PAGE_WIDTH_STORAGE_KEY, pageWidth);
  }, [pageWidth]);

  const handleSave = async () => {
    setSaveState('saving');
    try {
      await editorRef.current?.save();
      setSaveState('saved');
      setTimeout(() => setSaveState('idle'), 2500);
    } catch {
      setSaveState('error');
      setTimeout(() => setSaveState('idle'), 3000);
    }
  };

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.logo}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="16" />
              <line x1="8" y1="12" x2="16" y2="12" />
            </svg>
            Gravitee Portal
            <span className={styles.modeBadge}>Editor</span>
          </div>
          <nav className={styles.nav}>
            <div className={styles.widthToggle}>
              {widthOptions.map((opt) => (
                <button
                  key={opt.key}
                  className={`${styles.widthBtn} ${pageWidth === opt.key ? styles.widthActive : ''}`}
                  onClick={() => setPageWidth(opt.key)}
                  title={opt.label}
                  type="button"
                >
                  {opt.icon}
                </button>
              ))}
            </div>
            {saveState === 'saved' && (
              <span className={styles.saveSuccess}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                Saved
              </span>
            )}
            {saveState === 'error' && (
              <span className={styles.saveError}>Error</span>
            )}
            <button className={styles.saveButton} onClick={handleSave} disabled={saveState === 'saving'} type="button">
              {saveState === 'saving' ? 'Saving...' : 'Save'}
            </button>
            <Link to="/" className={styles.previewLink}>
              View page
            </Link>
          </nav>
        </div>
      </header>
      <main className={styles.main}>
        <Editor ref={editorRef} pageWidth={pageWidth} />
      </main>
    </div>
  );
}
