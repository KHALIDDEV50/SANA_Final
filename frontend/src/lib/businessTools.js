// Business reports and graph diagrams use different backend enum names.
export const BUSINESS_TOOLS = [
  {toolType:'BUSINESS_MODEL_BUILDER',diagramType:'BUSINESS_MODEL_CANVAS',ar:'نموذج العمل التجاري',en:'Business model'},
  {toolType:'TARGET_AUDIENCE_BUILDER',diagramType:'TARGET_AUDIENCE',ar:'الجمهور المستهدف',en:'Target audience'},
  {toolType:'SWOT_ANALYZER',diagramType:'SWOT',ar:'تحليل SWOT',en:'SWOT analysis'},
  {toolType:'COMPETITOR_ANALYSIS',diagramType:'COMPETITOR_ANALYSIS',ar:'تحليل المنافسين',en:'Competitor analysis'},
  {toolType:'RISK_ANALYZER',diagramType:'RISK_ANALYSIS',ar:'تحليل المخاطر',en:'Risk analysis'},
];
export const toolForDiagram = type => BUSINESS_TOOLS.find(tool=>tool.diagramType===type);
export const isPendingRun = run => ['PENDING','RUNNING'].includes(run?.status);
export function parseBusinessReport(message) {
  if(typeof message!=='string')return null;
  try {
    const report=JSON.parse(message);
    return report && !Array.isArray(report) && BUSINESS_TOOLS.some(t=>t.toolType===report.toolType)
      && typeof report.executiveSummary==='string' ? report : null;
  } catch { return null; }
}
export function parseBusinessInput(text) {
  let input;
  try {input=JSON.parse(text||'{}')}catch{throw new Error('المدخلات يجب أن تكون JSON صالحًا / Input must be valid JSON')}
  if(!input||Array.isArray(input)||typeof input!=='object')throw new Error('المدخلات يجب أن تكون كائن JSON / Input must be a JSON object');
  const serialized=JSON.stringify(input);
  if(serialized.length>20000)throw new Error('المدخلات تتجاوز 20000 حرف / Input exceeds 20000 characters');
  return serialized;
}
export function graphRequest(task,prompt,project,diagramType) {
  if(task==='CREATE')return {task,prompt,diagramType};
  return {task,prompt,outputId:project.activeOutputId,baseRevision:project.graphRevision};
}
// The server currently sends an empty conversationHistory. Carry only clarification
// turns for this request, never unrelated proposals or previous model analyses.
export function clarificationPrompt(original,turns,answer) {
  if(!turns.length)return answer;
  return ['الطلب الأصلي / Original request:',original,
    ...turns.flatMap(turn=>['سؤال توضيحي / Clarification:',turn.question,'إجابة المستخدم / User answer:',turn.answer]),
    'إجابة المستخدم الحالية / Current answer:',answer].join('\n');
}
export function initialBusinessInput(project) {
  return JSON.stringify({projectName:project.name,description:project.description||'',
    facts:[],assumptions:[],unknowns:[]},null,2);
}
