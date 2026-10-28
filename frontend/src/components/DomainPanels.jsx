import React,{useEffect,useRef,useState} from 'react';
import {API_MODE,api,aiApi} from '../lib/data';
import {tr} from '../lib/i18n';
import {moodboardReferenceUrl} from '../lib/creativeTools';
import {BookOpen, Sparkles, ArrowUpRight, RefreshCcw, Compass, LoaderCircle, Feather, Clock3, ScrollText, CalendarClock, MapPinned, ChevronsDown, ChevronUp, GitBranch, ArrowLeft, ArrowRight, RotateCw} from 'lucide-react';
const request=(method,data)=>({method,body:JSON.stringify(data)});
const tools=['BUSINESS_MODEL_BUILDER','TARGET_AUDIENCE_BUILDER','SWOT_ANALYZER','COMPETITOR_ANALYSIS','RISK_ANALYZER'];
export default function DomainPanels({project,lang,mode,onViewEvent}){
 const t=k=>tr(lang,k),ar=lang==='ar';
 const creative=['writing','games'].includes(project.domain);
 const available=creative?[['timeline','timeline']]:project.domain==='business'?[['business','businessReports'],['indicators','saudiIndicators']]:[];
 const [tab,setTab]=useState(mode==='timeline'?'timeline':available[0]?.[0]||'');
 const [error,setError]=useState(''),[busy,setBusy]=useState(false),[query,setQuery]=useState(''),[books,setBooks]=useState([]),[book,setBook]=useState(null),[tool,setTool]=useState(tools[0]),[input,setInput]=useState(''),[report,setReport]=useState(null),[indicators,setIndicators]=useState(null);
 const alive=useRef(true);useEffect(()=>{alive.current=true;return()=>{alive.current=false}},[]);
 async function act(fn){setBusy(true);setError('');try{await fn()}catch(e){if(alive.current)setError(e.message)}finally{if(alive.current)setBusy(false)}}
 if(creative&&tab==='timeline')return <StoryTimeline project={project} lang={lang} onViewEvent={onViewEvent}/>;
 if(!API_MODE)return <section className="spark-panel"><p>{t('noBackend')}</p></section>;
 return <section className="spark-domain-panels"><header><span className="eyebrow">SANA</span><h1>{creative?(ar?'التسلسل الزمني':'Timeline'):t('businessReports')}</h1></header>{!creative&&<nav className="spark-panel-tabs">{available.map(([id,key])=><button key={id} className={tab===id?'selected':''} onClick={()=>setTab(id)}>{t(key)}</button>)}</nav>}{error&&<p role="alert" className="ux-error">{error}</p>}
 {tab==='business'&&<form className="spark-panel" onSubmit={e=>{e.preventDefault();act(async()=>{const inputJson=JSON.stringify({description:input});let run=await api('/api/business/tools',request('POST',{projectId:project.id,outputId:project.activeOutputId,baseRevision:project.graphRevision,toolType:tool,inputJson}));setReport(null);for(let i=0;i<120&&alive.current&&['PENDING','RUNNING'].includes(run.status);i++){await new Promise(r=>setTimeout(r,2000));if(!alive.current)return;run=await aiApi.status(run.statusUrl)}if(!alive.current)return;if(run.status!=='COMPLETED')throw Error(run.error?.message||run.status);setReport(run.result)})}}><label className="form-field"><span>{t('businessReports')}</span><select value={tool} onChange={e=>setTool(e.target.value)}>{tools.map((k,i)=><option key={k} value={k}>{ar?['نموذج العمل','الجمهور المستهدف','تحليل SWOT','تحليل المنافسين','تحليل المخاطر'][i]:k.replaceAll('_',' ')}</option>)}</select></label><label className="form-field"><span>{t('reportInput')}</span><textarea required maxLength={15000} rows={5} value={input} onChange={e=>setInput(e.target.value)}/></label><button className="btn btn-primary" disabled={busy||!project.activeOutputId}>{t(busy?'awaitingResult':'runReport')}</button>{report&&<Report value={report} lang={lang}/>}</form>}
 {tab==='indicators'&&<section className="spark-panel"><p>{ar?'بيانات مرجعية من البنك الدولي؛ ليست تحليل سوق أو بيانات يستهلكها AI تلقائيًا.':'World Bank reference data, not market analysis or automatically supplied AI context.'}</p><button className="btn btn-primary" disabled={busy} onClick={()=>act(async()=>setIndicators(await api('/api/v1/external/world-bank/saudi-indicators')))}>{t('refresh')}</button>{indicators&&<Report value={indicators} lang={lang}/>}</section>}
 </section>
}

/** Timeline is a read-only visualization of the existing story-event endpoints.
 * No changes to events, graph state or any project data happen here. */
function StoryTimeline({project,lang,onViewEvent}){
 const ar=lang==='ar';
 const say=(arabic,english)=>ar?arabic:english;
 const [order,setOrder]=useState('chronological');
 const [events,setEvents]=useState([]);
 const [loading,setLoading]=useState(false);
 const [error,setError]=useState('');
 const [refresh,setRefresh]=useState(0);
 const [expanded,setExpanded]=useState(null);
 const storyDiagram=project.diagrams?.find(diagram=>diagram.outputType==='STORY_MAP');
 // Events live in the story map, which may not be the currently selected diagram.
 const sourceId=storyDiagram?.id??(project.storyMap?project.activeOutputId:null);
 useEffect(()=>{
  let current=true;
  setError('');setExpanded(null);
  if(!API_MODE||!sourceId){setEvents([]);setLoading(false);return()=>{current=false}};
  setLoading(true);
  api(`/api/v1/story-event/timeline/${order}/${sourceId}`)
   .then(data=>{if(current)setEvents(Array.isArray(data)?data:[])})
   .catch(e=>{if(current){setEvents([]);setError(e.message||say('تعذّر تحميل الأحداث.','Could not load story events.'))}})
   .finally(()=>{if(current)setLoading(false)});
  return()=>{current=false};
 },[sourceId,order,refresh,ar]);
 const dateField=order==='chronological'?'chronologicalOrder':'presentationOrder';
 const ordered=events.filter(item=>item[dateField]!=null);
 const unplaced=events.filter(item=>item[dateField]==null);
 const initialBlank=!sourceId;
 const countLabel=events.length===1?say('حدث واحد','1 event'):`${events.length} ${say('أحداث','events')}`;
 const eventTypeName=type=>{
  const tags={OPENING:["بداية","Opening"],INTRODUCTION:["تمهيد","Introduction"],INCITING_INCIDENT:["بداية التحول","Inciting event"],CONFLICT:["صراع","Conflict"],CLIMAX:["ذروة","Climax"],REVEAL:["اكتشاف","Reveal"],TWIST:["مفاجأة","Twist"],RESOLUTION:["حل العقدة","Resolution"],ENDING:["خاتمة","Ending"],FLASHBACK:["استرجاع","Flashback"]};
  return tags[String(type||'').toUpperCase()]?.[ar?0:1]||null;
 };
 const showMap=event=>onViewEvent?.(event||null);
 const renderEvent=(event,index)=>{
  const id=String(event.id??event.nodeId??index);
  const open=expanded===id;
  const type=eventTypeName(event.eventType);
  return <li className={`sana-timeline-moment ${open?'is-open':''}`} key={id} style={{'--sana-timeline-index':Math.min(index,9)}}>
   <div className="sana-timeline-rail-point" aria-hidden="true"><span>{String(index+1).padStart(2,'0')}</span></div>
   <article className="sana-timeline-event-card">
    <div className="sana-timeline-event-top">
     <div className="sana-timeline-event-meta">{type&&<span className="sana-timeline-event-type">{type}</span>}{event.inWorldTime&&<span className="sana-timeline-event-time"><Clock3 size={14}/>{event.inWorldTime}</span>}</div>
     <span className="sana-timeline-event-order">{say('الحدث','Event')} {event[dateField]??index+1}</span>
    </div>
    <h3>{event.title||say('حدث بلا عنوان','Untitled event')}</h3>
    {event.description&&<p className={`sana-timeline-event-description ${open?'is-expanded':''}`}>{event.description}</p>}
    <div className="sana-timeline-event-actions">
     {!!event.description&&<button type="button" className="sana-timeline-text-action" aria-expanded={open} onClick={()=>setExpanded(open?null:id)}>{open?<><ChevronUp size={16}/>{say('عرض أقل','Show less')}</>:<><ChevronsDown size={16}/>{say('تفاصيل الحدث','Event details')}</>}</button>}
     {onViewEvent&&event.nodeId!=null&&<button type="button" className="sana-timeline-map-action" onClick={()=>showMap(event)}><MapPinned size={16}/>{say('عرض في خريطة العلاقات','View on story map')}{ar?<ArrowLeft size={14}/>:<ArrowRight size={14}/>}</button>}
    </div>
   </article>
  </li>
 };
 if(!API_MODE)return <section className="sana-timeline-page"><div className="sana-timeline-empty"><p>{say('تتوفر رحلة الأحداث بعد الاتصال بسنا.','Connect to SANA to view your timeline.')}</p></div></section>;
 return <section className="sana-timeline-page" dir={ar?'rtl':'ltr'}>
  <header className="sana-timeline-hero">
   <div className="sana-timeline-sky" aria-hidden="true"><span/><span/><span/><span/><i/><i/></div>
   <div className="sana-timeline-hero-content">
    <span className="sana-timeline-kicker"><GitBranch size={15}/>{say('رحلة الأحداث · سنا','SANA · STORY TIMELINE')}</span>
    <h1>{say('كل حدث يصنع حكاية','Every event shapes a story')}</h1>
    <p>{say('استكشف تسلسل أحداث مشروعك، واكتشف الفرق بين وقت وقوعها وترتيب ظهورها في القصة.','Explore when events happen and how their reveal shapes your story.')}</p>
   </div>
   <div className="sana-timeline-hero-art" aria-hidden="true"><span className="sana-timeline-hero-orbit orbit-a"/><span className="sana-timeline-hero-orbit orbit-b"/><span className="sana-timeline-hero-core"><Clock3 size={36} strokeWidth={1.25}/></span></div>
  </header>
  <div className="sana-timeline-body">
   <section className="sana-timeline-controls" aria-label={say('اختيار طريقة ترتيب الأحداث','Choose timeline order')}>
    <div className="sana-timeline-controls-heading"><div><span className="sana-timeline-eyebrow">{say('استعرض الأحداث','EXPLORE THE STORY')}</span><h2>{say('تسلسل القصة','Story timeline')}</h2><p>{order==='chronological'?say('ما الذي حدث أولًا داخل عالم القصة؟','What happened first in your story world?'):say('كيف تظهر الأحداث للقارئ أو اللاعب؟','How will the reader or player encounter events?')}</p></div><button type="button" className="sana-timeline-refresh" disabled={loading||!sourceId} onClick={()=>setRefresh(value=>value+1)}><RotateCw size={16} className={loading?'sana-timeline-spin':''}/>{say('تحديث','Refresh')}</button></div>
    <div role="group" className="sana-timeline-order-toggle" aria-label={say('ترتيب الأحداث','Timeline order')}>
     <button type="button" className={order==='chronological'?'active':''} aria-pressed={order==='chronological'} onClick={()=>setOrder('chronological')}><CalendarClock size={18}/><span>{say('ترتيب وقوع الأحداث','When events happened')}</span></button>
     <button type="button" className={order==='presentation'?'active':''} aria-pressed={order==='presentation'} onClick={()=>setOrder('presentation')}><ScrollText size={18}/><span>{say('ترتيب عرض القصة','Storytelling order')}</span></button>
    </div>
   </section>
   <div className="sana-timeline-results" aria-live="polite" aria-busy={loading}>
    {error&&<div className="sana-timeline-error" role="alert"><p>{say('تعذّر تحميل الأحداث حاليًا. تحقق من الاتصال ثم حاول مرة أخرى.','Could not load the timeline. Check your connection and try again.')}</p><button type="button" onClick={()=>setRefresh(v=>v+1)}>{say('المحاولة مرة أخرى','Try again')}</button></div>}
    {loading&&!error&&<div className="sana-timeline-loading" role="status"><span className="sana-timeline-loading-light"/><p>{say('سنا ترتب أحداث القصة...','Organizing the story events...')}</p><div/><div/><div/></div>}
    {!loading&&!error&&(initialBlank||events.length===0)?<div className="sana-timeline-empty">
     <div className="sana-timeline-empty-icon"><GitBranch size={29}/></div>
     <h3>{initialBlank?say('ابدأ بخريطة القصة','Start with your story map'):say('لم تبدأ أحداث الحكاية بعد','Your story has no events yet')}</h3>
     <p>{initialBlank?say('أنشئ خريطة قصة أولًا، وستظهر أحداثها هنا تلقائيًا.','Create a story map and its events will appear here.'):say('أضف الأحداث في خريطة العلاقات لتبدأ رحلتها على الخط الزمني.','Add story events on the map to see their journey here.')}</p>
     {onViewEvent&&<button type="button" onClick={()=>showMap(null)}><MapPinned size={17}/>{say('الذهاب إلى خريطة العلاقات','Open story map')}{ar?<ArrowLeft size={15}/>:<ArrowRight size={15}/>}</button>}
    </div>:null}
    {!loading&&!error&&events.length>0&&<div className="sana-timeline-journey" key={order}>
     <div className="sana-timeline-list-top"><span><span className="sana-timeline-light-dot"/>{countLabel}</span><span>{order==='chronological'?say('بحسب وقت وقوع الحدث','Actual story order'):say('بحسب ظهور الحدث في القصة','Narrative reveal order')}</span></div>
     {ordered.length>0&&<ol className="sana-timeline-event-list">{ordered.map(renderEvent)}</ol>}
     {unplaced.length>0&&<section className="sana-timeline-unplaced"><div className="sana-timeline-unplaced-heading"><Clock3 size={19}/><div><h3>{say('أحداث لم يُحدّد ترتيبها','Events without an order')}</h3><p>{say('هذه الأحداث محفوظة، لكنها تنتظر تحديد موقعها في التسلسل.','These events are saved but do not have a position in this order yet.')}</p></div></div><ol className="sana-timeline-event-list">{unplaced.map((event,index)=>renderEvent(event,ordered.length+index))}</ol></section>}
     <div className="sana-timeline-end"><span/><span>{say('كل حدث يقود إلى الفصل التالي','Every event leads the story forward')}</span><span/></div>
    </div>}
   </div>
  </div>
 </section>;
}

function Report({value,lang}){if(typeof value==='string'&&/^[\[{]/.test(value.trim())){try{const parsed=JSON.parse(value);return <Report value={parsed} lang={lang}/>}catch{}}if(value==null)return null;if(typeof value!=='object')return <p className="spark-report-value">{String(value)}</p>;if(Array.isArray(value))return <ul>{value.map((v,i)=><li key={i}><Report value={v} lang={lang}/></li>)}</ul>;return <dl className="spark-report">{Object.entries(value).map(([k,v])=><div key={k}><dt>{tr(lang,k).replaceAll('_',' ')}</dt><dd><Report value={v}/></dd></div>)}</dl>}


/** The user-facing library is entirely project-led. Book discovery stays on the server. */
function SanaBookCover({book,lang}){
 const [failed,setFailed]=useState(false);
 const cover=moodboardReferenceUrl(book?.thumbnail);
 useEffect(()=>setFailed(false),[cover]);
 const alt=lang==='ar'?`غلاف كتاب ${book?.title||''}`:`Cover of ${book?.title||'book'}`;
 return <div className="sana-library-cover-art">
  {cover&&!failed?<img src={cover} loading="lazy" alt={alt} referrerPolicy="no-referrer" onError={()=>setFailed(true)}/>:
   <div className="sana-library-cover-fallback" role="img" aria-label={alt}>
    <span className="sana-library-cover-orbit" aria-hidden="true"/>
    <BookOpen size={49} strokeWidth={1.15} aria-hidden="true"/>
    <span className="sana-library-cover-monogram">SANA</span>
   </div>}
 </div>;
}

export function BookInspiration({project,lang}){
 const ar=lang==='ar',say=(a,b)=>ar?a:b;
 const [focus,setFocus]=useState('');
 const [draft,setDraft]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const mounted=useRef(true);
 useEffect(()=>{mounted.current=true;return()=>{mounted.current=false}},[]);
 const base=`/api/projects/${project.id}/creativity/assist`;
 const pause=ms=>new Promise(done=>setTimeout(done,ms));
 async function inspire(){
  if(busy)return;
  setBusy(true);setError('');setDraft(null);
  try{
   let next=await api(`${base}/books`,{method:'POST',body:JSON.stringify({focus:focus.trim()})});
   if(mounted.current)setDraft(next);
   for(let attempts=0;attempts<55&&mounted.current&&!['PENDING','FAILED','APPLIED'].includes(next.status);attempts++){
    await pause(2000);if(!mounted.current)return;
    next=await api(`${base}/drafts/${encodeURIComponent(next.draftId)}`);
    if(mounted.current)setDraft(next);
   }
   if(!mounted.current)return;
   if(next.status==='FAILED')setError(say('تعذّر العثور على كتب مناسبة الآن. جرّب مرة أخرى أو غيّر الموضوع الاختياري.','Books are unavailable right now. Try again or change the optional focus.'));
   else if(!['PENDING','APPLIED'].includes(next.status))setError(say('استغرق البحث وقتًا أطول من المتوقع. يمكنك المحاولة مرة أخرى.','This is taking longer than expected. Please try again.'));
  }catch(e){
   if(mounted.current){
    console.warn('SANA book recommendation request failed:',e);
    setError(say('تعذّر إكمال البحث عن الكتب الآن. حاول مرة أخرى بعد قليل.','We could not find books right now. Please try again shortly.'));
   }
  }finally{if(mounted.current)setBusy(false)}
 }
 const stageMessage= draft?.status==='SEARCH_PLANNING'?say('سنا تستكشف عالم المشروع وتفاصيله...','SANA is exploring your project...'):
  draft?.status==='SELECTING'?say('سنا تبحث عن كتب تناسب أجواء المشروع...','SANA is choosing books that suit your project...'):
  say('سنا تبحث عن مصدر إلهام جديد...','SANA is finding inspiration...');
 const recommendations=draft?.status==='PENDING'&&Array.isArray(draft.books)?draft.books.filter(entry=>entry?.book):[];
 const showResults=draft?.status==='PENDING';
 if(!API_MODE)return <section className="spark-panel"><p>{say('يتاح اكتشاف الكتب عند الاتصال بسنا.','Connect to SANA to discover books.')}</p></section>;
 return <section className="ux-tool-page sana-books-page sana-library-v2" dir={ar?'rtl':'ltr'}>
  <header className="sana-library-hero">
   <div className="sana-library-stars" aria-hidden="true"><i/><i/><i/><i/><i/><i/></div>
   <div className="sana-library-hero-text">
    <div className="sana-library-kicker"><Sparkles size={15} aria-hidden="true"/> {say('مكتبة الإلهام · سنا','SANA · Inspiration library')}</div>
    <h1>{say('بين صفحات الكتب، تولد أفكار جديدة','New ideas begin between the pages')}</h1>
    <p>{say('لكل مشروع حكاية، ولكل حكاية مصادر إلهام. دع سنا تكتشف الكتب الأقرب لعالم مشروعك وشخصياته وأحداثه.','Every project has a story. Let SANA find books that inspire your characters, setting, and ideas.')}</p>
    <div className="sana-library-hero-note"><Feather size={16} aria-hidden="true"/>{say('ترشيحات مستوحاة من مشروعك، لا مجرد قائمة كتب عشوائية','Thoughtful recommendations shaped by your project')}</div>
   </div>
   <div className="sana-library-illustration" aria-hidden="true"><div className="sana-library-illustration-ring"/><div className="sana-library-illustration-glow"/><BookOpen size={100} strokeWidth={0.85}/><Sparkles className="sana-library-illustration-spark" size={25}/></div>
  </header>

  <div className="sana-library-discovery">
   <div className="sana-library-discovery-title"><div className="sana-library-discovery-icon"><Sparkles size={24} aria-hidden="true"/></div><div><span>{say('رحلة إلهام جديدة','A fresh source of inspiration')}</span><h2>{say('سنا تختار ما يلهم مشروعك','Let SANA find your next inspiration')}</h2><p>{say('تستكشف سنا تفاصيل المشروع ثم تقدم كتبًا حقيقية مع سبب ارتباط كل كتاب بأفكارك.','SANA explores your project and recommends real books, explaining why each one fits.')}</p></div></div>
   <label className="sana-library-focus"><span>{say('موضوع تود التركيز عليه','A topic to focus on')} <small>{say('(اختياري)','(optional)')}</small></span><textarea rows={2} maxLength={350} value={focus} disabled={busy} onChange={e=>setFocus(e.target.value)} placeholder={say('مثل: بناء الغموض، تطور الشخصيات، أو تصميم العوالم','e.g. mystery, character growth, or worldbuilding')}/></label>
   <div className="sana-library-discovery-actions"><button type="button" className="sana-library-primary" disabled={busy} onClick={inspire}>{busy?<LoaderCircle className="sana-library-spinner" size={18} aria-hidden="true"/>:showResults?<RefreshCcw size={18} aria-hidden="true"/>:<Sparkles size={18} aria-hidden="true"/>}<span>{busy?say('سنا تستكشف الكتب...','Finding inspiring books...'):showResults?say('اكتشف إلهامًا جديدًا','Discover more inspiration'):say('اكتشف الكتب الملهمة','Discover inspiring books')}</span></button><span className="sana-library-soft-note">{say('يمكن ترك الموضوع فارغًا لتختار سنا من تفاصيل المشروع.','Leave the topic blank to let SANA explore your project.')}</span></div>
   {busy&&<div role="status" aria-live="polite" className="sana-library-progress"><span className="sana-library-progress-star" aria-hidden="true"><Sparkles size={20}/></span><div><strong>{stageMessage}</strong><span>{say('تتغير هذه الخطوة بحسب تقدم البحث.','This updates as your search progresses.')}</span></div></div>}
  </div>

  {error&&<p className="sana-library-error" role="alert">{error}</p>}
  {showResults&&<section className="sana-library-results" aria-live="polite">
   <div className="sana-library-results-header"><div><span className="sana-library-section-kicker"><Compass size={15} aria-hidden="true"/>{say('اختيارات سنا','SANA’s selections')}</span><h2>{say('كتب قد تفتح لمشروعك آفاقًا جديدة','Books to spark your next idea')}</h2>{draft.summary&&<p>{draft.summary}</p>}</div><span className="sana-library-result-count">{recommendations.length} {say('كتب مقترحة','suggested books')}</span></div>
   {recommendations.length?<div className="sana-book-grid">{recommendations.map(({book,reason},i)=>{
    const url=moodboardReferenceUrl(book.previewLink);
    const authors=Array.isArray(book.authors)?book.authors.join('، '):'';
    return <article key={`${book.id||book.title}-${i}`} className="sana-book-card" style={{'--sana-book-order':i}}>
     <div className="sana-library-book-art">{url?<a href={url} target="_blank" rel="noopener noreferrer" aria-label={say(`فتح صفحة كتاب ${book.title}`,`Open ${book.title}'s page`)}><SanaBookCover book={book} lang={lang}/><span className="sana-library-book-link-icon"><ArrowUpRight size={18} aria-hidden="true"/></span></a>:<SanaBookCover book={book} lang={lang}/>}</div>
     <div className="sana-book-info"><div className="sana-library-book-meta"><span>{say('كتاب للإلهام','Inspiration pick')}</span>{book.publishedDate&&<small>{book.publishedDate}</small>}</div><h3>{url?<a href={url} target="_blank" rel="noopener noreferrer">{book.title}</a>:book.title}</h3><p className="sana-library-author">{authors||say('المؤلف غير متاح','Author not available')}</p>{reason&&<div className="sana-book-reason"><div><Sparkles size={15} aria-hidden="true"/><strong>{say('لماذا يناسب مشروعك؟','Why it fits your project')}</strong></div><p>{reason}</p></div>}</div>
    </article>;
   })}</div>:<div className="sana-library-empty-result"><BookOpen size={33} aria-hidden="true"/><h3>{say('لم تظهر كتب مناسبة بعد','No suitable books yet')}</h3><p>{say('يمكن تغيير الموضوع الاختياري أو تجربة الترشيح مرة أخرى.','Try changing the optional focus or discovering again.')}</p></div>}
  </section>}
  {!showResults&&!busy&&<div className="sana-library-idle" aria-hidden="true"><div className="sana-library-idle-mark"><BookOpen size={27} strokeWidth={1.5}/></div><p>{say('مساحة لاكتشاف ما وراء الفكرة، صفحة بعد صفحة.','A space to explore what lies beyond the idea, one page at a time.')}</p></div>}
  <footer className="sana-library-footer"><BookOpen size={16} aria-hidden="true"/>{say('الكتب للإلهام وتطوير الأفكار، مع احترام إبداع مؤلفيها.','Books are here to inspire your ideas, with respect for their authors.')}</footer>
 </section>;
}

// Project Understanding is an OPTIONAL AI context, not a required project setup step.
// UI uses ordinary lines; preserve existing complex JSON records without changing their structure.
const summaryFields=['actorsJson','componentsJson','constraintsJson'];
const emptySummary=()=>({projectGoal:'',actorsJson:'',componentsJson:'',constraintsJson:''});
function decodeSummary(raw){
 if(!raw)return {text:'',locked:false};
 try{
  const value=JSON.parse(raw);
  if(Array.isArray(value)&&value.every(x=>typeof x==='string'))return {text:value.join('\n'),locked:false};
  if(typeof value==='string')return {text:value,locked:false};
  return {text:'',locked:true};
 }catch{return {text:raw,locked:false}}
}
export function ProjectSummary({projectId,lang}){
 const ar=lang==='ar',say=(a,e)=>ar?a:e;
 const [expanded,setExpanded]=useState(false),[loaded,setLoaded]=useState(false),[saved,setSaved]=useState(null);
 const [draft,setDraft]=useState(emptySummary),[busy,setBusy]=useState(false),[error,setError]=useState(''),[message,setMessage]=useState('');
 const [locked,setLocked]=useState({});
 async function load(){
  setBusy(true);setError('');
  try{
   const result=await api(`/api/v1/project-understanding/get/project/${projectId}`);
   setSaved(result);
   setDraft({projectGoal:result.projectGoal||'',...Object.fromEntries(summaryFields.map(key=>[key,decodeSummary(result[key]).text]))});
   setLocked(Object.fromEntries(summaryFields.map(key=>[key,decodeSummary(result[key]).locked])));
  }catch(e){
   if(e.status===404||/project understanding not found/i.test(e.message)){
    setSaved(null);setDraft(emptySummary());setLocked({});
   }else setError(e.message);
  }finally{setLoaded(true);setBusy(false)}
 }
 async function save(e){
  e.preventDefault();setMessage('');setError('');
  if(!draft.projectGoal.trim()){
   setError(say('أضف هدف المشروع قبل حفظ الملخص، أو اتركه فارغًا دون حفظ.','Add a project goal to save this optional summary.'));
   return;
  }
  setBusy(true);
  try{
   const payload={projectGoal:draft.projectGoal.trim()};
   for(const key of summaryFields){
    // Legacy complex JSON is read-only and passed through unchanged.
    payload[key]=locked[key]?saved?.[key]||null:JSON.stringify(draft[key].split('\n').map(x=>x.trim()).filter(Boolean));
   }
   await api(`/api/v1/project-understanding/${saved?`update/${saved.id}`:`add/${projectId}`}`,request(saved?'PUT':'POST',payload));
   setMessage(say('تم حفظ ملخص المشروع.','Project summary saved.'));
   await load();
  }catch(e){setError(e.message)}finally{setBusy(false)}
 }
 async function remove(){
  if(!saved||!window.confirm(say('حذف ملخص المشروع المحفوظ؟ لن يتغير محتوى المشروع.','Delete the saved summary? Project content stays unchanged.')))return;
  setBusy(true);setError('');
  try{await api(`/api/v1/project-understanding/delete/${saved.id}`,{method:'DELETE'});setSaved(null);setDraft(emptySummary());setLocked({});setMessage(say('تم حذف الملخص.','Summary deleted.'))}
  catch(e){setError(e.message)}finally{setBusy(false)}
 }
 return <details className="sana-project-summary" open={expanded} onToggle={e=>{const isOpen=e.currentTarget.open;setExpanded(isOpen);if(isOpen&&!loaded&&API_MODE)load()}}>
  <summary>{say('ملخص المشروع (اختياري)','Project summary (optional)')}</summary>
  <p>{say('يساعد سنا على فهم هدف المشروع ومتطلباته. لا تحتاج إلى تعبئته لاستخدام أدوات المشروع أو الذكاء الاصطناعي.','Helps SANA understand your goals and constraints. It is not required to use the project or AI.')}</p>
  {!API_MODE?<p>{say('يتاح الحفظ عند الاتصال بالخادم.','Saving is available when connected to the backend.')}</p>:
  <form className="sana-summary-form" onSubmit={save}>
   {!loaded&&<p>{say('جارٍ تحميل الملخص...','Loading summary...')}</p>}
   {loaded&&<><label className="form-field"><span>{say('هدف المشروع','Project goal')}</span><textarea rows={3} maxLength={10000} value={draft.projectGoal} onChange={e=>setDraft({...draft,projectGoal:e.target.value})} placeholder={say('ما الذي تريد تحقيقه من هذا المشروع؟','What should this project achieve?')}/></label>
   {summaryFields.map((key,i)=><label className="form-field" key={key}><span>{(ar?['المشاركون أو الجمهور','الأجزاء الأساسية','المتطلبات والقيود']:['People / audience','Main components','Requirements & constraints'])[i]}</span>
    {locked[key]?<p className="sana-summary-legacy">{say('توجد بيانات منظمة محفوظة مسبقًا في هذا الحقل. ستُحفظ كما هي دون تعديل.','This field contains existing structured data; it will be preserved unchanged.')}</p>:<textarea rows={2} maxLength={10000} value={draft[key]} onChange={e=>setDraft({...draft,[key]:e.target.value})} placeholder={say('اكتب عنصرًا في كل سطر (اختياري)','One item per line (optional)')}/>}
   </label>)}
   <div className="modal-actions"><button type="submit" className="btn btn-primary" disabled={busy}>{say('حفظ الملخص','Save summary')}</button>{saved&&<button type="button" className="btn btn-ghost" disabled={busy} onClick={remove}>{say('حذف الملخص','Delete summary')}</button>}</div></>}
   {error&&<p className="ux-error" role="alert">{error}</p>}{message&&<p className="sana-summary-success" role="status">{message}</p>}
  </form>}
 </details>;
}
