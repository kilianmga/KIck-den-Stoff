<script lang="ts">
  import type { QuizQuestion } from './types';

  export let questions: QuizQuestion[] = [];

  let currentIndex = 0;
  let selectedOption: number | null = null;
  let showExplanation = false;
  let score = 0;
  let quizFinished = false;

  $: currentQuestion = questions[currentIndex];
  $: isLastQuestion = currentIndex === questions.length - 1;

  function selectOption(index: number) {
    if (showExplanation) return;
    selectedOption = index;
    showExplanation = true;

    if (index === currentQuestion.correctIndex) {
      score++;
    }
  }

  function nextQuestion() {
    if (isLastQuestion) {
      quizFinished = true;
    } else {
      currentIndex++;
      selectedOption = null;
      showExplanation = false;
    }
  }

  function restartQuiz() {
    currentIndex = 0;
    selectedOption = null;
    showExplanation = false;
    score = 0;
    quizFinished = false;
  }
</script>

<div class="quiz-container">
  {#if quizFinished}
    <div class="quiz-result">
      <h3>Quiz beendet!</h3>
      <p class="score">Du hast {score} von {questions.length} Fragen richtig beantwortet.</p>
      
      <div class="progress-bar-bg">
        <div 
          class="progress-bar-fill" 
          style="width: {(score / questions.length) * 100}%"
        ></div>
      </div>

      <button class="primary-button" on:click={restartQuiz}>Nochmal spielen</button>
    </div>
  {:else if currentQuestion}
    <div class="quiz-header">
      <span class="quiz-progress">Frage {currentIndex + 1} von {questions.length}</span>
      <span class="quiz-score">Punkte: {score}</span>
    </div>

    <h3 class="question-text">{currentQuestion.question}</h3>

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
          on:click={() => selectOption(index)}
        >
          <span class="option-letter">{String.fromCharCode(65 + index)}</span>
          <span class="option-text">{option}</span>
          
          {#if showExplanation}
            {#if isCorrect}
              <span class="icon correct-icon">✓</span>
            {:else if isWrong}
              <span class="icon wrong-icon">✗</span>
            {/if}
          {/if}
        </button>
      {/each}
    </div>

    {#if showExplanation}
      <div class="explanation-box" class:correct-box={selectedOption === currentQuestion.correctIndex} class:wrong-box={selectedOption !== currentQuestion.correctIndex}>
        <h4>{selectedOption === currentQuestion.correctIndex ? 'Richtig!' : 'Falsch!'}</h4>
        <p>{currentQuestion.explanation}</p>
      </div>

      <button class="primary-button next-button" on:click={nextQuestion}>
        {isLastQuestion ? 'Ergebnis anzeigen' : 'Nächste Frage'}
      </button>
    {/if}
  {:else}
    <div class="error-state">
      <p>Keine Fragen gefunden.</p>
    </div>
  {/if}
</div>

<style>
  .quiz-container {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }

  .quiz-header {
    display: flex;
    justify-content: space-between;
    font-size: 0.9rem;
    color: var(--text-muted);
    font-weight: 500;
  }

  .question-text {
    margin: 0;
    font-size: 1.25rem;
    line-height: 1.4;
  }

  .options-grid {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .option-button {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
    background: var(--surface-2);
    border: 2px solid transparent;
    border-radius: 12px;
    cursor: pointer;
    text-align: left;
    transition: all 0.2s;
    font-size: 1rem;
    color: var(--text-primary);
  }

  .option-button:hover:not(:disabled) {
    background: var(--surface-3);
    transform: translateY(-2px);
  }

  .option-button:disabled {
    cursor: default;
  }

  .option-letter {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: var(--surface-1);
    border-radius: 50%;
    font-weight: 600;
    font-size: 0.9rem;
  }

  .option-text {
    flex: 1;
  }

  .icon {
    font-weight: bold;
    font-size: 1.2rem;
  }

  .correct-icon {
    color: #10b981;
  }

  .wrong-icon {
    color: #ef4444;
  }

  /* Status Colors */
  .option-button.correct {
    background: rgba(16, 185, 129, 0.1);
    border-color: #10b981;
  }

  .option-button.correct .option-letter {
    background: #10b981;
    color: white;
  }

  .option-button.wrong {
    background: rgba(239, 68, 68, 0.1);
    border-color: #ef4444;
  }

  .option-button.wrong .option-letter {
    background: #ef4444;
    color: white;
  }

  .explanation-box {
    padding: 1.25rem;
    border-radius: 12px;
    background: var(--surface-2);
    animation: slideIn 0.3s ease-out;
  }

  .explanation-box h4 {
    margin: 0 0 0.5rem 0;
    font-size: 1.1rem;
  }

  .explanation-box p {
    margin: 0;
    color: var(--text-secondary);
    line-height: 1.5;
  }

  .correct-box {
    border-left: 4px solid #10b981;
  }
  .correct-box h4 {
    color: #10b981;
  }

  .wrong-box {
    border-left: 4px solid #ef4444;
  }
  .wrong-box h4 {
    color: #ef4444;
  }

  .next-button {
    align-self: flex-end;
    margin-top: 0.5rem;
  }

  .quiz-result {
    text-align: center;
    padding: 2rem 1rem;
  }

  .quiz-result h3 {
    font-size: 1.5rem;
    margin-bottom: 0.5rem;
  }

  .quiz-result .score {
    font-size: 1.2rem;
    color: var(--text-secondary);
    margin-bottom: 2rem;
  }

  .progress-bar-bg {
    height: 12px;
    background: var(--surface-3);
    border-radius: 6px;
    overflow: hidden;
    margin-bottom: 2rem;
  }

  .progress-bar-fill {
    height: 100%;
    background: var(--accent-primary);
    transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
  }

  @keyframes slideIn {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
</style>
