<script lang="ts">
  import type { QuizQuestion } from './types';

  export let questions: QuizQuestion[] = [];
  export let title = 'Quiz erstellt';
  export let miniEvaluation: string[] = [];
  export let onComplete: (score: number, total: number) => void = () => {};

  let currentIndex = 0;
  let selectedOption: number | null = null;
  let showExplanation = false;
  let score = 0;
  let quizFinished = false;
  let showSolutions = false;
  let completionSent = false;

  $: currentQuestion = questions[currentIndex];
  $: isLastQuestion = currentIndex === questions.length - 1;
  $: percentage = questions.length ? Math.round((score / questions.length) * 100) : 0;
  $: activeEvaluation = pickEvaluation();

  function selectOption(index: number) {
    if (showExplanation || quizFinished) return;
    selectedOption = index;
    showExplanation = true;

    if (index === currentQuestion.correctIndex) {
      score += 1;
    }
  }

  function nextQuestion() {
    if (isLastQuestion) {
      finishQuiz();
      return;
    }

    currentIndex += 1;
    selectedOption = null;
    showExplanation = false;
  }

  function finishQuiz() {
    quizFinished = true;
    showSolutions = true;

    if (!completionSent) {
      completionSent = true;
      onComplete(score, questions.length);
    }
  }

  function restartQuiz() {
    currentIndex = 0;
    selectedOption = null;
    showExplanation = false;
    score = 0;
    quizFinished = false;
    showSolutions = false;
    completionSent = false;
  }

  function pickEvaluation() {
    if (!miniEvaluation.length || !questions.length) {
      if (percentage >= 80) return 'Stark: Du hast das Thema schon gut im Griff.';
      if (percentage >= 50) return 'Gute Basis: Wiederhole die unsicheren Fragen noch einmal.';
      return 'Da ist noch Übung nötig: Starte mit den Grundlagen und mache danach ein neues Quiz.';
    }

    if (percentage >= 80) return miniEvaluation[2] ?? miniEvaluation.at(-1) ?? miniEvaluation[0];
    if (percentage >= 50) return miniEvaluation[1] ?? miniEvaluation[0];
    return miniEvaluation[0];
  }
</script>

<section class="quiz-card" aria-labelledby="quiz-title">
  <div class="quiz-title-row">
    <div>
      <p>Quiz erstellen</p>
      <h3 id="quiz-title">{title}</h3>
    </div>
    <span>{questions.length} Fragen</span>
  </div>

  {#if quizFinished}
    <div class="quiz-result">
      <strong>{score} von {questions.length} richtig</strong>
      <div class="progress-bar-bg" aria-hidden="true">
        <div class="progress-bar-fill" style="width: {percentage}%"></div>
      </div>
      <p>{activeEvaluation}</p>
      <button class="primary-button" type="button" on:click={restartQuiz}>Nochmal üben</button>
    </div>
  {:else if currentQuestion}
    <div class="quiz-header">
      <span>Frage {currentIndex + 1} von {questions.length}</span>
      <span>Punkte: {score}</span>
    </div>

    <h4 class="question-text">{currentQuestion.question}</h4>

    <div class="options-grid">
      {#each currentQuestion.options as option, index}
        {@const isSelected = selectedOption === index}
        {@const isCorrect = index === currentQuestion.correctIndex}
        {@const isWrong = isSelected && !isCorrect}

        <button
          class="option-button"
          class:selected={isSelected}
          class:correct={showExplanation && isCorrect}
          class:wrong={showExplanation && isWrong}
          disabled={showExplanation}
          type="button"
          on:click={() => selectOption(index)}
        >
          <span class="option-letter">{String.fromCharCode(65 + index)}</span>
          <span class="option-text">{option}</span>
          {#if showExplanation && isCorrect}
            <b>Richtig</b>
          {:else if showExplanation && isWrong}
            <b>Falsch</b>
          {/if}
        </button>
      {/each}
    </div>

    {#if showExplanation}
      <div
        class="explanation-box"
        class:correct-box={selectedOption === currentQuestion.correctIndex}
        class:wrong-box={selectedOption !== currentQuestion.correctIndex}
      >
        <strong>
          {selectedOption === currentQuestion.correctIndex ? 'Richtig gelöst' : 'Nicht ganz'}
        </strong>
        <p>{currentQuestion.explanation}</p>
      </div>

      <button class="primary-button next-button" type="button" on:click={nextQuestion}>
        {isLastQuestion ? 'Ergebnis anzeigen' : 'Nächste Frage'}
      </button>
    {/if}
  {:else}
    <p class="quiz-empty">Keine Quizfragen gefunden.</p>
  {/if}

  {#if questions.length}
    <div class="solution-toggle">
      <button class="ghost-button" type="button" on:click={() => (showSolutions = !showSolutions)}>
        {showSolutions ? 'Lösungen ausblenden' : 'Lösungen anzeigen'}
      </button>
    </div>

    {#if showSolutions}
      <ol class="solution-list">
        {#each questions as question, index}
          <li>
            <strong>{index + 1}. {question.options[question.correctIndex] ?? 'Lösung prüfen'}</strong>
            <span>{question.explanation}</span>
          </li>
        {/each}
      </ol>
    {/if}
  {/if}
</section>

<style>
  .quiz-card {
    display: grid;
    gap: 18px;
    border: 2px solid #f5b400;
    border-radius: 8px;
    background: #fff9e7;
    padding: 18px;
    box-shadow: 0 18px 36px rgba(118, 83, 13, 0.13);
  }

  .quiz-title-row,
  .quiz-header {
    display: flex;
    justify-content: space-between;
    gap: 14px;
    align-items: flex-start;
  }

  .quiz-title-row p,
  .quiz-title-row h3,
  .quiz-header span,
  .question-text,
  .quiz-result p {
    margin: 0;
  }

  .quiz-title-row p {
    color: #76530d;
    font-size: 0.76rem;
    font-weight: 900;
    letter-spacing: 0.08em;
    text-transform: uppercase;
  }

  .quiz-title-row h3 {
    color: var(--ink);
    font-size: 1.3rem;
    line-height: 1.15;
  }

  .quiz-title-row > span {
    flex: 0 0 auto;
    border-radius: 999px;
    background: #ffe39a;
    color: #76530d;
    padding: 6px 10px;
    font-size: 0.8rem;
    font-weight: 900;
  }

  .quiz-header {
    color: var(--muted);
    font-size: 0.88rem;
    font-weight: 800;
  }

  .question-text {
    color: var(--ink);
    font-size: 1.08rem;
    line-height: 1.45;
  }

  .options-grid {
    display: grid;
    gap: 10px;
  }

  .option-button {
    display: grid;
    grid-template-columns: 32px minmax(0, 1fr) auto;
    align-items: center;
    gap: 10px;
    width: 100%;
    border: 1px solid #e2c46d;
    border-radius: 8px;
    background: #fffdf6;
    color: var(--ink);
    padding: 12px;
    text-align: left;
    transition: border-color 160ms ease, transform 160ms ease, background-color 160ms ease;
  }

  .option-button:hover:not(:disabled) {
    border-color: #b88400;
    transform: translateY(-1px);
  }

  .option-button:disabled {
    opacity: 1;
  }

  .option-letter {
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    border-radius: 999px;
    background: #ffe39a;
    color: #76530d;
    font-weight: 900;
  }

  .option-text {
    min-width: 0;
    overflow-wrap: anywhere;
  }

  .option-button b {
    color: inherit;
    font-size: 0.78rem;
  }

  .option-button.correct {
    border-color: #16a067;
    background: #e9fbf2;
  }

  .option-button.correct .option-letter {
    background: #16a067;
    color: white;
  }

  .option-button.wrong {
    border-color: #d94b4b;
    background: #fff0ef;
  }

  .option-button.wrong .option-letter {
    background: #d94b4b;
    color: white;
  }

  .explanation-box {
    display: grid;
    gap: 5px;
    border-left: 4px solid #16a067;
    border-radius: 8px;
    background: #fffdf6;
    padding: 12px;
  }

  .explanation-box p {
    margin: 0;
    color: var(--muted);
    line-height: 1.5;
  }

  .wrong-box {
    border-left-color: #d94b4b;
  }

  .next-button {
    justify-self: end;
  }

  .quiz-result {
    display: grid;
    gap: 12px;
  }

  .quiz-result strong {
    color: var(--ink);
    font-size: 1.3rem;
  }

  .quiz-result p {
    color: var(--muted);
    line-height: 1.5;
  }

  .progress-bar-bg {
    height: 12px;
    overflow: hidden;
    border-radius: 999px;
    background: #f3df9d;
  }

  .progress-bar-fill {
    height: 100%;
    border-radius: inherit;
    background: var(--accent);
    transition: width 420ms ease;
  }

  .solution-toggle {
    display: flex;
    justify-content: flex-start;
  }

  .solution-list {
    display: grid;
    gap: 10px;
    margin: 0;
    padding-left: 1.2rem;
  }

  .solution-list li {
    padding-left: 2px;
  }

  .solution-list strong,
  .solution-list span {
    display: block;
  }

  .solution-list strong {
    color: var(--ink);
  }

  .solution-list span,
  .quiz-empty {
    color: var(--muted);
    line-height: 1.5;
  }

  @media (max-width: 560px) {
    .quiz-title-row,
    .quiz-header {
      flex-direction: column;
    }

    .option-button {
      grid-template-columns: 30px minmax(0, 1fr);
    }

    .option-button b {
      grid-column: 2;
    }
  }
</style>
