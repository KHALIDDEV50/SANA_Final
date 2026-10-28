// Conservative routing only. This helper never sends requests or changes a graph.
export const isCreativityDomain = domain => ['writing', 'games', 'creativity'].includes(String(domain || '').toLowerCase());


function normalize(text) {
  return String(text || '').toLowerCase().replace(/[\u064b-\u065f\u0670\u0640]/g, '')
    .replace(/[أإآ]/g, 'ا').replace(/ة/g, 'ه').replace(/\s+/g, ' ').trim();
}

const BOARD = '(?:\\bmood\\s*boards?\\b|لوحه\\s+(?:ال)?الهام|مود\\s*بورد)';
const ACTION = '(?:\\b(?:create|generate|make|build|design)\\b|انشئ(?:ي)?|انشي|صمم(?:ي)?|سوي|سو|اعمل(?:ي)?|اصنع(?:ي)?|ولد(?:ي)?)';
const GRAPH_TARGET = '(?:\\b(?:character|characters|story|diagram|world|scene|plot|canvas|graph)\\b|شخصيه|شخصيات|قصه|مخطط|عالم|مشهد|خريطه)';

/** Returns existing, moodboard, or clarify. Explicit mode is an in-chat choice. */
export function routeCreativityChat({domain, text, mode = 'auto'}) {
  if (!isCreativityDomain(domain)) return 'existing';
  const message = normalize(text);
  if (!message) return 'existing';
  if (mode === 'moodboard') return 'moodboard';
  if (!new RegExp(BOARD).test(message)) return 'existing';

  // Advice and negation must not silently turn into generation.
  if (/\b(?:how (?:do|can|should|to)|what|why|explain|advice|tips)\b|كيف|اشرح|ما هي|ما هو|نصيحه|نصائح/.test(message)
      || /\b(?:do not|don't|dont|never)\b|لا\s+(?:تنشئ|تصمم|تسوي|تولد|تعمل)/.test(message)) return 'existing';

  const creation = new RegExp(`${ACTION}([^.!?;؟؛]{0,100}?)${BOARD}`).exec(message);
  if (!creation) return 'clarify';
  // "Create a character inspired by my moodboard" remains a graph request.
  if (new RegExp(GRAPH_TARGET).test(creation[1])) {
    return /\band\b|\sو/.test(creation[1]) ? 'clarify' : 'existing';
  }
  const remainder = message.replace(creation[0], ' ');
  const anotherAction = new RegExp(`(?:${ACTION}|\\b(?:edit|change|update|delete|remove|add)\\b|عدل(?:ي)?|غير(?:ي)?|اضف(?:ي)?|احذف(?:ي)?)[^.!?;؟؛]{0,70}${GRAPH_TARGET}`);
  const anotherObject = new RegExp(`(?:\\band\\s+(?:(?:a|an|the|my)\\s+)?|و(?:ال)?|و\\s+)${GRAPH_TARGET}`);
  return anotherAction.test(remainder) || anotherObject.test(remainder) ? 'clarify' : 'moodboard';
}
