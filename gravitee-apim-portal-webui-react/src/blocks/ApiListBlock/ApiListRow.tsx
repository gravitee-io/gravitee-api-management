import { useNavigate } from 'react-router-dom';
import type { Api } from '../../entities/api';
import styles from './ApiListRow.module.scss';

interface ApiListRowProps {
  api: Api;
  clickable?: boolean;
}

export function ApiListRow({ api, clickable = false }: ApiListRowProps) {
  const navigate = useNavigate();

  const handleClick = () => {
    if (clickable) navigate(`/api/${api.id}`);
  };

  return (
    <div className={`${styles.row} ${clickable ? styles.clickable : ''}`} onClick={handleClick}>
      <span className={styles.name}>{api.name}</span>
      <span className={styles.description}>
        {api.description || 'Description for this API is missing.'}
      </span>
      <div className={styles.meta}>
        {api.labels?.includes('MCP') && <span className={styles.mcpBadge}>MCP</span>}
        <span className={styles.version}>v{api.version}</span>
      </div>
    </div>
  );
}
