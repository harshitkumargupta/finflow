import { useCallback, useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { BookOpen, Newspaper, Plus, RefreshCw, Search, Trash2 } from "lucide-react";
import { getApiErrorMessage } from "../api/axios";
import {
  addWatchlistItem,
  getInvestmentEducation,
  getMarketNews as getLegacyMarketNews,
  getMarketSnapshot,
  getWatchlist,
  removeWatchlistItem,
} from "../api/investmentApi";
import { getInvestmentNewsFeed, getLatestNews, getMarketNewsFeed, searchNews } from "../api/newsApi";
import ErrorMessage from "../components/ErrorMessage";
import EmptyState from "../components/EmptyState";
import LoadingSkeleton from "../components/LoadingSkeleton";
import PageHeader from "../components/PageHeader";
import PortfolioPanel from "../components/PortfolioPanel";
import { humanize } from "../utils/status";
import { useToast } from "../context/ToastContext";
import { formatDate, formatDateTime } from "../utils/format";
import StatusBadge from "../components/StatusBadge";

const categories = ["STOCKS", "MUTUAL_FUNDS", "INDEX_FUNDS", "BONDS", "FIXED_DEPOSITS", "GOLD", "GOVERNMENT_SECURITIES", "OTHER"];

export default function Investments() {
  const notify = useToast();
  const location = useLocation();
  const [education, setEducation] = useState(null);
  const [news, setNews] = useState(null);
  const [newsFeed, setNewsFeed] = useState(null);
  const [newsTab, setNewsTab] = useState("market");
  const [newsQuery, setNewsQuery] = useState("");
  const [newsBusy, setNewsBusy] = useState(false);
  const [snapshot, setSnapshot] = useState(null);
  const [watchlist, setWatchlist] = useState([]);
  const [form, setForm] = useState({ symbol: "", name: "", category: "STOCKS" });
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    const [educationResult, newsResult, snapshotResult, watchlistResult] = await Promise.all([
      getInvestmentEducation(), getLegacyMarketNews(), getMarketSnapshot(), getWatchlist(),
    ]);
    setEducation(educationResult.data);
    setNews(newsResult.data);
    setSnapshot(snapshotResult.data);
    setWatchlist(watchlistResult.data);
  }, []);

  const loadNewsFeed = useCallback(async (tab = newsTab, query = newsQuery) => {
    setNewsBusy(true);
    try {
      const response = tab === "market"
        ? await getMarketNewsFeed()
        : tab === "investments"
          ? await getInvestmentNewsFeed()
          : tab === "watchlist"
            ? await getInvestmentNewsFeed()
            : await searchNews(query.trim() || "markets");
      setNewsFeed(response.data);
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setNewsBusy(false);
    }
  }, [newsQuery, newsTab]);

  useEffect(() => {
    load().catch((cause) => setError(getApiErrorMessage(cause))).finally(() => setLoading(false));
  }, [load]);

  useEffect(() => {
    loadNewsFeed().catch((cause) => setError(getApiErrorMessage(cause)));
  }, [loadNewsFeed]);

  const tab = location.pathname === "/market-news"
    ? "news"
    : location.pathname === "/investment-education" ? "education" : "overview";
  const filteredWatchlist = watchlist.filter((item) =>
    `${item.name} ${item.symbol} ${item.category}`.toLowerCase().includes(search.toLowerCase()));

  async function addItem(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await addWatchlistItem({ ...form, symbol: form.symbol.trim().toUpperCase() });
      setForm({ symbol: "", name: "", category: "STOCKS" });
      await load();
      notify("Watchlist item added. Live quotes are not configured.");
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  async function removeItem(id) {
    setBusy(true);
    setError("");
    try {
      await removeWatchlistItem(id);
      await load();
      notify("Watchlist item removed.");
    } catch (cause) {
      setError(getApiErrorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  const header = <PageHeader eyebrow="Understand" title="Investments" description="News, plain-language explainers and a private watchlist. FinFlow doesn’t recommend, buy or sell anything, and never shows prices it doesn’t have." />;
  const sections = [["/investments", "Overview", "overview"], ["/market-news", "News", "news"], ["/investment-education", "Learn", "education"]];
  const sectionNav = (
    <nav aria-label="Investment sections" className="inline-flex rounded-md border border-line bg-surface p-0.5">
      {sections.map(([to, label, key]) => <Link key={key} to={to} aria-current={tab === key ? "page" : undefined} className={`rounded-[7px] px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${tab === key ? "bg-ink text-paper" : "text-ink-soft hover:text-ink"}`}>{label}</Link>)}
    </nav>
  );
  if (loading) return <div className="space-y-10">{header}<LoadingSkeleton variant="rows" count={5} /></div>;
  const showOverview = tab === "overview";
  const statusLabel = newsFeed?.available ? (newsFeed?.message?.toLowerCase().includes("cache") ? "Cached" : "Live") : newsFeed?.stale ? "Cached" : "Unavailable";
  const statusTone = statusLabel === "Live" ? "SUCCESS" : statusLabel === "Cached" ? "PENDING" : "UNAVAILABLE";
  const allArticles = newsFeed?.articles || [];
  const articles = showOverview ? allArticles.slice(0, 5) : allArticles;
  return (
    <div className="space-y-12">
      {header}
      {sectionNav}
      <ErrorMessage message={error} onDismiss={() => setError("")} />

      {showOverview && <PortfolioPanel />}

      {(showOverview || tab === "news") && (
        <section aria-labelledby="snapshot-heading" className="flex flex-wrap items-start justify-between gap-4 border-y border-line py-4">
          <div className="min-w-0">
            <h2 id="snapshot-heading" className="text-sm font-semibold text-ink">Market prices</h2>
            {snapshot?.available && snapshot.indices?.length ? (
              <ul className="mt-2 flex flex-wrap gap-x-8 gap-y-2">{snapshot.indices.map((index) => <li key={index.symbol} className="text-sm"><span className="text-ink-soft">{index.name}</span> <span className="tabular font-semibold text-ink">{index.value}</span>{index.changePercent != null && <span className={`tabular ml-1 ${Number(index.changePercent) < 0 ? "text-warm" : "text-accent-strong"}`}>{Number(index.changePercent) > 0 ? "+" : ""}{index.changePercent}%</span>}</li>)}</ul>
            ) : <p className="mt-1 max-w-xl text-sm leading-6 text-ink-soft">No market-data provider is connected, so FinFlow shows no prices or indices rather than made-up ones.</p>}
          </div>
          <StatusBadge status={snapshot?.available ? "SUCCESS" : "UNAVAILABLE"} label={snapshot?.available ? "Live" : "Not connected"} />
        </section>
      )}

      {(showOverview || tab === "news") && (
        <section aria-labelledby="news-heading">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 id="news-heading" className="flex items-center gap-3 font-display text-2xl font-medium text-ink">News <StatusBadge status={statusTone} label={statusLabel} /></h2>
              <p className="mt-1 text-sm text-ink-soft">{newsFeed?.message || "Articles appear when a news provider is configured."}{newsFeed?.lastUpdated ? ` Updated ${formatDateTime(newsFeed.lastUpdated)}.` : ""}</p>
            </div>
            <button className="button-ghost -mr-3" onClick={() => loadNewsFeed()} disabled={newsBusy}><RefreshCw size={15} className={newsBusy ? "animate-spin" : ""} aria-hidden="true" /> Refresh</button>
          </div>
          <div role="tablist" aria-label="News topics" className="mt-4 flex flex-wrap gap-1">
            {[["market", "Markets"], ["investments", "Investing"], ["watchlist", "Your watchlist"], ["search", "Search"]].map(([key, label]) => <button key={key} role="tab" aria-selected={newsTab === key} className={`rounded-full px-3 py-1.5 text-[13px] font-medium transition-colors duration-150 ${newsTab === key ? "bg-ink text-paper" : "text-ink-soft hover:bg-surface-2"}`} onClick={() => { setNewsTab(key); if (key !== "search") loadNewsFeed(key); }}>{label}</button>)}
          </div>
          {newsTab === "search" && <form className="mt-4 flex flex-col gap-2 sm:flex-row" onSubmit={(event) => { event.preventDefault(); loadNewsFeed("search", newsQuery); }}><label className="relative flex-1"><span className="sr-only">Search news</span><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" /><input className="field-control pl-9" value={newsQuery} onChange={(event) => setNewsQuery(event.target.value)} placeholder="Index funds, RBI, Sensex…" /></label><button className="button-primary" disabled={newsBusy}>Search</button></form>}
          {newsBusy && !articles.length ? <div className="mt-5"><LoadingSkeleton variant="rows" count={4} /></div> : articles.length ? (
            <ul className={`mt-5 divide-y divide-line border-y border-line transition-opacity duration-200 ${newsBusy ? "opacity-60" : ""}`} aria-busy={newsBusy}>
              {articles.map((article, index) => (
                <li key={`${article.articleUrl || article.title}-${index}`} className="grid gap-4 py-5 sm:grid-cols-[minmax(0,1fr)_9rem]">
                  <div className="min-w-0">
                    <p className="text-xs text-muted">{article.source || "Unknown source"} · {article.publishedAt ? formatDate(article.publishedAt) : "time not given"}</p>
                    <h3 className="mt-1 font-display text-[1.2rem] font-medium leading-snug text-ink">{article.articleUrl ? <a href={article.articleUrl} target="_blank" rel="noreferrer" className="hover:underline">{article.title}<span className="sr-only"> (opens in a new tab)</span></a> : article.title}</h3>
                    {article.description && <p className="mt-1.5 line-clamp-3 text-sm leading-6 text-ink-soft">{article.description}</p>}
                  </div>
                  {article.imageUrl && <img src={article.imageUrl} alt="" className="hidden h-24 w-36 rounded-md object-cover sm:block" loading="lazy" referrerPolicy="no-referrer" />}
                </li>
              ))}
            </ul>
          ) : null}
          {showOverview && allArticles.length > articles.length && <Link to="/market-news" className="button-ghost -ml-3 mt-2">All {allArticles.length} articles</Link>}
          {!(newsBusy && !articles.length) && !articles.length && <div className="mt-5"><EmptyState icon={Newspaper} title={statusLabel === "Unavailable" ? "News isn’t available right now" : "No articles for this topic"} description={statusLabel === "Unavailable" ? "FinFlow won’t make up headlines. Try again later, or connect a news provider." : "Try another topic or search."} action={<button type="button" className="button-secondary" onClick={() => loadNewsFeed()} disabled={newsBusy}>Try again</button>} /></div>}
        </section>
      )}

      {(showOverview || tab === "education") && education && (
        <section aria-labelledby="learn-heading">
          <h2 id="learn-heading" className="flex items-center gap-2 font-display text-2xl font-medium text-ink"><BookOpen size={20} className="text-accent" aria-hidden="true" />Learn the basics</h2>
          <p className="mt-1 text-sm text-ink-soft">General ideas with their risk and time horizon. Not advice for your situation.</p>
          <dl className="mt-4 divide-y divide-line border-y border-line">
            {(showOverview ? (education.topics || []).slice(0, 4) : education.topics || []).map((topic) => (
              <div key={topic.key} className="grid gap-1 py-4 sm:grid-cols-[13rem_minmax(0,1fr)] sm:gap-6">
                <dt><p className="text-sm font-semibold text-ink">{topic.title}</p><p className="mt-0.5 text-xs text-muted">{humanize(topic.riskLevel)} risk · {humanize(topic.timeHorizon)}</p></dt>
                <dd className="text-sm leading-6 text-ink-soft">{topic.summary}</dd>
              </div>
            ))}
          </dl>
          {showOverview && (education.topics || []).length > 4 && <Link to="/investment-education" className="button-ghost -ml-3 mt-2">All {education.topics.length} explainers and investment types</Link>}
          {!showOverview && (education.categories || []).length > 0 && (
            <>
              <h3 className="mt-8 text-sm font-semibold text-ink">Kinds of investment</h3>
              <ul className="mt-2 divide-y divide-line border-y border-line">
                {education.categories.map((category) => (
                  <li key={category.name} className="py-3">
                    <p className="text-sm font-medium text-ink">{category.name}</p>
                    <p className="mt-0.5 text-xs text-muted">{humanize(category.riskLevel)} risk · {category.typicalTimeHorizon} · {humanize(category.liquidity)} liquidity</p>
                    <p className="mt-1 text-sm leading-6 text-ink-soft">{category.characteristics}</p>
                  </li>
                ))}
              </ul>
            </>
          )}
          {education.disclaimer && <p className="mt-4 border-l-2 border-warm pl-4 text-[13px] leading-5 text-ink-soft">{education.disclaimer}</p>}
        </section>
      )}

      {(showOverview || tab !== "news") && (
        <section id="watchlist" aria-labelledby="watchlist-heading" className="scroll-mt-6">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 id="watchlist-heading" className="font-display text-2xl font-medium text-ink">Watchlist</h2>
              <p className="mt-1 text-sm text-ink-soft">Things you want to keep an eye on. Prices show only when a provider supplies them.</p>
            </div>
            {watchlist.length > 3 && <label className="relative w-full max-w-xs"><Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted" aria-hidden="true" /><span className="sr-only">Search watchlist</span><input className="field-control min-h-10 pl-9" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Filter" /></label>}
          </div>
          <form onSubmit={addItem} className="mt-4 grid gap-3 rounded-lg border border-line bg-surface p-4 sm:grid-cols-[8rem_minmax(0,1fr)_12rem_auto] sm:items-end">
            <label className="field-label">Symbol<input required maxLength="24" pattern="[A-Za-z0-9._\-]{1,24}" className="field-control uppercase" value={form.symbol} onChange={(event) => setForm({ ...form, symbol: event.target.value })} placeholder="NIFTYBEES" /></label>
            <label className="field-label">Name<input required maxLength="120" className="field-control" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Nifty 50 ETF" /></label>
            <label className="field-label">Type<select className="field-control" value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}>{categories.map((category) => <option key={category} value={category}>{humanize(category)}</option>)}</select></label>
            <button className="button-primary" disabled={busy}><Plus size={16} aria-hidden="true" /> Add</button>
          </form>
          {filteredWatchlist.length ? (
            <ul className="mt-4 divide-y divide-line border-y border-line">
              {filteredWatchlist.map((item) => (
                <li key={item.id} className="flex flex-wrap items-center gap-x-4 gap-y-1 py-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-ink"><span className="tabular">{item.symbol}</span> <span className="font-normal text-ink-soft">{item.name}</span></p>
                    <p className="text-xs text-muted">{humanize(item.category)} · added {formatDate(item.addedAt)}</p>
                  </div>
                  <p className="text-right text-sm">{item.marketDataAvailable && item.latestPrice ? <span className="tabular font-semibold text-ink">{item.latestPrice}{item.dailyChange ? <span className="ml-1 text-xs text-muted">{item.dailyChange}</span> : null}</span> : <span className="text-xs text-muted">Price unavailable</span>}</p>
                  <button className="icon-action hover:text-danger" disabled={busy} aria-label={`Remove ${item.symbol}`} onClick={() => removeItem(item.id)}><Trash2 size={15} /></button>
                </li>
              ))}
            </ul>
          ) : <p className="mt-4 text-sm text-ink-soft">{watchlist.length ? "Nothing matches that filter." : "Nothing here yet. Add a fund or stock symbol above to follow its news."}</p>}
        </section>
      )}
    </div>
  );
}
