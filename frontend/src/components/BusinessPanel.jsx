import React,{useEffect,useRef,useState} from 'react';
import {aiApi,businessApi,repository,api} from '../lib/data';
import {BUSINESS_TOOLS,toolForDiagram,parseBusinessInput,parseBusinessReport,isPendingRun,graphRequest,clarificationPrompt,initialBusinessInput} from '../lib/businessTools';
import BusinessReport from './BusinessReport';
import './business.css';

function GraphPreview({graph,lang,t}) {
 const nodes=graph?.nodes||[],edges=graph?.edges||[];
 if(!nodes.length)return <p>{lang==='ar'?'المقترح لا يحتوي عناصر.':'The proposal has no nodes.'}</p>;
 const byKey=new Map(nodes.map(n=>[n.key,n]));
 const minX=Math.min(...nodes.map(n=>n.positionX))-20,minY=Math.min(...nodes.map(n=>n.positionY))-20;
 const width=Math.max(...nodes.map(n=>n.positionX))+200-minX,height=Math.max(...nodes.map(n=>n.positionY))+100-minY;
 return <div className="business-preview">
  <svg role="img" aria-label={lang==='ar'?'معاينة المخطط المقترح':'Proposed graph preview'} viewBox={`${minX} ${minY} ${width} ${height}`}>
   {edges.map(e=>{const a=byKey.get(e.sourceKey),b=byKey.get(e.targetKey);return a&&b?<g key={e.key}><line x1={a.positionX+80} y1={a.positionY+30} x2={b.positionX+80} y2={b.positionY+30} stroke="#a0814b"/><text x={(a.positionX+b.positionX)/2+80} y={(a.positionY+b.positionY)/2+22} textAnchor="middle" fontSize="10">{e.label||e.type}</text></g>:null})}
   {nodes.map(n=><g key={n.key}><rect x={n.positionX} y={n.positionY} width="160" height="60" rx="8" fill="#f7f3e9" stroke="#a0814b"/><text x={n.positionX+80} y={n.positionY+24} textAnchor="middle" fontSize="11">{t(n.type.toLowerCase())}</text><text x={n.positionX+80} y={n.positionY+43} textAnchor="middle" fontSize="11">{String(n.properties?.label||'').slice(0,24)}</text></g>)}
  </svg>
  <details><summary>{lang==='ar'?'تفاصيل جميع العناصر والعلاقات':'All nodes and relationships'}</summary>
   {nodes.map(n=><div className="business-preview-item" key={n.key}><strong>{n.properties?.label}</strong><small>{t(n.type.toLowerCase())}</small><p>{n.properties?.description}</p><pre>{JSON.stringify(n.properties,null,2)}</pre></div>)}
   {edges.map(e=><p key={e.key}>{byKey.get(e.sourceKey)?.properties?.label} → {e.label||e.type} → {byKey.get(e.targetKey)?.properties?.label}</p>)}
  </details>
 </div>;
}
export default function BusinessPanel({project,lang,t,onClose,onApplied}) {
 const ar=lang==='ar';
 const text=(a,b)=>ar?a:b;
 const [mode,setMode]=useState('CREATE'),[diagramType,setDiagramType]=useState(project.activeType),[toolType,setToolType]=useState(toolForDiagram(project.activeType)?.toolType||BUSINESS_TOOLS[0].toolType);
 const [prompt,setPrompt]=useState(''),[input,setInput]=useState(()=>initialBusinessInput(project));
 const [run,setRun]=useState(null),[proposal,setProposal]=useState(null),[history,setHistory]=useState([]),[selectedRun,setSelectedRun]=useState('');
 const [busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState(''),[clarification,setClarification]=useState(null);
 const alive=useRef(true),locked=useRef(false),origin=useRef(null);
 useEffect(()=>{alive.current=true;aiApi.history(project.id).then(rows=>{if(alive.current)setHistory(rows)}).catch(e=>{if(alive.current)setError(e.message)});return()=>{alive.current=false}},[project.id]);
 async function refreshHistory(){const rows=await aiApi.history(project.id);if(alive.current)setHistory(rows)}
 function resetContext(){setClarification(null);origin.current=null;setPrompt('')}
 // Polling only reads a run; it never dispatches a second AI request.
 async function follow(started){
  let current=started;
  if(alive.current)setRun(current);
  const expires=Date.now()+240000;
  while(alive.current&&isPendingRun(current)&&Date.now()<expires){
   await new Promise(resolve=>setTimeout(resolve,2000));
   if(!alive.current)return null;
   current=await aiApi.status(current.statusUrl);if(alive.current)setRun(current);
  }
  return alive.current?current:null;
 }
 async function display(current){
  if(!current||!alive.current)return;
  setRun(current);setSelectedRun(current.requestId);
  if(current.status==='FAILED'||current.status==='TIMED_OUT'){
   setError(`${current.error?.code||current.status}: ${current.error?.message||text('تعذر التنفيذ.','Execution failed.')}`);return;
  }
  if(isPendingRun(current)){setNotice(text('الطلب ما زال يعمل. استخدم متابعة الطلب دون إعادة إرساله.','The run is still active. Resume it without submitting again.'));return}
  if(current.status!=='COMPLETED'||!current.result)throw new Error(text('الاستجابة لا تحتوي نتيجة مكتملة.','The response has no completed result.'));
  if(current.result.type==='CLARIFICATION'){
   if(origin.current){setClarification(current.result.clarification);setPrompt('')}
   else setNotice(text('لاستكمال هذا الطلب من السجل، اكتب الطلب الأصلي مع إجابة سؤال التوضيح وأرسله كطلب جديد.','To continue a historical clarification, submit the original request together with your answer.'));
  }else{setClarification(null);origin.current=null}
  if(current.result.type==='PROPOSAL'){
   if(!current.proposalId)throw new Error(text('معرف المقترح غير موجود.','Missing proposal ID.'));
   const preview=await aiApi.proposal(project.id,current.proposalId);if(alive.current)setProposal(preview);
  }
 }
 async function perform(action){
  if(locked.current)return;locked.current=true;setBusy(true);setError('');setNotice('');
  try{await action()}catch(e){if(alive.current)setError(e.message)}finally{locked.current=false;if(alive.current){setBusy(false);refreshHistory().catch(e=>{if(alive.current)setNotice(e.message)})}}
 }
 function send(e){e.preventDefault();perform(async()=>{
  // Resolve all local validation before creating a diagram or recording a run.
  const reportInput=mode==='REPORT'?parseBusinessInput(input):null;
  const answer=prompt.trim();
  if((mode!=='REPORT'||clarification)&&!answer)throw new Error(text('اكتب طلبك أولًا.','Enter a request first.'));
  if(proposal?.reviewStatus==='PENDING')throw new Error(text('راجع المقترح الحالي بالقبول أو الرفض أولًا.','Accept or reject the pending proposal first.'));
  setProposal(null);
  let submitted;
  if(mode==='REPORT'){
   const tool=BUSINESS_TOOLS.find(item=>item.toolType===toolType);
   // Use an output of the selected tool's diagram type, even when a different diagram is open.
   const outputId=clarification&&origin.current?.outputId?origin.current.outputId:await repository.ensureDiagram(project,tool.diagramType);
   const current=await api(`/api/v1/diagram/graph/get/${outputId}`);
   const data=JSON.parse(reportInput);
   if(clarification&&origin.current){
    const next=[...origin.current.turns,{question:clarification.question,answer}];
    data.clarificationAnswers=next;origin.current.turns=next;
   }else origin.current={prompt:answer,turns:[],mode,toolType,outputId};
   submitted=await businessApi.execute({projectId:project.id,outputId,toolType,baseRevision:current.graphRevision,inputJson:parseBusinessInput(JSON.stringify(data))});
  }else{
   const original=origin.current;
   let fullPrompt=answer;
   if(clarification&&original){
    const next=[...original.turns,{question:clarification.question,answer}];
    fullPrompt=clarificationPrompt(original.prompt,next,'');original.turns=next;
   }else origin.current={prompt:answer,turns:[],mode,diagramType,outputId:project.activeOutputId,baseRevision:project.graphRevision};
   if(fullPrompt.length>10000)throw new Error(text('الطلب مع التوضيحات يتجاوز 10000 حرف. اختصره وأعد المحاولة.','The request exceeds 10000 characters. Shorten it before retrying.'));
   submitted=await aiApi.start(project.id,graphRequest(mode,fullPrompt,clarification?{...project,activeOutputId:original.outputId,graphRevision:original.baseRevision}:project,diagramType));
  }
  await display(await follow(submitted));
 })}
 function restore(){perform(async()=>{
  resetContext();setProposal(null);
  const existing=history.find(item=>item.requestId===selectedRun);if(!existing)return;
  const current=await aiApi.status(existing.statusUrl);
  setMode(current.businessToolType?'REPORT':current.task);
  setDiagramType(current.diagramType||project.activeType);
  if(current.businessToolType){setToolType(current.businessToolType);setInput(JSON.stringify(current.businessInput||{},null,2))}
  // Keep the authoritative target and revision when answering an older clarification.
  origin.current={prompt:current.prompt||'',turns:Array.isArray(current.businessInput?.clarificationAnswers)?current.businessInput.clarificationAnswers:[],mode:current.task,outputId:current.outputId,baseRevision:current.baseRevision};
  await display(await follow(current));
 })}
 function decide(accept){perform(async()=>{
  if(accept){const applied=await aiApi.accept(project.id,proposal.proposalId);setProposal({...proposal,reviewStatus:'ACCEPTED'});setNotice(text('تم قبول المقترح وحفظ المخطط.','Proposal accepted and diagram saved.'));await onApplied(applied.outputId)}
  else{const rejected=await aiApi.reject(project.id,proposal.proposalId);setProposal(rejected);setNotice(text('تم رفض المقترح.','Proposal rejected.'))}
 })}
 const result=run?.result,report=result?.type==='ADVICE'?parseBusinessReport(result.message):null;
 const historicalPending=history.some(isPendingRun);
 return <aside className="ai-sidebar business-panel" dir={ar?'rtl':'ltr'}>
  <div className="ai-side-head"><strong>{text('أدوات سنا التجارية','SANA Business tools')}</strong><button type="button" onClick={onClose} aria-label={text('إغلاق','Close')}>×</button></div>
  <div className="ai-body business-body">
   <p>{text('إنشاء المخططات ينتج مقترحًا يحتاج قبولك. التقرير التحليلي يُحفظ في سجل التنفيذ.','Diagram generation produces a proposal for your review. Reports are saved in run history.')}</p>
   <details open={historicalPending}><summary>{text('سجل الطلبات — آخر 50','Request history — latest 50')}</summary>
    <label>{text('الطلب المحفوظ','Saved request')}<select aria-label={text('الطلب المحفوظ','Saved request')} disabled={busy} value={selectedRun} onChange={e=>setSelectedRun(e.target.value)}><option value="">{text('اختر طلبًا','Select a request')}</option>{history.map(item=><option key={item.requestId} value={item.requestId}>#{item.interactionId} · {item.businessToolType||item.diagramType||item.task} · {item.status} · {item.createdAt}</option>)}</select></label>
    <button type="button" disabled={busy||!selectedRun} onClick={restore}>{text('عرض / متابعة الطلب','View / resume request')}</button>
   </details>
   {error&&<p className="business-error" role="alert">{error}</p>}
   {notice&&<p role="status">{notice}</p>}
   {run&&<div className="business-run" role="status"><strong>{run.status}</strong><small>{text('رقم الطلب','Request')}: {run.requestId}</small>{run.executionId&&<small>n8n: {run.executionId}</small>}
    {isPendingRun(run)&&!busy&&<button type="button" onClick={()=>perform(async()=>display(await follow(await aiApi.status(run.statusUrl))))}>{text('متابعة الطلب','Resume request')}</button>}
   </div>}
   {report?<BusinessReport report={report} lang={lang}/>:result&&<div className="business-message">{result.message}</div>}
   {result?.type==='CLARIFICATION'&&<section className="business-question"><strong>{result.clarification?.question}</strong><div>{(result.clarification?.suggestedAnswers||[]).map(answer=><button type="button" key={answer} disabled={busy} onClick={()=>setPrompt(answer)}>{answer}</button>)}</div></section>}
   {proposal&&<section className="business-proposal"><h3>{proposal.title}</h3><p>{proposal.description}</p><strong>{proposal.reviewStatus}</strong><GraphPreview graph={proposal.proposedGraph} lang={lang} t={t}/>
    {proposal.reviewStatus==='PENDING'&&<div className="business-actions"><button type="button" disabled={busy} onClick={()=>decide(true)}>{text('قبول وحفظ المخطط','Accept and save diagram')}</button><button type="button" disabled={busy} onClick={()=>decide(false)}>{text('رفض','Reject')}</button></div>}
   </section>}
  </div>
  <form className="ai-bottom business-form" onSubmit={send}>
   <label>{text('نوع العملية','Operation')}<select aria-label={text('نوع العملية','Operation')} disabled={busy||!!clarification} value={mode} onChange={e=>{setMode(e.target.value);resetContext()}}>{['CREATE','EDIT','ADVISE','REPORT'].map(value=><option key={value} value={value}>{value==='REPORT'?text('تقرير تحليلي منظم','Structured analysis report'):t(`aiTask_${value}`)}</option>)}</select></label>
   {mode==='CREATE'&&<label>{text('نوع المخطط','Diagram type')}<select aria-label={text('نوع المخطط','Diagram type')} disabled={busy||!!clarification} value={diagramType} onChange={e=>{setDiagramType(e.target.value);resetContext()}}>{BUSINESS_TOOLS.map(tool=><option key={tool.diagramType} value={tool.diagramType}>{tool[lang]||tool.en}</option>)}</select></label>}
   {mode==='REPORT'&&<><label>{text('أداة التحليل','Analysis tool')}<select aria-label={text('أداة التحليل','Analysis tool')} disabled={busy||!!clarification} value={toolType} onChange={e=>{setToolType(e.target.value);resetContext()}}>{BUSINESS_TOOLS.map(tool=><option key={tool.toolType} value={tool.toolType}>{tool[lang]||tool.en}</option>)}</select></label><details><summary>{text('بيانات التحليل: حقائق وافتراضات ومعلومات ناقصة','Analysis input: facts, assumptions and unknowns')}</summary><textarea aria-label="Business input JSON" dir="ltr" rows={7} maxLength={20000} value={input} disabled={busy||!!clarification} onChange={e=>setInput(e.target.value)}/></details></>}
   {(mode!=='REPORT'||clarification)&&<label>{clarification?text('إجابتك على سؤال التوضيح','Your clarification answer'):text('طلبك','Your request')}<textarea rows={3} maxLength={10000} disabled={busy} value={prompt} onChange={e=>setPrompt(e.target.value)} required placeholder={text('اكتب الطلب مع التمييز بين الحقائق والافتراضات','Describe your request and distinguish facts from assumptions')}/></label>}
   <button type="submit" disabled={busy||isPendingRun(run)||proposal?.reviewStatus==='PENDING'}>{busy?text('جارٍ التنفيذ…','Working…'):clarification?text('إرسال الإجابة','Send answer'):text('إرسال','Send')}</button>
   {clarification&&<button type="button" disabled={busy} onClick={resetContext}>{text('بدء طلب مستقل','Start a separate request')}</button>}
  </form>
 </aside>;
}
