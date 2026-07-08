<script lang="ts">
  import { onMount } from 'svelte';
  import { getProgressLast7Days } from './db';
  import type { ProgressRecord } from './db';

  export let refreshTrigger = 0;

  let records: ProgressRecord[] = [];
  let days: { label: string; count: number; dateStr: string }[] = [];
  let maxCount = 1;

  async function loadData() {
    records = await getProgressLast7Days();
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
      
      const dayObj = newDays.find(d => d.dateStr === rDateStr);
      if (dayObj) {
        dayObj.count++;
      }
    }

    days = newDays;
    maxCount = Math.max(1, ...days.map(d => d.count));
  }

  $: if (refreshTrigger !== undefined) {
    loadData();
  }

  onMount(() => {
    loadData();
  });
</script>

<div class="chart-card">
  <div class="chart-header">
    <h3>Lernkurve</h3>
    <span class="total-badge">
      {records.length} {records.length === 1 ? 'Lernkarte' : 'Lernkarten'}
    </span>
  </div>

  <div class="chart-area">
    {#each days as day}
      {@const heightPercent = (day.count / maxCount) * 100}
      <div class="bar-container">
        <div class="bar-value" class:active={day.count > 0} style="height: {heightPercent}%">
          <div class="tooltip">{day.count}</div>
        </div>
        <span class="bar-label">{day.label}</span>
      </div>
    {/each}
  </div>
</div>

<style>
  .chart-card {
    background: rgba(251, 255, 251, 0.96);
    border: 2px solid var(--ink);
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 22px;
    box-shadow: var(--shadow);
    backdrop-filter: blur(8px);
  }

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .chart-header h3 {
    margin: 0;
    font-size: 1.15rem;
    font-weight: 850;
    color: var(--ink);
  }

  .total-badge {
    background: var(--accent-soft);
    color: var(--accent-strong);
    padding: 4px 10px;
    border-radius: 999px;
    font-size: 0.8rem;
    font-weight: 800;
  }

  .chart-area {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    height: 140px;
    padding-top: 20px;
    border-bottom: 2px solid var(--line);
    padding-bottom: 8px;
  }

  .bar-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    height: 100%;
    width: 100%;
    gap: 8px;
  }

  .bar-value {
    width: 24px;
    background: var(--line);
    border-radius: 4px;
    transition: height 0.6s cubic-bezier(0.2, 0.8, 0.2, 1), background-color 0.3s ease;
    position: relative;
    cursor: pointer;
    min-height: 4px;
  }

  .bar-value.active {
    background: var(--accent);
    box-shadow: 0 4px 12px rgba(40, 185, 109, 0.3);
  }

  .bar-value:hover {
    background: var(--accent-strong);
  }

  .tooltip {
    position: absolute;
    top: -30px;
    left: 50%;
    transform: translateX(-50%) scale(0.9);
    background: var(--ink);
    color: white;
    padding: 4px 8px;
    border-radius: 6px;
    font-size: 0.75rem;
    font-weight: 800;
    opacity: 0;
    pointer-events: none;
    transition: all 0.2s ease;
  }
  
  .tooltip::after {
    content: '';
    position: absolute;
    bottom: -4px;
    left: 50%;
    transform: translateX(-50%);
    border-width: 4px 4px 0;
    border-style: solid;
    border-color: var(--ink) transparent transparent transparent;
  }

  .bar-value:hover .tooltip {
    opacity: 1;
    transform: translateX(-50%) scale(1);
    top: -34px;
  }

  .bar-label {
    font-size: 0.75rem;
    color: var(--muted);
    font-weight: 700;
    text-transform: uppercase;
  }
</style>
