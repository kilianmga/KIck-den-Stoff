<script lang="ts">
  import { onMount } from 'svelte';
  import { deleteProgressRecord, getProgress } from './db';
  import type { ProgressRecord } from './db';

  export let refreshTrigger = 0;
  export let onOpenSession: (record: ProgressRecord) => void = () => {};
  export let onDeleteSession: (id: string) => void = () => {};

  let records: ProgressRecord[] = [];
  let days: { label: string; count: number; dateStr: string }[] = [];
  let maxCount = 1;

  $: recentRecords = [...records].sort((a, b) => b.timestamp - a.timestamp).slice(0, 5);
  $: quizRecords = records.filter((record) => record.isQuiz);
  $: scoredQuizzes = quizRecords.filter(
    (record) => record.quizScore !== null && record.quizTotal !== null && record.quizTotal > 0
  );
  $: averageQuizScore = calculateAverageScore(scoredQuizzes);

  async function loadData() {
    records = await getProgress();
    processChartData();
  }

  function processChartData() {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const newDays = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);

      const label = d.toLocaleDateString('de-DE', { weekday: 'short' });
      const dateStr = d.toISOString().split('T')[0];
      newDays.push({ label, count: 0, dateStr });
    }

    for (const record of records) {
      const rDate = new Date(record.timestamp);
      rDate.setHours(0, 0, 0, 0);
      const rDateStr = rDate.toISOString().split('T')[0];

      const dayObj = newDays.find((day) => day.dateStr === rDateStr);
      if (dayObj) {
        dayObj.count++;
      }
    }

    days = newDays;
    maxCount = Math.max(1, ...days.map((day) => day.count));
  }

  function calculateAverageScore(items: ProgressRecord[]) {
    if (!items.length) {
      return null;
    }

    const ratios = items.map((record) => (record.quizScore ?? 0) / (record.quizTotal || 1));
    const average = ratios.reduce((sum, ratio) => sum + ratio, 0) / ratios.length;
    return Math.round(average * 100);
  }

  function modeLabel(mode: string) {
    const labels: Record<string, string> = {
      explain: 'Erklären',
      exercises: 'Üben',
      quiz: 'Quiz',
      correct: 'Korrigieren',
      'crash-course': 'Crashkurs',
      chat: 'Frage'
    };

    return labels[mode] ?? mode;
  }

  async function deleteSession(record: ProgressRecord) {
    const shouldDelete = window.confirm(`"${record.topic}" aus der Lernkurve löschen?`);
    if (!shouldDelete) {
      return;
    }

    await deleteProgressRecord(record.id);
    onDeleteSession(record.id);
    await loadData();
  }

  $: if (refreshTrigger !== undefined) {
    void loadData();
  }

  onMount(() => {
    void loadData();
  });
</script>

<section class="progress-card" aria-labelledby="progress-title">
  <div class="progress-header">
    <div>
      <p class="eyebrow">Lernkurve</p>
      <h2 id="progress-title">Fortschritt</h2>
    </div>
    <span class="session-pill">{records.length} Sessions</span>
  </div>

  <div class="metric-grid">
    <div>
      <span class="metric-value">{quizRecords.length}</span>
      <span class="metric-label">Quiz</span>
    </div>
    <div>
      <span class="metric-value">{averageQuizScore === null ? '-' : `${averageQuizScore}%`}</span>
      <span class="metric-label">Ø Score</span>
    </div>
  </div>

  <div class="chart-area" aria-label="Lernaktionen der letzten 7 Tage">
    {#each days as day}
      {@const heightPercent = (day.count / maxCount) * 100}
      <div class="bar-container">
        <div class="bar-value" class:active={day.count > 0} style="height: {heightPercent}%">
          <span>{day.count}</span>
        </div>
        <span class="bar-label">{day.label}</span>
      </div>
    {/each}
  </div>

  <div class="recent-topics">
    <h3>Zuletzt geübt</h3>
    {#if recentRecords.length}
      <ul>
        {#each recentRecords as record}
          <li>
            <div>
              <strong>{record.topic}</strong>
              <span>{modeLabel(record.mode)} · {new Date(record.timestamp).toLocaleString('de-DE')}</span>
            </div>
            <div class="session-actions">
              {#if record.isQuiz && record.quizScore !== null && record.quizTotal !== null}
                <b>{record.quizScore}/{record.quizTotal}</b>
              {/if}
              <button type="button" on:click={() => onOpenSession(record)}>Öffnen</button>
              <button
                class="danger-action"
                type="button"
                aria-label={`${record.topic} löschen`}
                on:click={() => deleteSession(record)}
              >
                Löschen
              </button>
            </div>
          </li>
        {/each}
      </ul>
    {:else}
      <p>Noch keine Sessions gespeichert.</p>
    {/if}
  </div>
</section>

<style>
  .progress-card {
    display: grid;
    gap: 18px;
    border: 1px solid var(--line);
    border-radius: 8px;
    background: var(--surface);
    padding: 18px;
  }

  .progress-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 14px;
  }

  .eyebrow {
    margin: 0 0 4px;
    color: var(--accent-strong);
    font-size: 0.74rem;
    font-weight: 850;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  h2,
  h3,
  p {
    margin: 0;
  }

  h2 {
    color: var(--ink);
    font-size: 1.2rem;
    line-height: 1.1;
  }

  h3 {
    color: var(--ink);
    font-size: 0.95rem;
  }

  .session-pill {
    flex: 0 0 auto;
    border-radius: 999px;
    background: var(--accent-soft);
    color: var(--accent-strong);
    padding: 6px 10px;
    font-size: 0.82rem;
    font-weight: 850;
  }

  .metric-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .metric-grid div {
    display: grid;
    gap: 2px;
    border-radius: 8px;
    background: var(--surface-2);
    padding: 12px;
  }

  .metric-value {
    color: var(--ink);
    font-size: 1.35rem;
    font-weight: 900;
  }

  .metric-label {
    color: var(--muted);
    font-size: 0.8rem;
    font-weight: 750;
  }

  .chart-area {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    height: 132px;
    gap: 8px;
    border-bottom: 1px solid var(--line);
    padding-top: 14px;
    padding-bottom: 8px;
  }

  .bar-container {
    display: grid;
    align-items: end;
    justify-items: center;
    height: 100%;
    min-width: 0;
    flex: 1;
    gap: 8px;
  }

  .bar-value {
    display: grid;
    align-items: start;
    justify-items: center;
    width: 100%;
    max-width: 28px;
    min-height: 6px;
    border-radius: 6px 6px 2px 2px;
    background: var(--surface-3);
    color: transparent;
    transition: height 240ms ease, background-color 160ms ease;
  }

  .bar-value.active {
    background: var(--accent);
    color: var(--ink);
  }

  .bar-value span {
    transform: translateY(-20px);
    color: var(--muted);
    font-size: 0.72rem;
    font-weight: 850;
  }

  .bar-label {
    color: var(--muted);
    font-size: 0.72rem;
    font-weight: 800;
    text-transform: uppercase;
  }

  .recent-topics {
    display: grid;
    gap: 10px;
  }

  .recent-topics ul {
    display: grid;
    gap: 8px;
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .recent-topics li {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    border-radius: 8px;
    background: var(--surface-2);
    padding: 10px;
  }

  .recent-topics li div {
    display: grid;
    gap: 2px;
    min-width: 0;
  }

  .session-actions {
    display: flex !important;
    flex: 0 0 auto;
    align-items: center;
    gap: 6px !important;
  }

  .recent-topics strong {
    overflow: hidden;
    color: var(--ink);
    font-size: 0.9rem;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .recent-topics span,
  .recent-topics p {
    color: var(--muted);
    font-size: 0.78rem;
    line-height: 1.35;
  }

  .recent-topics b {
    flex: 0 0 auto;
    border-radius: 999px;
    background: #fff1c7;
    color: #76530d;
    padding: 5px 8px;
    font-size: 0.78rem;
  }

  .session-actions button {
    border: 1px solid var(--line);
    border-radius: 8px;
    background: var(--surface);
    color: var(--accent-strong);
    padding: 5px 8px;
    font-size: 0.78rem;
    font-weight: 850;
  }

  .session-actions button:hover {
    border-color: var(--accent);
    background: var(--accent-soft);
  }

  .session-actions .danger-action {
    color: var(--danger);
  }

  .session-actions .danger-action:hover {
    border-color: rgba(196, 63, 63, 0.35);
    background: var(--danger-soft);
  }

  @media (max-width: 520px) {
    .recent-topics li {
      flex-direction: column;
    }

    .session-actions {
      flex-wrap: wrap;
      width: 100%;
    }
  }
</style>
