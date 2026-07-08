export type LearningMode = 'explain' | 'exercises' | 'quiz' | 'correct' | 'crash-course' | 'chat';

export type LearningRequest = {
  subject?: string;
  grade?: string;
  topic?: string;
  inputText?: string;
  input?: string;
  userQuestion?: string;
  style?: string;
  mode?: string;
};

export type LearningResponse = {
  mode: string;
  title?: string;
  answer: string | null;
  result?: string | null;
  success: boolean;
  error: string | null;
};

export type ModeDefinition = {
  id: LearningMode;
  label: string;
  shortLabel: string;
  hint: string;
};

export type QuizQuestion = {
  question: string;
  options: string[];
  correctIndex: number;
  explanation: string;
};

export type QuizPayload = {
  title: string;
  topic: string;
  questions: QuizQuestion[];
  miniEvaluation: string[];
  overallAssessment?: string;
};
