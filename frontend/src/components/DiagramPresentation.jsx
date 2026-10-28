import React from 'react';
import {tr} from '../lib/i18n';
export const graphPresentation={STORY_MAP:'story',CREATIVITY:'story',BRANCHING_STORY:'narrative',ARCHITECTURE:'architecture',ERD:'erd',CLASS_DIAGRAM:'uml',USE_CASE:'usecase',BUSINESS_MODEL_CANVAS:'business',TARGET_AUDIENCE:'audience',SWOT:'swot',COMPETITOR_ANALYSIS:'competitor',RISK_ANALYSIS:'risk',MIND_MAP:'mindmap'};
export function edgeEndpoints(a,b,width=220,height=120){const dx=b.x-a.x,dy=b.y-a.y,ratio=1/Math.max(Math.abs(dx)/(width/2+5),Math.abs(dy)/(height/2+5),1);return {x1:a.x+width/2+dx*ratio,y1:a.y+height/2+dy*ratio,x2:b.x+width/2-dx*ratio,y2:b.y+height/2-dy*ratio}}
// Visual-only geometry: technical nodes do not all share the size or silhouette of story cards.
export function techNodeBounds(node,diagramType,relations=[]){
 if(diagramType==='ERD'){
  const columns=Math.min(10,Array.isArray(node?.properties?.attributes)?node.properties.attributes.length:0);
  const links=relations.filter(r=>String(r.source)===String(node?.id)||String(r.target)===String(node?.id)).length;
  return {width:302,height:Math.max(155,82+columns*34+(links?30+Math.min(3,links)*22:0))};
 }
 if(diagramType==='CLASS_DIAGRAM')return {width:264,height:Math.max(144,76+Math.min(7,Array.isArray(node?.properties?.attributes)?node.properties.attributes.length:0)*24+Math.min(5,Array.isArray(node?.properties?.methods)?node.properties.methods.length:0)*24)};
 if(diagramType==='USE_CASE')return {width:node?.type==='actor'?160:244,height:node?.type==='actor'?160:115,ellipse:node?.type==='use_case'};
 return {width:282,height:node?.type==='database'?174:156};
}
// Use the actual visible silhouette, especially the actor stick figure (not its 160px wrapper).
export function techEdgeEndpoints(a,b,aNode,bNode,diagramType,relations=[]){
 const A=techNodeBounds(aNode,diagramType,relations),B=techNodeBounds(bNode,diagramType,relations);
 const c1={x:a.x+A.width/2,y:a.y+A.height/2},c2={x:b.x+B.width/2,y:b.y+B.height/2};
 const dx=c2.x-c1.x,dy=c2.y-c1.y;
 if(Math.abs(dx)+Math.abs(dy)<.001)return {x1:c1.x,y1:c1.y,x2:c2.x,y2:c2.y};
 function intersect(node,center,bounds,sign){
  // The SVG actor is about 75x84px centered above its caption.
  // Anchor to the visible hand/torso rather than the transparent outer box.
  if(diagramType==='USE_CASE'&&node?.type==='actor'){
   const handY=center.y-12;
   if(Math.abs(dx)>=Math.abs(dy)*.55)
    return {x:center.x+(sign*Math.sign(dx))*31,y:handY};
   return {x:center.x,y:center.y+(sign*Math.sign(dy)>0?16:-39)};
  }
  const rx=bounds.width/2,ry=bounds.height/2;
  const t=bounds.ellipse?1/Math.sqrt(dx*dx/(rx*rx)+dy*dy/(ry*ry)):Math.min(rx/Math.max(.001,Math.abs(dx)),ry/Math.max(.001,Math.abs(dy)));
  return {x:center.x+sign*dx*t,y:center.y+sign*dy*t};
 }
 const p=intersect(aNode,c1,A,1),q=intersect(bNode,c2,B,-1);
 return {x1:p.x,y1:p.y,x2:q.x,y2:q.y};
}

const safeRows=value=>Array.isArray(value)?value:[];
const textValue=value=>typeof value==='string'?value:value==null?'':String(value);
const techTags={client:'Client / UI',application:'Application',service:'Service / API',database:'Database',queue:'Message Queue',component:'Component',external_system:'External System'};
export function DiagramNodeContent({element,diagramType,t,lang='ar',relations=[],nodes=[]}){
 const p=element.properties||{},kind=graphPresentation[diagramType]||'story';
 const ar=lang==='ar',attributes=safeRows(p.attributes),methods=safeRows(p.methods),values=safeRows(p.values);
 if(kind==='architecture'){
  const links=relations.filter(r=>String(r.source)===String(element.id)||String(r.target)===String(element.id)).length;
  return <div className={`spark-node-content spark-tech-content spark-architecture-content spark-type-${element.type}`} dir="ltr">
   <div className="spark-architecture-overline"><span className="spark-architecture-indicator"/>{techTags[element.type]||'Component'}</div>
   <strong className="spark-architecture-name" dir="auto">{element.name}</strong>
   {element.description&&<p dir="auto">{element.description}</p>}
   <div className="spark-architecture-foot">{p.technology?<span className="spark-tech-tag" dir="auto">{textValue(p.technology)}</span>:<span className="spark-architecture-role">{element.type==='database'?'DATA STORE':element.type==='queue'?'ASYNC MESSAGING':element.type==='client'?'PRESENTATION':element.type==='external_system'?'INTEGRATION':element.type==='service'?'BUSINESS LOGIC':'SYSTEM MODULE'}</span>}
   {links>0&&<span className="spark-architecture-links">{links} {links===1?'LINK':'LINKS'}</span>}</div>
  </div>;
 }
 if(kind==='erd'){
  const links=relations.filter(r=>String(r.source)===String(element.id)||String(r.target)===String(element.id));
  const tableName=id=>nodes.find(n=>String(n.id)===String(id))?.name||'Table';
  return <div className="spark-node-content spark-tech-content spark-erd-content" dir="ltr">
   <div className="spark-tech-table-head"><small>ENTITY / TABLE</small><strong dir="auto">{element.name}</strong><span>{attributes.length} {attributes.length===1?'COLUMN':'COLUMNS'}</span></div>
   <div className="spark-tech-table-columns"><span>KEY</span><span>COLUMN</span><span>TYPE</span></div>
   <div className="spark-tech-rows">{attributes.length?attributes.slice(0,10).map((a,i)=>{
    const explicitFk=Boolean(a.foreignKey===true||a.references||a.referencesTable||a.referencedTable||a.referenceTable);
    return <div key={i} className={`spark-tech-attribute ${a.primaryKey?'is-primary':''} ${explicitFk?'is-foreign':''}`}>
     <span className="spark-tech-key">{a.primaryKey?'PK':explicitFk?'FK':'—'}</span>
     <span dir="auto">{a.name||'—'}{a.nullable===false&&<b className="spark-tech-required" title="NOT NULL">*</b>}</span>
     <code dir="ltr">{a.dataType||'—'}</code>
    </div>;
   }):<div className="spark-tech-no-rows">No columns yet</div>}
   {attributes.length>10&&<small className="spark-tech-more">+{attributes.length-10} more</small>}
   </div>
   {links.length>0&&<div className="spark-erd-relationships"><small>TABLE RELATIONSHIPS</small>{links.slice(0,3).map(r=>{
    const outgoing=String(r.source)===String(element.id),other=tableName(outgoing?r.target:r.source);
    const cardinality=({ONE_TO_ONE:'1:1',ONE_TO_MANY:outgoing?'1:N':'N:1',MANY_TO_MANY:'N:M'})[r.type]||r.type;
    return <span key={r.id}><b>{cardinality}</b><span dir="auto">{other}</span>{r.type==='ONE_TO_MANY'&&!outgoing&&<em title="Relationship implies a foreign-key side; the backend does not identify a specific FK column">FK side</em>}</span>;
   })}{links.length>3&&<small>+{links.length-3} more</small>}</div>}
  </div>;
 }
 if(kind==='uml')return <div className="spark-node-content spark-tech-content spark-uml-content" dir={ar?'rtl':'ltr'}>
  <div className="spark-tech-uml-head">{element.type!=='class'&&<small>«{element.type==='interface'?'interface':element.type==='abstract_class'?'abstract':'enumeration'}»</small>}<strong dir="auto">{element.name}</strong></div>
  <section className="spark-tech-uml-section" aria-label="Attributes">
   {attributes.length?attributes.slice(0,7).map((a,i)=><div key={i} dir="ltr"><span className="spark-tech-visibility">{({PRIVATE:'−',PROTECTED:'#',PACKAGE:'~',PUBLIC:'+'})[a.visibility]||'+'}</span> {a.name||'—'}<span className="spark-tech-type"> : {a.type||'—'}</span>{a.static?' ̲':''}</div>):<small>{'— Attributes —'}</small>}
   {attributes.length>7&&<small>+{attributes.length-7}</small>}
  </section>
  <section className="spark-tech-uml-section" aria-label="Methods">
   {methods.length?methods.slice(0,5).map((m,i)=><div key={i} dir="ltr"><span className="spark-tech-visibility">{({PRIVATE:'−',PROTECTED:'#',PACKAGE:'~',PUBLIC:'+'})[m.visibility]||'+'}</span> {m.name||'—'}({safeRows(m.parameters).map(x=>x.name).filter(Boolean).join(', ')})<span className="spark-tech-type"> : {m.returnType||'void'}</span></div>):<small>{'— Methods —'}</small>}
   {methods.length>5&&<small>+{methods.length-5}</small>}
   {values.length>0&&<div className="spark-tech-enum-values">{values.slice(0,5).map((v,i)=><span key={i}>{textValue(v)}</span>)}</div>}
  </section>
 </div>;
 if(kind==='story'){
  const related=relations.filter(r=>String(r.source)===String(element.id)||String(r.target)===String(element.id));
  const metadata={
   characters:[[ar?'الدور':'Role',element.role||p.role],[ar?'الهدف':'Goal',p.goals]],
   worlds:[[ar?'الأجواء':'Atmosphere',p.atmosphere],[ar?'التاريخ':'History',p.history]],
   locations:[[ar?'نوع المكان':'Place type',element.locationType],[ar?'العالم':'World',p.world]],
   events:[[ar?'الترتيب':'Sequence',element.presentationOrder],[ar?'الزمن':'Timeline',element.chronologicalOrder??p.inWorldTime]],
   rules:[[ar?'النطاق':'Scope',p.scope],[ar?'النتيجة':'Consequence',p.consequences]]
  };
  const facts=(metadata[element.type]||[]).filter(([,v])=>v!==undefined&&v!==null&&String(v).trim()!=='').slice(0,2);
  return <div className={`spark-node-content sana-story-content sana-story-${element.type}`} dir={ar?'rtl':'ltr'}>
   <small className="sana-story-kind"><span aria-hidden="true">✦</span>{t(element.type)}</small>
   <strong dir="auto">{element.name}</strong>
   {element.description&&<p dir="auto">{element.description}</p>}
   {facts.length>0&&<div className="sana-story-facts">{facts.map(([key,value])=><span key={key}><b>{key}</b><em dir="auto">{typeof value==='object'?Array.isArray(value)?value.join('، '):'—':String(value)}</em></span>)}</div>}
   <div className="sana-story-footer"><span>{element.type==='events'?(ar?'مشهد قصصي':'Story event'):element.type==='rules'?(ar?'قانون العالم':'World rule'):element.type==='locations'?(ar?'مكان في العالم':'World location'):element.type==='worlds'?(ar?'عالم قصصي':'Story world'):(ar?'ملف شخصية':'Character profile')}</span><span>{related.length} {ar?'روابط':'links'}</span></div>
  </div>;
 }
 if(kind==='usecase')return <div className={`spark-node-content spark-tech-content spark-usecase-content spark-type-${element.type}`} dir={ar?'rtl':'ltr'}>
  {element.type==='actor'&&<svg viewBox="0 0 62 72" className="spark-tech-actor-icon" aria-hidden="true"><circle cx="31" cy="11" r="9"/><path d="M31 20V45M10 32H52M31 45L14 68M31 45L48 68"/></svg>}
  <strong dir="auto">{element.name}</strong>
  {element.description&&<p>{element.description}</p>}
 </div>;
 return <div className={`spark-node-content spark-${kind} spark-type-${element.type}`}><small className="spark-node-kind"><span aria-hidden="true">✦</span>{t(element.type)}</small><strong>{element.name}</strong><p>{element.description||''}</p></div>;
}
const advanced={characters:['aliases','appearance','traits','strengths','weaknesses','skills','history','goals','fears','innerConflict','secrets','characterValues','characterArc'],worlds:['history','lore','atmosphere'],locations:[],rules:['scope','conditions','exceptions','consequences'],events:['inWorldTime']};
export function DiagramPropertiesEditor({type,properties,onChange,lang,creative}){
 const t=k=>tr(lang,k),fields=creative?(advanced[type]||[]):type==='entity'?['attributes']:['class','abstract_class','interface','enum'].includes(type)?['attributes','methods',...(type==='enum'?['values']:[])]:type==='risk'?['likelihood','impact']:['client','application','service','database','queue','component','external_system'].includes(type)?['technology']:[];
 if(!fields.length)return null;
 const tech=type==='entity'||['class','abstract_class','interface','enum','client','application','service','database','queue','component','external_system'].includes(type);
 return <details className="spark-properties" open={['entity','class','abstract_class','interface','enum'].includes(type)}><summary>{t('advancedProperties')}</summary>{fields.map(key=>['attributes','methods','values'].includes(key)?<StructuredProperties key={key} field={key} type={type} value={properties[key]} lang={lang} onChange={value=>onChange({...properties,[key]:value})}/>:<label className="form-field" key={key}><span>{t(key)}</span><textarea rows={2} value={properties[key]??''} onChange={e=>onChange({...properties,[key]:e.target.value})}/></label>)}</details>
}
function StructuredProperties({field,type,value,onChange,lang}){
 const t=k=>tr(lang,k),ar=lang==='ar',tech=['entity','class','abstract_class','interface','enum'].includes(type);let rows=[];try{rows=typeof value==='string'?JSON.parse(value||'[]'):value||[];if(!Array.isArray(rows))rows=[]}catch{return <label>{tech?({attributes:type==='entity'?'Columns':'Attributes',methods:'Methods',values:'Values'})[field]:t(field)}<textarea value={value} onChange={e=>onChange(e.target.value)}/></label>}
 const update=(i,patch)=>onChange(JSON.stringify(rows.map((r,j)=>j===i?{...r,...patch}:r)));
 const add=()=>onChange(JSON.stringify([...rows,field==='values'?'':field==='attributes'?type==='entity'?{name:'',dataType:'',primaryKey:false,nullable:true}:{name:'',type:'',visibility:'PUBLIC',static:false}:{name:'',returnType:'',parameters:[],visibility:'PUBLIC',static:false,abstract:false}]));
 return <section className="spark-structured"><h4>{tech?({attributes:type==='entity'?'Columns':'Attributes',methods:'Methods',values:'Values'})[field]:t(field)}</h4>{rows.map((row,i)=><div className="spark-structured-row" key={i}>{field==='values'?<input aria-label={t('value')} required value={row} onChange={e=>onChange(JSON.stringify(rows.map((v,j)=>i===j?e.target.value:v)))}/>:<><input required aria-label={tech?'Name':t('name')} placeholder={tech?'Name':t('name')} value={row.name||''} onChange={e=>update(i,{name:e.target.value})}/><input required aria-label={tech?(field==='methods'?'Return type':'Data type'):field==='methods'?(ar?'نوع الإرجاع':'Return type'):t('type')} placeholder={tech?(field==='methods'?'Return type':'Data type'):field==='methods'?(ar?'نوع الإرجاع':'Return type'):t('type')} value={row[field==='methods'?'returnType':type==='entity'?'dataType':'type']||''} onChange={e=>update(i,{[field==='methods'?'returnType':type==='entity'?'dataType':'type']:e.target.value})}/>{type==='entity'?<><label><input type="checkbox" checked={!!row.primaryKey} onChange={e=>update(i,{primaryKey:e.target.checked})}/>PK</label><label><input type="checkbox" checked={!!row.nullable} onChange={e=>update(i,{nullable:e.target.checked})}/>{'Nullable'}</label></>:<><select aria-label={'Visibility'} value={row.visibility||'PUBLIC'} onChange={e=>update(i,{visibility:e.target.value})}>{['PUBLIC','PRIVATE','PROTECTED','PACKAGE'].map(v=><option key={v}>{v}</option>)}</select><label><input type="checkbox" checked={!!row.static} onChange={e=>update(i,{static:e.target.checked})}/>static</label>{field==='methods'&&<><label><input type="checkbox" checked={!!row.abstract} onChange={e=>update(i,{abstract:e.target.checked})}/>abstract</label><label>{'Parameters: name:type, ...'}<input value={(row.parameters||[]).map(p=>`${p.name}:${p.type}`).join(', ')} onChange={e=>update(i,{parameters:e.target.value?e.target.value.split(',').map(v=>{const [name,type]=v.trim().split(':');return {name:name||'',type:type||''}}):[]})}/></label></>}</>}</>}<button type="button" onClick={()=>onChange(JSON.stringify(rows.filter((_,j)=>j!==i)))}>{t('remove')}</button></div>)}<button type="button" onClick={add}>{t('add')}</button></section>
}
export function organizedGraph(project){
 const positions={},areas=[],nodes=project.elements||[],counts={};nodes.forEach(n=>counts[n.type]=(counts[n.type]||0)+1);
 if(project.activeType==='BUSINESS_MODEL_CANVAS'){
  const h=Math.max(250,...['key_activities','key_resources','customer_relationships','channels'].map(t=>(counts[t]||0)*160+60));const tall=Math.max(h*2,...['key_partnerships','value_proposition','customer_segment'].map(t=>(counts[t]||0)*160+60));
  const cells={key_partnerships:[20,20,250,tall],key_activities:[290,20,250,h-10],key_resources:[290,h+20,250,tall-h],value_proposition:[560,20,250,tall],customer_relationships:[830,20,250,h-10],channels:[830,h+20,250,tall-h],customer_segment:[1100,20,250,tall],cost_structure:[20,tall+45,655,Math.max(250,(counts.cost_structure||0)*160+60)],revenue_streams:[695,tall+45,655,Math.max(250,(counts.revenue_streams||0)*160+60)]};
  for(const [type,[x,y,width,height]] of Object.entries(cells)){areas.push({type,x,y,width,height});nodes.filter(n=>n.type===type).forEach((n,i)=>positions[n.id]={x:x+14,y:y+48+i*160})}
 }else if(project.activeType==='SWOT'){
  const h=Math.max(330,...Object.values(counts).map(n=>Math.ceil(n/2)*170+70));['swot_strength','swot_weakness','swot_opportunity','swot_threat'].forEach((type,i)=>{const x=20+(i%2)*560,y=20+Math.floor(i/2)*(h+20);areas.push({type,x,y,width:540,height:h});nodes.filter(n=>n.type===type).forEach((n,j)=>positions[n.id]={x:x+20+(j%2)*250,y:y+60+Math.floor(j/2)*170})});
 }else if(project.activeType==='ARCHITECTURE'){
  // Visual-only preview of a layered system. Never silently persist coordinates.
  const lanes=[{types:['client','application'],ar:'01 · CLIENT & APP',en:'01 · CLIENT & APP'},{types:['service','component'],ar:'02 · SERVICES & MODULES',en:'02 · SERVICES & MODULES'},{types:['database','queue'],ar:'03 · DATA & MESSAGING',en:'03 · DATA & MESSAGING'},{types:['external_system'],ar:'04 · INTEGRATIONS',en:'04 · INTEGRATIONS'}];
  lanes.forEach((lane,i)=>{const elements=nodes.filter(n=>lane.types.includes(n.type));const x=25+i*350;areas.push({type:lane.types[0],title:{ar:lane.ar,en:lane.en},x,y:25,width:335,height:Math.max(440,elements.length*204+95)});elements.forEach((n,j)=>positions[n.id]={x:x+26,y:94+j*204})});
 }else if(project.activeType==='ERD'){
  // Tables may grow with their real column counts; reserve row heights accordingly.
  const columns=3,rowHeights=[];
  nodes.forEach((n,i)=>{const count=Array.isArray(n.properties?.attributes)?n.properties.attributes.length:0;const h=Math.max(180,125+Math.min(10,count)*25);const row=Math.floor(i/columns);rowHeights[row]=Math.max(rowHeights[row]||0,h)});
  nodes.forEach((n,i)=>{const row=Math.floor(i/columns),col=i%columns;positions[n.id]={x:45+col*340,y:80+rowHeights.slice(0,row).reduce((sum,v)=>sum+v+38,0)}});
 }else if(project.activeType==='CLASS_DIAGRAM'){
  const types=['interface','abstract_class','class','enum'];let index=0;
  for(const type of types){const group=nodes.filter(n=>n.type===type);for(const n of group){const col=index%3,row=Math.floor(index/3);positions[n.id]={x:45+col*340,y:70+row*350};index++}}
 }else if(project.activeType==='USE_CASE'){
  const actors=nodes.filter(n=>n.type==='actor'),cases=nodes.filter(n=>n.type==='use_case'),other=nodes.filter(n=>!['actor','use_case'].includes(n.type));
  actors.forEach((n,i)=>positions[n.id]={x:45,y:155+i*190});
  cases.forEach((n,i)=>positions[n.id]={x:385+(i%2)*335,y:120+Math.floor(i/2)*200});
  other.forEach((n,i)=>positions[n.id]={x:740,y:150+i*190});
 }else if(project.activeType==='MIND_MAP'){
  const root=nodes.find(n=>n.type==='central_topic'),children=new Map(),visited=new Set();for(const r of project.relations.filter(r=>r.type==='BRANCH'))children.set(r.source,[...(children.get(r.source)||[]),r.target]);let row=0;function visit(id,depth){if(visited.has(id))return;visited.add(id);positions[id]={x:50+depth*300,y:80+row++*155};for(const child of children.get(id)||[])visit(child,depth+1)}if(root)visit(root.id,0);for(const n of nodes)if(!visited.has(n.id))visit(n.id,1);
 }else if(['STORY_MAP','CREATIVITY','BRANCHING_STORY'].includes(project.activeType)){
  // Arrange connected story elements together. Preserve relations and saved data; these are preview-only coordinates.
  const byId=new Map(nodes.map(n=>[String(n.id),n])),adj=new Map(nodes.map(n=>[String(n.id),new Set()]));
  const incoming=new Map(nodes.map(n=>[String(n.id),0]));
  for(const rel of project.relations||[]){
   const from=String(rel.source),to=String(rel.target);
   if(!byId.has(from)||!byId.has(to)||from===to)continue;
   adj.get(from).add(to);adj.get(to).add(from);incoming.set(to,(incoming.get(to)||0)+1);
  }
  const rank={worlds:0,characters:1,locations:2,events:3,rules:4};
  const order=(a,b)=>(rank[a.type]??10)-(rank[b.type]??10)||(a.y??0)-(b.y??0)||String(a.id).localeCompare(String(b.id));
  const unvisited=new Set(byId.keys());let top=75;
  while(unvisited.size){
   const seeds=[...unvisited].map(id=>byId.get(id)).sort((a,b)=>{
    const ai=incoming.get(String(a.id))===0?0:1,bi=incoming.get(String(b.id))===0?0:1;
    return ai-bi||order(a,b);
   });
   const root=seeds[0],depth=new Map([[String(root.id),0]]),queue=[String(root.id)];unvisited.delete(String(root.id));
   for(let i=0;i<queue.length;i++){
    const parent=queue[i];const next=[...adj.get(parent)].filter(id=>unvisited.has(id)).map(id=>byId.get(id)).sort(order);
    for(const child of next){const id=String(child.id);if(!unvisited.has(id))continue;unvisited.delete(id);depth.set(id,(depth.get(parent)||0)+1);queue.push(id)}
   }
   const layers=new Map();for(const id of queue){const layer=depth.get(id)||0;layers.set(layer,[...(layers.get(layer)||[]),byId.get(id)])}
   // A parent-barycenter sort reduces overlapping lines between neighboring layers.
   for(const layer of [...layers.keys()].sort((a,b)=>a-b)){
    const items=layers.get(layer);if(layer){items.sort((a,b)=>{
     const parents=n=>[...(adj.get(String(n.id))||[])].filter(id=>depth.get(id)===layer-1);
     const average=n=>{const values=parents(n).map(id=>{const parent=byId.get(id),arr=layers.get(layer-1)||[];return arr.findIndex(x=>String(x.id)===id)}).filter(i=>i>=0);return values.length?values.reduce((sum,v)=>sum+v,0)/values.length:100};
     return average(a)-average(b)||order(a,b);
    })}
    items.forEach((n,i)=>{positions[n.id]={x:60+layer*335,y:top+i*215}});
   }
   top+=Math.max(1,...[...layers.values()].map(arr=>arr.length))*215+100;
  }
 }else nodes.forEach((n,i)=>positions[n.id]={x:50+(i%3)*300,y:70+Math.floor(i/3)*250});
 return {positions,areas};
}
