import React from 'react';
import {Plus, Pencil, Link2, ArrowUpRight, ShieldAlert, LayoutGrid, Sparkles} from 'lucide-react';

// Presentation only: actual elements, relationships and revisions stay in the existing editor/API.
const names={
 key_partnerships:['الشركاء الرئيسيون','Key partners'],key_activities:['الأنشطة الرئيسية','Key activities'],key_resources:['الموارد الرئيسية','Key resources'],
 value_proposition:['القيمة المقترحة','Value propositions'],customer_relationships:['علاقات العملاء','Customer relationships'],channels:['القنوات','Channels'],
 customer_segment:['شرائح العملاء','Customer segments'],cost_structure:['هيكل التكاليف','Cost structure'],revenue_streams:['مصادر الإيرادات','Revenue streams'],
 swot_strength:['نقاط القوة','Strengths'],swot_weakness:['نقاط الضعف','Weaknesses'],swot_opportunity:['الفرص','Opportunities'],swot_threat:['التهديدات','Threats'],
 target_audience:['الجمهور المستهدف','Target audience'],customer_persona:['شخصية العميل','Customer persona'],
 competitor:['المنافسون','Competitors'],competitor_product:['المنتجات','Products'],competitor_feature:['الميزات','Features'],
 risk:['المخاطر','Risks'],risk_cause:['الأسباب','Causes'],risk_mitigation:['إجراءات التخفيف','Mitigations']
};
const label=(type,lang,t)=>names[type]?.[lang==='ar'?0:1]||t(type);
const BMC=['key_partnerships','key_activities','key_resources','value_proposition','customer_relationships','channels','customer_segment','cost_structure','revenue_streams'];
const SWOT=['swot_strength','swot_weakness','swot_opportunity','swot_threat'];
const riskScale=value=>{if(value==null||value==='')return null;const n=Number(value);return Number.isFinite(n)&&n>=1&&n<=5?n:null};

// Each saved relationship appears where its source/target is shown, not just in a distant list.
// Keep server IDs and relationship objects unchanged; selecting a connection opens the existing editor.
function Item({element,lang,onSelect,relations,onRelation,elementById,t}){
 const links=relations.filter(r=>String(r.source)===String(element.id)||String(r.target)===String(element.id));
 return <article className="sana-board-item">
  <button type="button" className="sana-board-item-edit" onClick={()=>onSelect(element)} title={lang==='ar'?'اضغطي للتعديل':'Click to edit'}>
   <span className="sana-board-item-title">{element.name||'—'} <Pencil size={13}/></span>
   {element.description&&<small>{element.description}</small>}
  </button>
  {links.length>0&&<div className="sana-item-relations" aria-label={lang==='ar'?'علاقات العنصر':'Element relationships'}>{links.map(r=>{
   const outgoing=String(r.source)===String(element.id),other=elementById.get(String(outgoing?r.target:r.source));
   return <button type="button" key={r.id} className="sana-item-link" onClick={()=>onRelation(r)} title={lang==='ar'?'اضغطي لعرض العلاقة':'View relationship'}>
    <Link2 size={12}/><span className="sana-link-direction" aria-hidden="true">{outgoing?'→':'←'}</span>
    <span className="sana-link-target">{other?.name||'—'}</span>
    <span className="sana-link-kind">{r.label||t('relation_'+r.type)||r.type}</span>
   </button>;
  })}</div>}
 </article>;
}
function Cell({type,project,lang,t,onSelect,onAdd,onRelation,number,description}){
 const elements=(project.elements||[]).filter(e=>e.type===type);
 const elementById=new Map((project.elements||[]).map(e=>[String(e.id),e]));
 const relations=project.relations||[];
 return <section className={`sana-board-cell sana-cell-${type}`}>
   <header><span className="sana-cell-index">{number||'✦'}</span><div><h3>{label(type,lang,t)}</h3>{description&&<p>{description}</p>}</div><b>{elements.length}</b></header>
   <div className="sana-cell-elements">{elements.length?elements.map(el=><Item key={el.id} element={el} lang={lang} onSelect={onSelect} relations={relations} onRelation={onRelation} elementById={elementById} t={t}/>):<p className="sana-cell-empty">{lang==='ar'?'ابدأ بإضافة أول عنصر هنا':'Add your first item here'}</p>}</div>
   <button type="button" className="sana-cell-add" onClick={()=>onAdd(type)}><Plus size={16}/>{lang==='ar'?'إضافة عنصر':'Add item'}</button>
 </section>
}
export default function SpecializedDiagramBoard({project,t,lang,onSelect,onAdd,onRelation}){
 const type=project.activeType,summary=lang==='ar'?'طريقة عرض مناسبة لطبيعة المخطط — اضغط على أي عنصر لتعديله':'Diagram-specific layout — select any item to edit it';
 const count=project.elements?.length||0;
 const cell=p=><Cell key={p} type={p} project={project} lang={lang} t={t} onSelect={onSelect} onAdd={onAdd} onRelation={onRelation} number={BMC.includes(p)?BMC.indexOf(p)+1:null}/>;
 return <div className={`sana-board sana-board-${type.toLowerCase()}`}>
 <div className="sana-board-heading"><div><span className="sana-board-overline"><Sparkles size={15}/> SANA · {type.replaceAll('_',' ')}</span><h2>{t('diagram_'+type)||type}</h2><p>{summary}</p></div><div className="sana-board-count"><strong>{count}</strong><span>{lang==='ar'?'عنصر محفوظ':'Saved items'}</span></div></div>
 {type==='BUSINESS_MODEL_CANVAS'&&<div className="sana-bmc">
  <div className="sana-bmc-body"><div className="sana-bmc-partners">{cell('key_partnerships')}</div><div className="sana-bmc-activities">{cell('key_activities')}{cell('key_resources')}</div><div className="sana-bmc-value">{cell('value_proposition')}</div><div className="sana-bmc-customer">{cell('customer_relationships')}{cell('channels')}</div><div className="sana-bmc-segments">{cell('customer_segment')}</div></div>
  <div className="sana-bmc-footer">{cell('cost_structure')}{cell('revenue_streams')}</div>
 </div>}
 {type==='SWOT'&&<div className="sana-swot">{SWOT.map(cell)}</div>}
 {type==='TARGET_AUDIENCE'&&<div className="sana-audience">{['target_audience','customer_persona'].map(cell)}</div>}
 {type==='COMPETITOR_ANALYSIS'&&<div className="sana-competition"><div className="sana-competition-intro">{lang==='ar'?'قارن بين المنافسين والمنتجات والميزات المحفوظة، وافتح أي عنصر لتعديل تفاصيله.':'Compare your saved competitors, products, and features. Open any item to edit its details.'}</div><div className="sana-competition-columns">{['competitor','competitor_product','competitor_feature'].map(cell)}</div></div>}
 {type==='RISK_ANALYSIS'&&<div className="sana-risk-board">
   <div className="sana-risk-columns">{['risk','risk_cause','risk_mitigation'].map(cell)}</div>
   {(project.elements||[]).filter(e=>e.type==='risk'&&riskScale(e.properties?.likelihood)!=null&&riskScale(e.properties?.impact)!=null).length>0&&<section className="sana-risk-heatmap"><h3><ShieldAlert size={18}/>{lang==='ar'?'مصفوفة الاحتمالية والأثر (البيانات المدخلة فقط)':'Likelihood × impact (saved values only)'}</h3><div className="sana-risk-grid" role="grid" aria-label={lang==='ar'?'مصفوفة المخاطر':'Risk matrix'}>{[5,4,3,2,1].flatMap(l=>[1,2,3,4,5].map(impact=>{const risks=(project.elements||[]).filter(e=>e.type==='risk'&&riskScale(e.properties?.likelihood)===l&&riskScale(e.properties?.impact)===impact);return <div key={`${l}-${impact}`} className={`sana-risk-box sana-risk-level-${Math.min(4,Math.ceil(l*impact/7))}`} role="gridcell" aria-label={`${lang==='ar'?'احتمال':'Likelihood'} ${l}, ${lang==='ar'?'أثر':'Impact'} ${impact}`}><small>{l}×{impact}</small>{risks.map(el=><button type="button" key={el.id} onClick={()=>onSelect(el)}>{el.name}</button>)}</div>}))}</div><p>{lang==='ar'?'المحور الأفقي: الأثر من 1 إلى 5 · العمودي: الاحتمالية من 5 إلى 1. القيم غير المدخلة لا تظهر في المصفوفة.':'Horizontal: impact 1–5 · Vertical: likelihood 5–1. Missing scores are not plotted.'}</p></section>}
 </div>}
 {(project.relations||[]).length>0&&<section className="sana-board-relations"><h3><Link2 size={18}/>{lang==='ar'?'العلاقات المحفوظة':'Saved relationships'}</h3><div className="sana-relations-flow">{project.relations.map(r=>{const a=(project.elements||[]).find(e=>String(e.id)===String(r.source)),b=(project.elements||[]).find(e=>String(e.id)===String(r.target));return <button className="sana-relation-flow" type="button" key={r.id} onClick={()=>onRelation(r)} title={lang==='ar'?'تعديل العلاقة':'Edit relationship'}><span className="sana-relation-from">{a?.name||'?'}</span><span className="sana-relation-connector"><small>{r.label||t('relation_'+r.type)||r.type}</small><span className="sana-connector-line"><ArrowUpRight size={13}/></span></span><span className="sana-relation-to">{b?.name||'?'}</span></button>})}</div></section>}
 </div>
}
export const SPECIAL_BOARD_TYPES=new Set(['BUSINESS_MODEL_CANVAS','SWOT','TARGET_AUDIENCE','COMPETITOR_ANALYSIS','RISK_ANALYSIS']);
