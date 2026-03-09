import HistoryPage from '../user_transaction_history/History';

export default function History() {
  return <HistoryPage apiBaseUrl="http://localhost:3001" isAuthenticated userId={1} />;
}
