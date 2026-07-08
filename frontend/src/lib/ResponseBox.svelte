<script lang="ts">
  import type { LearningResponse } from './types';

  export let loading = false;
  export let response: LearningResponse | null = null;
  export let error = '';
  export let onSimplify: () => void;
  export let onQuiz: () => void;

  let copied = false;

  $: answer = response?.answer ?? response?.result ?? '';
  $: hasAnswer = Boolean(answer && response?.success);

  async function copyAnswer() {
    if (!answer) return;
    await navigator.clipboard.writeText(answer);
    copied = true;
    window.setTimeout(() => {
      copied = false;
    }, 1600);
  }
</script>

<section class="response-panel" aria-live="polite">
  <div class="panel-heading">
    <div>
      <p class="panel-kicker">KI-Antwort</p>
      <h2>{response?.title ?? 'Lernkarte'}</h2>
    </div>
    {#if hasAnswer}
      <button class="ghost-button" type="button" on:click={copyAnswer}>
        {copied ? 'Kopiert' : 'Kopieren'}
      </button>
    {/if}
  </div>

  {#if loading}
    <div class="loading-state">
      <span class="pulse"></span>
      <p>KI denkt lokal...</p>
    </div>
  {:else if error}
    <div class="error-state">
      <strong>Das hat nicht geklappt.</strong>
      <p>{error}</p>
    </div>
  {:else if response && !response.success}
    <div class="error-state">
      <strong>LM Studio braucht Aufmerksamkeit.</strong>
      <p>{response.error ?? 'LM Studio ist nicht erreichbar. Bitte Modell laden und Local Server starten.'}</p>
    </div>
  {:else if hasAnswer}
    <article class="answer-card">
      <pre>{answer}</pre>
    </article>
    <div class="response-actions">
      <button type="button" on:click={onSimplify}>Noch einfacher erklären</button>
      <button type="button" on:click={onQuiz}>Quiz daraus machen</button>
    </div>
  {:else}
    <div class="empty-state">
      <p>Wähle einen Lernmodus. Die Antwort erscheint hier als Lernkarte.</p>
    </div>
  {/if}
</section>
