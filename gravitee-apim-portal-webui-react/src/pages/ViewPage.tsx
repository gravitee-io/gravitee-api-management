import { Link } from 'react-router-dom';
import { Viewer } from '../components/Viewer';
import { type PageWidth } from '../components/Editor';
import styles from './ViewPage.module.scss';

const PAGE_WIDTH_STORAGE_KEY = 'gravitee-portal-page-width';

export function ViewPage() {
  const pageWidth = (localStorage.getItem(PAGE_WIDTH_STORAGE_KEY) as PageWidth) || 'narrow';

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
          </div>
          <nav className={styles.nav}>
            <Link to="/edit" className={styles.editLink}>
              Edit page
            </Link>
          </nav>
        </div>
      </header>
      <main className={styles.main}>
        <Viewer pageWidth={pageWidth} />
      </main>
    </div>
  );
}
