import { useEffect, useMemo, useState } from "react";
import { RotateCcw, Search, SlidersHorizontal } from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { getMyAccounts, getTransactionHistory, updateTransactionCategory } from "../api/accountApi";
import { getApiErrorMessage } from "../api/axios";
import AccountSelect from "../components/AccountSelect";
import ErrorMessage from "../components/ErrorMessage";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import { categoryLabel } from "../constants/categories";
import { humanize } from "../utils/status";
import { Link } from "react-router-dom";
import TransactionTable from "../components/TransactionTable";
import { useToast } from "../context/ToastContext";
import PageHeader from "../components/PageHeader";

export default function Transactions() {
  const notify = useToast();
  const [searchParams] = useSearchParams();
  const [accounts, setAccounts] = useState([]);
  const [accountId, setAccountId] = useState(searchParams.get("accountId") || "");
  const [transactions, setTransactions] = useState([]);
  const [query, setQuery] = useState("");
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [type, setType] = useState("ALL");
  const [category, setCategory] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sortOrder, setSortOrder] = useState("NEWEST");
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [accountsLoading, setAccountsLoading] = useState(true);
  const [transactionsLoading, setTransactionsLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getMyAccounts().then(({ data }) => {
      setAccounts(data);
      if (!data.length) {
        setAccountId("");
      } else {
        const selected = searchParams.get("accountId");
        setAccountId(data.some((account) => String(account.id) === selected) ? selected : String(data[0].id));
      }
    }).catch((e) => setError(getApiErrorMessage(e))).finally(() => setAccountsLoading(false));
  }, []);

  useEffect(() => {
    if (!accountId) { setTransactions([]); setTransactionsLoading(false); return; }
    let active = true;
    setTransactionsLoading(true);
    setTransactions([]);
    getTransactionHistory(accountId)
      .then(({ data }) => { if (active) setTransactions(data); })
      .catch((e) => { if (active) setError(getApiErrorMessage(e)); })
      .finally(() => { if (active) setTransactionsLoading(false); });
    return () => { active = false; };
  }, [accountId]);

  const filtered = useMemo(() => {
    const term = query.trim().toLowerCase();
    return transactions
      .filter((item) => {
        const matchesType = type === "ALL" || item.type === type;
        const matchesCategory =
          category === "ALL" || (item.category || "") === category;
        const matchesStatus = status === "ALL" || item.status === status;
        const transactionDate = String(item.createdAt || "").slice(0, 10);
        const matchesDate = (!fromDate || transactionDate >= fromDate)
          && (!toDate || transactionDate <= toDate);
        const searchable = [
          item.referenceNumber,
          item.description,
          item.category,
          item.receiverAccountNumber,
          item.type,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        return (
          matchesType &&
          matchesCategory &&
          matchesStatus &&
          matchesDate &&
          (!term || searchable.includes(term))
        );
      })
      .sort((left, right) => {
        if (sortOrder === "AMOUNT_HIGH" || sortOrder === "AMOUNT_LOW") {
          const difference = Number(right.amount) - Number(left.amount);
          return sortOrder === "AMOUNT_HIGH" ? difference : -difference;
        }
        const difference = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
        return sortOrder === "NEWEST" ? difference : -difference;
      });
  }, [transactions, query, type, category, status, sortOrder, fromDate, toDate]);

  const categories = useMemo(
    () => [...new Set(transactions.map((item) => item.category).filter(Boolean))].sort(),
    [transactions],
  );
  const statuses = useMemo(
    () => [...new Set(transactions.map((item) => item.status).filter(Boolean))].sort(),
    [transactions],
  );

  function clearFilters() {
    setQuery("");
    setType("ALL");
    setCategory("ALL");
    setStatus("ALL");
    setSortOrder("NEWEST");
    setFromDate("");
    setToDate("");
  }

  async function changeCategory(transaction, nextCategory) {
    try {
      const { data } = await updateTransactionCategory(accountId, transaction.id, nextCategory);
      setTransactions((current) => current.map((item) => item.id === data.id ? data : item));
      notify("Transaction category updated.");
    } catch (e) {
      notify(getApiErrorMessage(e), "error");
    }
  }

  const activeFilters = [type !== "ALL", category !== "ALL", status !== "ALL", Boolean(fromDate), Boolean(toDate), sortOrder !== "NEWEST"].filter(Boolean).length;
  const header = <PageHeader eyebrow="Money" title="Transactions" description="Everything that moved in or out of your account. Search it, filter it, or fix a category." />;
  if (accountsLoading || transactionsLoading) return <div className="space-y-8">{header}<LoadingSkeleton variant="rows" count={5} /></div>;
  return (
    <div className="space-y-8">
      {header}
      <ErrorMessage message={error} onDismiss={() => setError("")} />
      {!accounts.length ? <EmptyState title="No account yet" description="Create your FinFlow account to start seeing transactions here." action={<Link to="/accounts" className="button-primary">Set up account</Link>} /> : (
        <section className="space-y-5">
          <div className="grid gap-4 md:grid-cols-[minmax(0,1fr)_minmax(0,1.4fr)_auto] md:items-end">
            <AccountSelect accounts={accounts} value={accountId} onChange={setAccountId} label="Account" />
            <label className="field-label">Search transactions<span className="relative"><Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" /><input className="field-control pl-10" placeholder="Description, category, reference…" value={query} onChange={(e) => setQuery(e.target.value)} /></span></label>
            <button type="button" className="button-secondary" aria-expanded={filtersOpen} onClick={() => setFiltersOpen(!filtersOpen)}><SlidersHorizontal size={16} aria-hidden="true" /> Filters{activeFilters ? <span className="tabular rounded-full bg-accent px-1.5 text-[11px] text-accent-on">{activeFilters}</span> : null}</button>
          </div>
          {filtersOpen && (
            <div className="motion-content grid gap-4 rounded-lg border border-line bg-surface p-4 sm:grid-cols-2 xl:grid-cols-3">
              <label className="field-label">Type<select className="field-control" value={type} onChange={(e) => setType(e.target.value)}><option value="ALL">All types</option><option value="DEPOSIT">{humanize("DEPOSIT")}</option><option value="WITHDRAWAL">{humanize("WITHDRAWAL")}</option><option value="TRANSFER_IN">{humanize("TRANSFER_IN")}</option><option value="TRANSFER_OUT">{humanize("TRANSFER_OUT")}</option></select></label>
              <label className="field-label">Category<select className="field-control" value={category} onChange={(e) => setCategory(e.target.value)}><option value="ALL">All categories</option>{categories.map((item) => <option key={item} value={item}>{categoryLabel(item)}</option>)}</select></label>
              <label className="field-label">Status<select className="field-control" value={status} onChange={(e) => setStatus(e.target.value)}><option value="ALL">All statuses</option>{statuses.map((item) => <option key={item} value={item}>{humanize(item)}</option>)}</select></label>
              <label className="field-label">From date<input className="field-control" type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} /></label>
              <label className="field-label">To date<input className="field-control" type="date" min={fromDate || undefined} value={toDate} onChange={(e) => setToDate(e.target.value)} /></label>
              <label className="field-label">Sort by<select className="field-control" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)}><option value="NEWEST">Newest first</option><option value="OLDEST">Oldest first</option><option value="AMOUNT_HIGH">Amount: high to low</option><option value="AMOUNT_LOW">Amount: low to high</option></select></label>
            </div>
          )}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-[13px] text-muted">Showing <span className="tabular font-semibold text-ink-soft">{filtered.length}</span> of <span className="tabular">{transactions.length}</span></p>
            {(activeFilters > 0 || query) && <button className="button-ghost min-h-8 px-2 text-xs" type="button" onClick={clearFilters}><RotateCcw size={14} aria-hidden="true" /> Clear filters</button>}
          </div>
          {filtered.length || !transactions.length ? <TransactionTable transactions={filtered} onCategoryChange={changeCategory} /> : <EmptyState title="Nothing matches these filters" description="Try a wider date range or clear the filters." action={<button type="button" className="button-secondary" onClick={clearFilters}>Clear filters</button>} />}
        </section>
      )}
    </div>
  );
}
