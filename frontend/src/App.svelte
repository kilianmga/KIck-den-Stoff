<script lang="ts">
  import ModeButton from './lib/ModeButton.svelte';
  import ResponseBox from './lib/ResponseBox.svelte';
  import ProgressChart from './lib/ProgressChart.svelte';
  import { sendLearningRequest } from './lib/api';
  import { addProgress } from './lib/db';
  import type { LearningMode, LearningRequest, LearningResponse, ModeDefinition } from './lib/types';

  const modes: ModeDefinition[] = [
    {
      id: 'explain',
      label: 'Einfach erklären',
      shortLabel: 'Erklären',
      hint: 'Kurz, klar, mit Beispiel'
    },
    {
      id: 'exercises',
      label: 'Aufgaben erstellen',
      shortLabel: 'Aufgaben',
      hint: 'Üben von leicht bis schwer'
    },
    {
      id: 'quiz',
      label: 'Wissen testen',
      shortLabel: 'Quiz',
      hint: 'Mini-Test mit Lösungen'
    },
    {
      id: 'correct',
      label: 'Text korrigieren',
      shortLabel: 'Korrektur',
      hint: 'Antwort verbessern'
    },
    {
      id: 'crash-course',
      label: 'Klausur-Crashkurs',
      shortLabel: 'Crashkurs',
      hint: 'Plan für die letzte Stunde'
    },
    {
      id: 'chat',
      label: 'Freie Frage',
      shortLabel: 'Frage',
      hint: 'Lerncoach direkt fragen'
    }
  ];

  let request: LearningRequest = {
    subject: 'Mathe',
    grade: '8',
    topic: 'Lineare Funktionen',
    inputText:
      'Eine lineare Funktion hat die Form y = mx + b. m ist die Steigung und b der y-Achsenabschnitt.',
    userQuestion: 'Erklär mir das so, dass ich es vor einer Klassenarbeit verstehe.'
  };

  let selectedMode: LearningMode = 'explain';
  let response: LearningResponse | null = null;
  let loading = false;
  let error = '';
  let progressTrigger = 0;

  const demoInputs = {
    math: {
      subject: 'Mathe',
      grade: '8',
      topic: 'Lineare Funktionen',
      inputText:
        'Eine lineare Funktion hat die Form y = mx + b. m ist die Steigung und b der y-Achsenabschnitt.',
      userQuestion: 'Ich schreibe morgen eine Mathearbeit. Erklär mir m und b mit einem Beispiel.'
    },
    history: {
      subject: 'Geschichte',
      grade: '8',
      topic: 'Französische Revolution',
      inputText:
        'Die Französische Revolution war weil arme Leute kein Brot hatten und der König schlecht war.',
      userQuestion: 'Korrigiere meinen Text und mach ihn genauer.'
    },
    percent: {
      subject: 'Mathe',
      grade: '7',
      topic: 'Prozentrechnung',
      inputText: 'Grundwert, Prozentwert und Prozentsatz berechnen.',
      userQuestion: 'Erstelle Aufgaben mit Lösungen.'
    }
  } satisfies Record<string, LearningRequest>;

  async function runMode(mode: LearningMode) {
    selectedMode = mode;
    loading = true;
    error = '';
    response = null;

    try {
      response = await sendLearningRequest(mode, request);
      await addProgress(mode, request.subject);
      progressTrigger++;
    } catch (err) {
      error = err instanceof Error ? err.message : 'Unbekannter Fehler.';
    } finally {
      loading = false;
    }
  }

  function updateField(field: keyof LearningRequest, value: string) {
    request = {
      ...request,
      [field]: value
    };
  }

  function loadDemo(kind: keyof typeof demoInputs, mode: LearningMode) {
    request = { ...demoInputs[kind] };
    void runMode(mode);
  }

  function simplifyAnswer() {
    updateField('userQuestion', 'Erkläre mir das noch einfacher, mit einem Alltagsbeispiel.');
    void runMode('explain');
  }

  function createQuiz() {
    updateField('userQuestion', 'Mach daraus einen kurzen Wissenstest mit Lösungen am Ende.');
    void runMode('quiz');
  }
</script>

<main class="app-shell">
  <section class="hero-section" aria-labelledby="page-title">
    <div class="brand-mark" aria-hidden="true">
      <span>K</span>
    </div>
    <div class="hero-copy">
      <h1 id="page-title">Kick den Stoff</h1>
    </div>
  </section>

  <section class="workspace">
    <form class="input-panel" on:submit|preventDefault={() => runMode(selectedMode)}>
      <div class="panel-heading">
        <div>
          <p class="panel-kicker">Lernstoff</p>
          <h2>Was willst du verstehen?</h2>
        </div>
        <button class="primary-button compact" type="submit" disabled={loading}>
          {loading ? 'Lädt...' : 'Starten'}
        </button>
      </div>



      <div class="field-grid">
        <label>
          <span>Fach</span>
          <input
            type="text"
            value={request.subject}
            placeholder="Mathe"
            on:input={(event) => updateField('subject', event.currentTarget.value)}
          />
        </label>
        <label>
          <span>Klasse</span>
          <input
            type="text"
            value={request.grade}
            placeholder="8"
            on:input={(event) => updateField('grade', event.currentTarget.value)}
          />
        </label>
      </div>

      <label>
        <span>Thema</span>
        <input
          type="text"
          value={request.topic}
          placeholder="Lineare Funktionen"
          on:input={(event) => updateField('topic', event.currentTarget.value)}
        />
      </label>

      <label>
        <span>Schulstoff oder Material</span>
        <textarea
          rows="7"
          value={request.inputText}
          placeholder="Füge deine Notizen, Aufgabe oder Antwort ein..."
          on:input={(event) => updateField('inputText', event.currentTarget.value)}
        ></textarea>
      </label>

      <label>
        <span>Frage oder Wunsch</span>
        <textarea
          rows="3"
          value={request.userQuestion}
          placeholder="Was soll die KI damit machen?"
          on:input={(event) => updateField('userQuestion', event.currentTarget.value)}
        ></textarea>
      </label>

      <div class="mode-grid" aria-label="Lernmodus auswählen">
        {#each modes as mode}
          <ModeButton
            {mode}
            selected={selectedMode === mode.id}
            {loading}
            onSelect={runMode}
          />
        {/each}
      </div>
    </form>

    <div class="right-column">
      <ProgressChart refreshTrigger={progressTrigger} />
      <ResponseBox
        {loading}
        {response}
        {error}
        onSimplify={simplifyAnswer}
        onQuiz={createQuiz}
      />
    </div>
  </section>
</main>
