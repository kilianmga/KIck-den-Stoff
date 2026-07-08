<script lang="ts">
  import { onMount, tick } from 'svelte';
  import logoUrl from '../KIck den Stoff.png';
  import ModeButton from './lib/ModeButton.svelte';
  import ProgressChart from './lib/ProgressChart.svelte';
  import QuizView from './lib/QuizView.svelte';
  import { sendLearningRequest } from './lib/api';
  import { addProgress, getProgress, updateProgressRecord } from './lib/db';
  import type { ProgressRecord } from './lib/db';
  import type {
    LearningMode,
    LearningRequest,
    LearningResponse,
    ModeDefinition,
    QuizPayload,
    QuizQuestion
  } from './lib/types';

  type ChatMessage = {
    id: string;
    role: 'user' | 'assistant';
    text: string;
    timestamp: number;
    mode?: LearningMode;
    modeLabel?: string;
    title?: string;
    topic?: string;
    quiz?: QuizPayload | null;
    assessment?: string;
    progressId?: string;
    pdfName?: string;
    pending?: boolean;
    error?: boolean;
    quizScore?: number;
    quizTotal?: number;
  };

  const modes: ModeDefinition[] = [
    {
      id: 'explain',
      label: 'Erklären',
      shortLabel: 'Erklären',
      hint: 'einfach und klar'
    },
    {
      id: 'exercises',
      label: 'Üben',
      shortLabel: 'Üben',
      hint: 'Aufgaben mit Lösungen'
    },
    {
      id: 'quiz',
      label: 'Quiz erstellen',
      shortLabel: 'Quiz erstellen',
      hint: '5 Fragen mit Auswertung'
    },
    {
      id: 'correct',
      label: 'Korrigieren',
      shortLabel: 'Korrigieren',
      hint: 'Text verbessern'
    },
    {
      id: 'crash-course',
      label: 'Crashkurs',
      shortLabel: 'Crashkurs',
      hint: 'schnell wiederholen'
    }
  ];

  let selectedMode: LearningMode = 'explain';
  let composerText = '';
  let loading = false;
  let error = '';
  let pdfFile: File | null = null;
  let pdfError = '';
  let pdfInput: HTMLInputElement;
  let messagesContainer: HTMLDivElement;
  let progressRecords: ProgressRecord[] = [];
  let progressTrigger = 0;

  let messages: ChatMessage[] = [
    {
      id: 'welcome',
      role: 'assistant',
      title: 'KIck den Stoff',
      text:
        'Schreib deinen Schulstoff, eine Aufgabe oder eine Frage in die Chatbox. Ich erkenne das Thema automatisch und mache daraus Erklärungen, Übungen, Quizfragen oder einen Crashkurs.',
      timestamp: Date.now(),
      topic: 'Start'
    }
  ];

  $: selectedModeDefinition = modes.find((mode) => mode.id === selectedMode) ?? modes[0];
  $: canSend = Boolean(composerText.trim() || pdfFile) && !loading;
  $: latestAssistant = [...messages].reverse().find((message) => message.role === 'assistant' && !message.pending && !message.error);
  $: overallAssessment = buildOverallAssessment(progressRecords, latestAssistant);

  onMount(() => {
    void refreshProgress();
  });

  function selectMode(mode: LearningMode) {
    selectedMode = mode;
  }

  async function submitMessage() {
    await runMode(selectedMode, composerText);
  }

  async function runMode(mode: LearningMode, text: string) {
    if (loading) return;

    const trimmedText = text.trim();
    const attachedPdf = pdfFile;

    if (!trimmedText && !attachedPdf) {
      error = 'Schreib erst etwas in die Chatbox oder hänge eine PDF an.';
      return;
    }

    selectedMode = mode;
    error = '';
    pdfError = '';
    loading = true;

    const topic = inferTopic(trimmedText, attachedPdf?.name);
    const userMessage: ChatMessage = {
      id: createId(),
      role: 'user',
      text: trimmedText || 'PDF-Material auswerten',
      timestamp: Date.now(),
      mode,
      modeLabel: labelForMode(mode),
      topic,
      pdfName: attachedPdf?.name
    };
    const pendingMessage: ChatMessage = {
      id: createId(),
      role: 'assistant',
      text: 'KIck den Stoff denkt lokal...',
      timestamp: Date.now(),
      mode,
      modeLabel: labelForMode(mode),
      pending: true,
      topic
    };

    messages = [...messages, userMessage, pendingMessage];
    composerText = '';
    clearPdf();
    await scrollChatToBottom();

    const payload: LearningRequest = {
      subject: '',
      grade: '',
      topic,
      inputText: trimmedText,
      userQuestion: instructionForMode(mode),
      mode
    };

    try {
      const response = await sendLearningRequest(mode, payload, attachedPdf);
      const assistantMessage = await buildAssistantMessage(response, mode, topic, pendingMessage.id);
      messages = messages.map((message) =>
        message.id === pendingMessage.id ? assistantMessage : message
      );
      await refreshProgress();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unbekannter Fehler.';
      error = message;
      messages = messages.map((item) =>
        item.id === pendingMessage.id
          ? {
              ...item,
              pending: false,
              error: true,
              title: 'Das hat nicht geklappt',
              text: message
            }
          : item
      );
    } finally {
      loading = false;
      await scrollChatToBottom();
    }
  }

  async function buildAssistantMessage(
    response: LearningResponse,
    mode: LearningMode,
    fallbackTopic: string,
    pendingId: string
  ): Promise<ChatMessage> {
    if (!response.success) {
      return {
        id: pendingId,
        role: 'assistant',
        text: response.error ?? 'LM Studio ist nicht erreichbar. Bitte starte den lokalen Server.',
        timestamp: Date.now(),
        mode,
        modeLabel: labelForMode(mode),
        error: true,
        title: 'LM Studio braucht Aufmerksamkeit',
        topic: fallbackTopic
      };
    }

    const answer = response.answer ?? response.result ?? '';
    const quiz = parseQuizPayload(answer, mode === 'quiz');
    const detectedTopic = sanitizeTopic(quiz?.topic || extractDetectedTopic(answer) || fallbackTopic);
    const assessment = extractAssessment(answer, quiz, mode, detectedTopic);
    const progress = await addProgress({
      mode,
      topic: detectedTopic,
      isQuiz: mode === 'quiz',
      quizScore: null,
      quizTotal: quiz?.questions.length ?? null,
      selfAssessment: selfAssessmentForMode(mode),
      aiAssessment: assessment
    });

    return {
      id: pendingId,
      role: 'assistant',
      text: answer,
      timestamp: Date.now(),
      mode,
      modeLabel: labelForMode(mode),
      title: quiz?.title ?? response.title ?? labelForMode(mode),
      topic: detectedTopic,
      quiz,
      assessment,
      progressId: progress.id
    };
  }

  async function runFollowUp(mode: LearningMode, sourceText: string, instruction: string) {
    composerText = `${instruction}\n\n${stripJsonNoise(sourceText)}`.trim();
    await tick();
    await runMode(mode, composerText);
  }

  async function handleQuizComplete(messageId: string, score: number, total: number) {
    const message = messages.find((item) => item.id === messageId);
    const assessment = scoreAssessment(score, total, message?.topic ?? 'diesem Thema');

    messages = messages.map((item) =>
      item.id === messageId
        ? {
            ...item,
            quizScore: score,
            quizTotal: total,
            assessment
          }
        : item
    );

    if (message?.progressId) {
      await updateProgressRecord(message.progressId, {
        quizScore: score,
        quizTotal: total,
        selfAssessment: `${score} von ${total} Fragen richtig`,
        aiAssessment: assessment
      });
      await refreshProgress();
    }
  }

  function handlePdfChange(event: Event) {
    const input = event.currentTarget as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    pdfError = '';
    pdfFile = null;

    if (!file) {
      return;
    }

    const isPdf = file.type === 'application/pdf' && file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
      input.value = '';
      pdfError = 'Bitte nur PDF-Dateien hochladen. Bilder werden nicht verarbeitet.';
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      input.value = '';
      pdfError = 'Die PDF ist zu groß. Bitte maximal 10 MB hochladen.';
      return;
    }

    pdfFile = file;
  }

  function clearPdf() {
    pdfFile = null;
    pdfError = '';
    if (pdfInput) {
      pdfInput.value = '';
    }
  }

  function handleComposerKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void submitMessage();
    }
  }

  async function refreshProgress() {
    progressRecords = await getProgress();
    progressTrigger += 1;
  }

  async function scrollChatToBottom() {
    await tick();
    if (messagesContainer) {
      messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }
  }

  function parseQuizPayload(text: string, enabled: boolean): QuizPayload | null {
    if (!enabled || !text.trim()) return null;

    try {
      const parsed = JSON.parse(stripJsonNoise(text));
      const rawQuestions = Array.isArray(parsed) ? parsed : parsed.questions;

      if (!Array.isArray(rawQuestions)) {
        return null;
      }

      const questions = rawQuestions
        .map(normalizeQuestion)
        .filter((question): question is QuizQuestion => Boolean(question));

      if (!questions.length) {
        return null;
      }

      const miniEvaluation = normalizeMiniEvaluation(parsed?.miniEvaluation);

      return {
        title: typeof parsed?.title === 'string' ? parsed.title : 'Quiz erstellt',
        topic:
          typeof parsed?.topic === 'string' && parsed.topic.trim()
            ? sanitizeTopic(parsed.topic)
            : '',
        questions,
        miniEvaluation,
        overallAssessment:
          typeof parsed?.overallAssessment === 'string' ? parsed.overallAssessment.trim() : undefined
      };
    } catch {
      return null;
    }
  }

  function normalizeQuestion(raw: unknown): QuizQuestion | null {
    const item = raw as Partial<QuizQuestion>;
    if (!item || typeof item.question !== 'string' || !Array.isArray(item.options)) {
      return null;
    }

    const options = item.options.map((option) => String(option).trim()).filter(Boolean);
    const correctIndex =
      typeof item.correctIndex === 'number' && item.correctIndex >= 0
        ? Math.min(item.correctIndex, Math.max(options.length - 1, 0))
        : 0;

    if (!item.question.trim() || options.length < 2) {
      return null;
    }

    return {
      question: item.question.trim(),
      options,
      correctIndex,
      explanation:
        typeof item.explanation === 'string' && item.explanation.trim()
          ? item.explanation.trim()
          : 'Vergleiche die richtige Antwort noch einmal mit dem Material.'
    };
  }

  function normalizeMiniEvaluation(value: unknown) {
    if (Array.isArray(value)) {
      return value.map((item) => String(item).trim()).filter(Boolean);
    }

    if (value && typeof value === 'object') {
      return Object.values(value).map((item) => String(item).trim()).filter(Boolean);
    }

    return [
      '0-2 richtig: Wiederhole die Grundlagen und lies die Erklärungen zu den falschen Antworten.',
      '3-4 richtig: Solide Basis. Übe gezielt die Fragen, bei denen du geraten hast.',
      '5 richtig: Sehr gut. Versuche als Nächstes eine schwerere Aufgabe.'
    ];
  }

  function stripJsonNoise(text: string) {
    let cleanText = text.trim();
    
    const firstBrace = cleanText.indexOf('{');
    const firstBracket = cleanText.indexOf('[');
    let startIndex = -1;
    
    if (firstBrace !== -1 && firstBracket !== -1) {
      startIndex = Math.min(firstBrace, firstBracket);
    } else if (firstBrace !== -1) {
      startIndex = firstBrace;
    } else {
      startIndex = firstBracket;
    }
    
    if (startIndex !== -1) {
      cleanText = cleanText.slice(startIndex);
    }
    
    const lastBrace = cleanText.lastIndexOf('}');
    const lastBracket = cleanText.lastIndexOf(']');
    let endIndex = -1;
    
    if (lastBrace !== -1 && lastBracket !== -1) {
      endIndex = Math.max(lastBrace, lastBracket);
    } else if (lastBrace !== -1) {
      endIndex = lastBrace;
    } else {
      endIndex = lastBracket;
    }
    
    if (endIndex !== -1) {
      cleanText = cleanText.slice(0, endIndex + 1);
    }

    return cleanText;
  }

  function extractDetectedTopic(text: string) {
    const match = text.match(/Erkanntes Thema:\s*([^\n]+)/i);
    return match?.[1] ?? '';
  }

  function extractAssessment(
    answer: string,
    quiz: QuizPayload | null,
    mode: LearningMode,
    topic: string
  ) {
    if (quiz?.overallAssessment) {
      return quiz.overallAssessment;
    }

    const match = answer.match(/(?:##\s*)?Gesamteinschätzung\s*:?\s*([\s\S]+)/i);
    if (match?.[1]) {
      return match[1].replace(/^[-*\s]+/, '').trim().slice(0, 420);
    }

    if (mode === 'quiz') {
      return `Bearbeite das Quiz zu ${topic}. Danach siehst du, welche Bereiche schon sicher sitzen und was du wiederholen solltest.`;
    }

    return `Du hast gerade an ${topic} gearbeitet. Als Nächstes lohnt sich eine kurze Übung oder ein Quiz dazu.`;
  }

  function buildOverallAssessment(records: ProgressRecord[], latest?: ChatMessage) {
    if (latest?.assessment && (latest.mode === 'quiz' || records.length >= 2)) {
      return latest.assessment;
    }

    if (records.length < 2) {
      return '';
    }

    const recent = [...records].slice(-3);
    const topics = [...new Set(recent.map((record) => record.topic))].join(', ');
    const quiz = [...records]
      .reverse()
      .find((record) => record.quizScore !== null && record.quizTotal !== null);

    if (quiz && quiz.quizScore !== null && quiz.quizTotal !== null) {
      return scoreAssessment(quiz.quizScore, quiz.quizTotal, quiz.topic);
    }

    return `Du hast zuletzt an ${topics} gearbeitet. Gut ist, dass du mehrere Lernaktionen nutzt. Als Nächstes solltest du eines der Themen mit einem Quiz überprüfen.`;
  }

  function scoreAssessment(score: number, total: number, topic: string) {
    const ratio = total ? score / total : 0;

    if (ratio >= 0.8) {
      return `Bei ${topic} bist du schon stark. Bleib dran und übe als Nächstes schwierigere Transferaufgaben.`;
    }

    if (ratio >= 0.5) {
      return `Bei ${topic} sitzt die Grundlage schon, aber es gibt noch Lücken. Wiederhole die falsch beantworteten Fragen und mache danach ein zweites Quiz.`;
    }

    return `Bei ${topic} brauchst du noch Grundlagenarbeit. Starte mit einer einfachen Erklärung und übe anschließend wenige Aufgaben Schritt für Schritt.`;
  }

  function selfAssessmentForMode(mode: LearningMode) {
    if (mode === 'quiz') return 'Quiz erstellt, Ergebnis noch offen';
    if (mode === 'exercises') return 'Übungsphase gestartet';
    if (mode === 'correct') return 'Text überarbeitet';
    if (mode === 'crash-course') return 'Crashkurs bearbeitet';
    return 'Erklärung gelesen';
  }

  function instructionForMode(mode: LearningMode) {
    const instructions: Record<LearningMode, string> = {
      explain: 'Erkläre den Inhalt einfach, mit Beispiel, Merksatz und kurzem nächsten Schritt.',
      exercises: 'Erstelle passende Übungsaufgaben mit Lösungen und kurzen Hinweisen.',
      quiz: 'Erstelle ein Quiz mit mindestens 5 Fragen, Antwortmöglichkeiten, richtiger Lösung, Erklärung und Mini-Auswertung.',
      correct: 'Korrigiere den Text und erkläre kurz, was verbessert wurde.',
      'crash-course': 'Erstelle einen kompakten Crashkurs mit Lernplan, typischen Aufgaben und letzter Wiederholung.',
      chat: 'Beantworte die Frage als Lerncoach.'
    };

    return instructions[mode];
  }

  function labelForMode(mode: LearningMode) {
    return modes.find((item) => item.id === mode)?.label ?? 'Erklären';
  }

  function inferTopic(text: string, pdfName?: string) {
    const firstUsefulLine =
      text
        .split('\n')
        .map((line) => line.trim())
        .find((line) => line.length > 3) ?? '';

    if (firstUsefulLine) {
      return sanitizeTopic(
        firstUsefulLine
          .replace(/^(erkläre|erklaere|korrigiere|erstelle|mach|bitte)\s+/i, '')
          .replace(/[?.!:]+$/g, '')
      );
    }

    if (pdfName) {
      return sanitizeTopic(pdfName.replace(/\.pdf$/i, '').replace(/[-_]/g, ' '));
    }

    return 'Unbekanntes Thema';
  }

  function sanitizeTopic(value: string) {
    const cleaned = String(value || '')
      .replace(/^["'`]+|["'`]+$/g, '')
      .replace(/\s+/g, ' ')
      .trim();

    if (!cleaned) {
      return 'Unbekanntes Thema';
    }

    return cleaned.length > 64 ? `${cleaned.slice(0, 61).trim()}...` : cleaned;
  }

  function createId() {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID();
    }

    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  function formatAnswer(value: string) {
    const cleanValue = value.trim();
    if (!cleanValue) return '';

    const lines = cleanValue.replace(/\r\n/g, '\n').split('\n');
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

      const topic = line.match(/^Erkanntes Thema:\s*(.+)$/i);
      if (topic) {
        closeList();
        html.push(`<p class="detected-topic"><strong>Erkanntes Thema:</strong> ${formatInline(topic[1])}</p>`);
        continue;
      }

      const heading = line.match(/^#{1,3}\s+(.+)$/);
      if (heading) {
        closeList();
        html.push(`<h3>${formatInline(heading[1])}</h3>`);
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

<main class="app-shell">
  <header class="app-header">
    <div class="brand">
      <img src={logoUrl} alt="KIck den Stoff Logo" />
      <div>
        <h1>KIck den Stoff</h1>
        <p>Lokaler KI-Lerncoach</p>
      </div>
    </div>
    <span class="local-badge">LM Studio lokal</span>
  </header>

  <section class="chat-layout">
    <div class="chat-panel" aria-label="Chat mit KIck den Stoff">
      <div bind:this={messagesContainer} class="messages">
        {#each messages as message (message.id)}
          <article class:from-user={message.role === 'user'} class:from-ai={message.role === 'assistant'} class:error-message={message.error} class="message-row">
            <div class="message-bubble">
              <div class="message-meta">
                <span>{message.role === 'user' ? 'Du' : 'KIck den Stoff'}</span>
                {#if message.modeLabel}
                  <b>{message.modeLabel}</b>
                {/if}
              </div>

              {#if message.topic && message.role === 'assistant' && !message.pending}
                <p class="topic-pill">Erkanntes Thema: {message.topic}</p>
              {/if}

              {#if message.pending}
                <div class="typing-state">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              {:else if message.quiz}
                <QuizView
                  title={message.quiz.title}
                  questions={message.quiz.questions}
                  miniEvaluation={message.quiz.miniEvaluation}
                  onComplete={(score, total) => handleQuizComplete(message.id, score, total)}
                />
              {:else}
                <div class="message-content">
                  {@html formatAnswer(message.text)}
                </div>
              {/if}

              {#if message.pdfName}
                <p class="attachment-note">PDF: {message.pdfName}</p>
              {/if}

              {#if message.assessment && message.role === 'assistant' && !message.pending && message.mode === 'quiz'}
                <section class="assessment-card" aria-label="Gesamteinschätzung">
                  <h3>Gesamteinschätzung</h3>
                  <p>{message.assessment}</p>
                </section>
              {/if}

              {#if message.role === 'assistant' && !message.pending && !message.error && !message.quiz}
                <div class="message-actions">
                  <button
                    type="button"
                    on:click={() =>
                      runFollowUp(
                        'explain',
                        message.text,
                        'Erkläre mir das noch einfacher, mit einem Alltagsbeispiel.'
                      )}
                  >
                    Noch einfacher erklären
                  </button>
                  <button
                    type="button"
                    on:click={() =>
                      runFollowUp(
                        'quiz',
                        message.text,
                        'Mach daraus ein Quiz mit mindestens 5 Fragen.'
                      )}
                  >
                    Quiz daraus machen
                  </button>
                </div>
              {/if}
            </div>
          </article>
        {/each}
      </div>

      <form class="composer" on:submit|preventDefault={submitMessage}>
        <div class="mode-grid" aria-label="Lernmodus auswählen">
          {#each modes as mode}
            <ModeButton
              {mode}
              selected={selectedMode === mode.id}
              {loading}
              onSelect={selectMode}
            />
          {/each}
        </div>

        {#if pdfFile || pdfError}
          <div class="file-row">
            {#if pdfFile}
              <span>{pdfFile.name}</span>
              <button type="button" on:click={clearPdf} disabled={loading}>Entfernen</button>
            {/if}
            {#if pdfError}
              <strong>{pdfError}</strong>
            {/if}
          </div>
        {/if}

        <div class="composer-box">
          <input
            bind:this={pdfInput}
            class="hidden-file"
            type="file"
            accept="application/pdf,.pdf"
            disabled={loading}
            on:change={handlePdfChange}
          />
          <button
            class="pdf-button"
            type="button"
            disabled={loading}
            aria-label="PDF anhängen"
            on:click={() => pdfInput?.click()}
          >
            PDF
          </button>
          <textarea
            bind:value={composerText}
            rows="2"
            placeholder="Schreib hier deinen Schulstoff, deine Aufgabe oder deine Frage rein..."
            on:keydown={handleComposerKeydown}
          ></textarea>
          <button class="send-button" type="submit" disabled={!canSend}>
            {loading ? 'Denkt...' : selectedModeDefinition.label}
          </button>
        </div>

        {#if error}
          <p class="composer-error">{error}</p>
        {/if}
      </form>
    </div>

    <aside class="learning-sidebar">
      <ProgressChart refreshTrigger={progressTrigger} />

      <section class="overall-card" aria-labelledby="overall-title">
        <p class="eyebrow">Gesamteinschätzung</p>
        <h2 id="overall-title">Was jetzt dran ist</h2>
        {#if overallAssessment}
          <p>{overallAssessment}</p>
        {:else}
          <p>Nach einem Quiz oder mehreren Lernaktionen erscheint hier eine Einschätzung zu Stärken, Lücken und nächstem Schritt.</p>
        {/if}
      </section>
    </aside>
  </section>
</main>
