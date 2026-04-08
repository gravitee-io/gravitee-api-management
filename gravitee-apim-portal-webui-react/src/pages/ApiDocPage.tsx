import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { listPagesByApiId, getPageContent } from '../services/page.service';
import { searchApis } from '../services/api.service';
import { MarkdownRenderer } from '../components/MarkdownRenderer';
import type { Page } from '../entities/page';
import styles from './ApiDocPage.module.scss';

function buildTree(pages: Page[]): Page[] {
  const contentPages = pages
    .filter((p) => p.type !== 'FOLDER' && p.type !== 'ROOT' && p.type !== 'LINK')
    .sort((a, b) => a.order - b.order);

  return contentPages;
}

export function ApiDocPage() {
  const { apiId } = useParams<{ apiId: string }>();
  const [selectedPageId, setSelectedPageId] = useState<string | null>(null);

  const { data: apiData } = useQuery({
    queryKey: ['api-detail', apiId],
    queryFn: () => searchApis({ size: 1, q: apiId ?? '' }),
    enabled: !!apiId,
  });

  const apiName = apiData?.data?.[0]?.name ?? 'API';

  const { data: pages = [], isLoading: pagesLoading } = useQuery({
    queryKey: ['api-pages', apiId],
    queryFn: () => listPagesByApiId(apiId!),
    enabled: !!apiId,
  });

  const navPages = buildTree(pages);

  useEffect(() => {
    if (navPages.length > 0 && !selectedPageId) {
      setSelectedPageId(navPages[0].id);
    }
  }, [navPages, selectedPageId]);

  const { data: pageContent, isLoading: contentLoading } = useQuery({
    queryKey: ['page-content', apiId, selectedPageId],
    queryFn: () => getPageContent(apiId!, selectedPageId!),
    enabled: !!apiId && !!selectedPageId,
  });

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.breadcrumb}>
            <Link to="/" className={styles.breadcrumbLink}>Portal</Link>
            <span className={styles.separator}>/</span>
            <span className={styles.current}>{apiName}</span>
          </div>
        </div>
      </header>

      <div className={styles.layout}>
        <aside className={styles.sidebar}>
          <div className={styles.sidebarTitle}>{apiName}</div>
          <nav className={styles.nav}>
            {pagesLoading && <div className={styles.navLoading}>Loading...</div>}
            {navPages.map((p) => (
              <button
                key={p.id}
                className={`${styles.navItem} ${selectedPageId === p.id ? styles.active : ''}`}
                onClick={() => setSelectedPageId(p.id)}
                type="button"
              >
                {p.name}
              </button>
            ))}
          </nav>
        </aside>

        <main className={styles.main}>
          {contentLoading && (
            <div className={styles.loading}>
              <div className={styles.spinner} />
            </div>
          )}

          {!contentLoading && pageContent?.content && (
            <MarkdownRenderer content={pageContent.content} />
          )}

          {!contentLoading && !pageContent?.content && selectedPageId && (
            <div className={styles.empty}>This page has no content.</div>
          )}

          {!contentLoading && !selectedPageId && navPages.length === 0 && !pagesLoading && (
            <div className={styles.empty}>No documentation available for this API.</div>
          )}
        </main>
      </div>
    </div>
  );
}
