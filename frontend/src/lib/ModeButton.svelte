<script lang="ts">
  import type { LearningMode, ModeDefinition } from './types';

  export let mode: ModeDefinition;
  export let selected = false;
  export let loading = false;
  export let onSelect: (mode: LearningMode) => void;
</script>

<button
  class:selected
  class="mode-button"
  type="button"
  disabled={loading}
  aria-pressed={selected}
  on:click={() => onSelect(mode.id)}
>
  <span>{mode.label}</span>
  <small>{mode.hint}</small>
</button>
