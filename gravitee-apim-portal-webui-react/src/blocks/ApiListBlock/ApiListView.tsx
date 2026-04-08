import { useQuery } from '@tanstack/react-query';
import { searchApis } from '../../services/api.service';
import { ApiCard } from './ApiCard';
import { ApiListRow } from './ApiListRow';
import styles from './ApiListView.module.scss';

export type ViewMode = 'cards' | 'list';

interface ApiListViewProps {
  limit?: number;
  category?: string;
  viewMode?: ViewMode;
  title?: string;
  clickable?: boolean;
}

export function ApiListView({ limit = 5, category = '', viewMode = 'cards', title, clickable = false }: ApiListViewProps) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['apis', { limit, category }],
    queryFn: () => searchApis({ size: limit, category }),
  });

  const apis = data?.data ?? [];

  return (
    <div className={styles.container}>
      {title && <h3 className={styles.title}>{title}</h3>}

      {isLoading && (
        <div className={styles.state}>
          <div className={styles.spinner} />
          <span>Loading APIs...</span>
        </div>
      )}

      {isError && (
        <div className={styles.state}>
          <span className={styles.error}>Failed to load APIs.</span>
        </div>
      )}

      {!isLoading && !isError && apis.length === 0 && (
        <div className={styles.state}>
          <span>No APIs found.</span>
        </div>
      )}

      {!isLoading && !isError && apis.length > 0 && (
        <>
          {viewMode === 'cards' ? (
            <div className={styles.grid}>
              {apis.map((api) => (
                <ApiCard key={api.id} api={api} clickable={clickable} />
              ))}
            </div>
          ) : (
            <div className={styles.list}>
              {apis.map((api) => (
                <ApiListRow key={api.id} api={api} clickable={clickable} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
