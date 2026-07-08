<script lang="ts">
  import type { LearningResponse, QuizQuestion } from './types';
  import QuizView from './QuizView.svelte';

  export let loading = false;
  export let response: LearningResponse | null = null;
  export let error = '';
  export let onSimplify: () => void;
  export let onQuiz: () => void;

  let copied = false;

  $: answer = response?.answer ?? response?.result ?? '';
  $: hasAnswer = Boolean(answer && response?.success);
  $: formattedAnswer = formatAnswer(answer);

  // Parse quiz if the mode was quiz
  $: isQuizMode = response?.mode === 'quiz' || response?.mode === 'QUIZ';
  $: quizData = parseQuizData(answer, isQuizMode);

  function parseQuizData(text: string, isQuiz: boolean): QuizQuestion[] | null {
    if (!isQuiz || !text) return null;
    
    // Sometimes the LLM wraps JSON in markdown blocks
    let cleanText = text.trim();
    if (cleanText.startsWith('```json')) {
      cleanText = cleanText.substring(7);
    } else if (cleanText.startsWith('```')) {
      cleanText = cleanText.substring(3);
    }
    if (cleanText.endsWith('```')) {
      cleanText = cleanText.substring(0, cleanText.length - 3);
    }

    try {
      const parsed = JSON.parse(cleanText);
      if (Array.isArray(parsed) && parsed.length > 0 && 'question' in parsed[0]) {
        return parsed as QuizQuestion[];
      }
    } catch (e) {
      console.warn('Failed to parse Quiz JSON:', e);
    }
    return null;
  }

  async function copyAnswer() {
    if (!answer) return;
    await navigator.clipboard.writeText(answer);
    copied = true;
    window.setTimeout(() => {
      copied = false;
    }, 1600);
  }

  function formatAnswer(value: string) {
    if (!value.trim()) return '';

    const lines = value.trim().replace(/\r\n/g, '\n').split('\n');
    const html: string[] = [];
    let listOpen = false;

    const closeList = () => {
      if (listOpen) {
        html.push('</ul>');
        listOpen = false;
      }
    };

    for (const rawLine of lines) {
      const line = rawLine.trim();

      if (!line) {
        closeList();
        continue;
      }

      const markdownHeading = line.match(/^#{1,3}\s+(.+)$/);
      if (markdownHeading) {
        closeList();
        html.push(`<h3>${formatInline(markdownHeading[1])}</h3>`);
        continue;
      }

      const numberedHeading = line.match(/^\d+[.)]\s+([^:.?]+[.:?])\s*(.*)$/);
      if (numberedHeading) {
        closeList();
        html.push(`<h3>${formatInline(numberedHeading[1].replace(/[.:?]$/, ''))}</h3>`);
        if (numberedHeading[2]) {
          html.push(`<p>${formatInline(numberedHeading[2])}</p>`);
        }
        continue;
      }

      const bullet = line.match(/^[-*•]\s+(.+)$/);
      if (bullet) {
        if (!listOpen) {
          html.push('<ul>');
          listOpen = true;
        }
        html.push(`<li>${formatInline(bullet[1])}</li>`);
        continue;
      }

      closeList();
      html.push(`<p>${formatInline(line)}</p>`);
    }

    closeList();
    return html.join('');
  }

  function formatInline(value: string) {
    return escapeHtml(value)
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/`([^`]+)`/g, '<code>$1</code>');
  }

  function escapeHtml(value: string) {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
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
    {#if isQuizMode && quizData}
      <QuizView questions={quizData} />
    {:else}
      <article class="answer-card">
        <div class="answer-content">
          {@html formattedAnswer}
        </div>
      </article>
      <div class="response-actions">
        <button type="button" on:click={onSimplify}>Noch einfacher erklären</button>
        <button type="button" on:click={onQuiz}>Quiz daraus machen</button>
      </div>
    {/if}
  {:else}
    <div class="empty-state">
      <p>Wähle einen Lernmodus. Die Antwort erscheint hier als Lernkarte.</p>
    </div>
  {/if}
</section>
