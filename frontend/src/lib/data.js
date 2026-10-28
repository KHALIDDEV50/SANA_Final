import {TYPES, CREATIVE_TYPES, typesForDomain, starterNodes, starterConnections} from './templates';
export {TYPES};
export const API_MODE = import.meta.env.VITE_SANA_MODE === 'api';
const BASE = (import.meta.env.VITE_SANA_API_URL || '').replace(/\/$/,'');
const STORE = 'sana-demo-v1';
const now = () => new Date().toISOString();
const clone = value => JSON.parse(JSON.stringify(value));
const makeId = () => Date.now() * 100 + Math.floor(Math.random()*100);
const examples = [
 {id:101,name:'مدينة أركان',description:'في مدينة تختفي منها الذكريات، تبحث ليان عن الحقيقة وراء مكتبة لا يتذكر أحد وجودها.',domain:'writing',status:'ACTIVE',createdAt:'2026-10-01',updatedAt:'2026-10-22',elements:[
  {id:1001,type:'characters',name:'ليان',description:'بطلة تبحث عن ذكريات عائلتها.',role:'بطلة',age:24,x:110,y:115},
  {id:1002,type:'characters',name:'راشد',description:'أمين المكتبة الغامض.',role:'مساعد',age:45,x:445,y:105},
  {id:1003,type:'worlds',name:'عالم أركان',description:'عالم تتحكم فيه الذاكرة.',x:770,y:140},
  {id:1004,type:'locations',name:'مكتبة الذكريات',description:'مكتبة سرية.',x:410,y:335},
  {id:1005,type:'rules',name:'قانون الذاكرة',description:'كل سحر يفقد صاحبه ذكرى.',x:770,y:425},
  {id:1006,type:'events',name:'اكتشاف المكتبة',description:'أول دليل للغز.',presentationOrder:1,x:90,y:425}],
  relations:[{id:1,source:1001,target:1004,type:'VISITS',label:'تبحث عن'},{id:2,source:1002,target:1004,type:'LIVES_IN',label:'يحرس'},{id:3,source:1004,target:1006,type:'CONNECTED_TO',label:'مكان الحدث'}]},
 {id:102,name:'بوابة الرمال',description:'لعبة مغامرات عربية مستوحاة من الصحراء.',domain:'games',status:'ACTIVE',createdAt:'2026-10-05',updatedAt:'2026-10-20',elements:[{id:2001,type:'characters',name:'سهيل',description:'رحالة شجاع',role:'بطل',age:23,x:180,y:160},{id:2002,type:'worlds',name:'الصحراء',description:'مدن منسية',x:510,y:200}],relations:[]},
 {id:103,name:'معمارية سنا',description:'تصميم مكونات النظام وروابطها.',domain:'technology',status:'DRAFT',createdAt:'2026-10-10',updatedAt:'2026-10-20',elements:[{id:3001,type:'application',name:'REST API',description:'Spring Boot',x:135,y:180},{id:3002,type:'service',name:'خدمة المشروع',description:'Project Service',x:430,y:180},{id:3003,type:'database',name:'قاعدة البيانات',description:'MySQL',x:750,y:180}],relations:[{id:3011,source:3001,target:3002,type:'CONNECTS_TO',label:'يتواصل مع'},{id:3012,source:3002,target:3003,type:'CONNECTS_TO',label:'يخزن في'}]},
 {id:104,name:'فكرة متجر سعودي',description:'نموذج أولي لفكرة تجارية محلية.',domain:'business',status:'DRAFT',createdAt:'2026-10-12',updatedAt:'2026-10-20',elements:[{id:4001,type:'value_proposition',name:'المتجر',description:'منتجات محلية',x:150,y:160},{id:4002,type:'customer_segment',name:'العملاء',description:'الجمهور المستهدف',x:480,y:160},{id:4003,type:'channels',name:'المتجر الإلكتروني',description:'البيع عبر الإنترنت',x:320,y:370}],relations:[]}
];
function load(){try{const parsed=JSON.parse(localStorage.getItem(STORE));if(Array.isArray(parsed))return parsed;}catch{} return clone(examples)}
function persist(projects){localStorage.setItem(STORE,JSON.stringify(projects));return projects}
function projectWithCounts(p){return {...p,elementCount:(p.elements?.length ?? (p.outputs?.every(o=>Array.isArray(o.nodes))?p.outputs.reduce((n,o)=>n+o.nodes.length,0):null)),relationCount:p.relations?.length ?? 0}}
// ---------- Login (HTTP Basic) ----------
// The backend uses Basic Auth: every request carries "Basic base64(email:password)".
// It is kept in sessionStorage, so it is forgotten when the tab closes.
const AUTH_KEY='sana-auth';
const toBasic=(email,password)=>btoa(String.fromCharCode(...new TextEncoder().encode(`${email}:${password}`)));
export const authApi={
 hasCredentials:()=>{try{return Boolean(sessionStorage.getItem(AUTH_KEY))}catch{return false}},
 me:()=>api('/api/v1/auth/me'),
 async login(email,password){try{sessionStorage.setItem(AUTH_KEY,toBasic(email,password))}catch{}try{return await api('/api/v1/auth/me')}catch(e){authApi.forget();throw e}},
 async register(name,email,password){await api('/api/v1/auth/register',{method:'POST',body:JSON.stringify({name,email,passwordHash:password})});return authApi.login(email,password)},
 async logout(){try{await api('/api/v1/auth/logout',{method:'POST'})}catch{}authApi.forget()},
 forget(){try{sessionStorage.removeItem(AUTH_KEY)}catch{}},
};
export async function api(path,opts={}){
 let token=null;try{token=sessionStorage.getItem(AUTH_KEY)}catch{}
 const response=await fetch(`${BASE}${path}`,{...opts,credentials:'include',headers:{'Content-Type':'application/json',...(token?{Authorization:`Basic ${token}`}:{}),...(opts.headers||{})}});
 // A 401 means the login is missing or wrong: forget it and let the app show the login screen
 if(response.status===401&&!path.startsWith('/api/v1/auth/')){authApi.forget();window.dispatchEvent(new Event('sana-unauthorized'))}
 if(!response.ok){let detail='';try{const j=await response.json();detail=j.message||(typeof j.error==='string'?j.error:j.error?.message)||j.detail||'';}catch{}const error=new Error(detail||`HTTP ${response.status} — ${path}`);error.status=response.status;throw error}
 if(response.status===204)return null;const text=await response.text();if(!text)return null;try{return JSON.parse(text)}catch{return text}
}
const json=(method,payload)=>({method,body:JSON.stringify(payload)});
const entityPaths={characters:'character-profile',worlds:'world',locations:'location',rules:'world-rule',events:'story-event'};

// ---------- UI <-> backend mapping ----------
// The UI keeps its own domain names; the backend has TECH, BUSINESS, EDUCATION and CREATIVITY.
const BACKEND_DOMAIN={writing:'CREATIVITY',games:'CREATIVITY',technology:'TECH',business:'BUSINESS',education:'EDUCATION'};
// The diagram a new project starts with
const PRIMARY_DIAGRAM={writing:'STORY_MAP',games:'STORY_MAP',technology:'ARCHITECTURE',business:'BUSINESS_MODEL_CANVAS',education:'MIND_MAP'};
// Creativity node types use plural UI names; every other node type is the backend name in lower case
const STORY_TYPES={characters:'CHARACTER',worlds:'WORLD',locations:'LOCATION',rules:'WORLD_RULE',events:'STORY_EVENT'};
const toBackendType=type=>STORY_TYPES[type]||String(type).toUpperCase();
const toUiType=type=>Object.keys(STORY_TYPES).find(k=>STORY_TYPES[k]===type)||String(type).toLowerCase();
export const backendDomain=domain=>BACKEND_DOMAIN[String(domain||'').toLowerCase()]||({'CREATIVITY':'CREATIVITY','TECH':'TECH','BUSINESS':'BUSINESS','EDUCATION':'EDUCATION'}[String(domain||'').toUpperCase()]||'CREATIVITY');
const uiDomainKey=id=>`sana-ui-domain-${id}`;
const activeDiagramKey=id=>`sana-active-diagram-${id}`;
const branchingDiagramKey=id=>`sana-branching-diagram-${id}`;
// A project may contain several branching stories. Remember the exact output
// approved by the AI instead of silently displaying the first older tree.
export function rememberBranchingOutput(projectId,outputId){
 const id=Number(outputId);
 if(Number.isSafeInteger(id)&&id>0)storage.set(branchingDiagramKey(projectId),id);
}
export function selectedBranchingDiagram(project){
 const options=(project?.diagrams||[]).filter(d=>d.outputType==='BRANCHING_STORY');
 const saved=storage.get(branchingDiagramKey(project?.id));
 return options.find(d=>String(d.id)===saved)||options.sort((a,b)=>Number(b.id)-Number(a.id))[0]||null;
}
const storage={get:k=>{try{return localStorage.getItem(k)}catch{return null}},set:(k,v)=>{try{localStorage.setItem(k,String(v))}catch{}}};
// writing and games are both CREATIVITY; remember which one the user picked, else guess from a branching diagram
function toUiDomain(p){
 const raw=String(p.domain||'').toUpperCase();
 if(raw==='TECH'||raw==='TECHNOLOGY')return 'technology';if(raw==='BUSINESS')return 'business';if(raw==='EDUCATION')return 'education';
 // Old SANA clients stored writing/games as backend domain values. Keep their UI identity.
 if(raw==='GAMES')return 'games';if(raw==='WRITING')return 'writing';
 const hint=storage.get(uiDomainKey(p.id));if(hint==='writing'||hint==='games')return hint;
 return (p.outputs||[]).some(o=>['BRANCHING_STORY','GAME'].includes(o.outputType))?'games':'writing';
}
// Existing project records are read-only during navigation. A GET must never
// silently PUT or migrate projects: migration belongs to an explicit admin step.
// Node types the add-element palette offers: the active diagram's types in API mode
export const elementTypes=project=>project?.nodeTypes?.length?project.nodeTypes:typesForDomain(project?.domain);
let diagramTypesCache=null;
export async function diagramTypes(){if(!diagramTypesCache)diagramTypesCache=await api('/api/v1/diagram/types');return diagramTypesCache||[]}

// ---------- API reads ----------
async function getApiProjects(){const ps=await api('/api/v1/project/get/my');return (Array.isArray(ps)?ps:[]).map(p=>({...p,backendDomain:p.domain,domain:toUiDomain(p)}));}
async function listDiagrams(projectId){const list=await api(`/api/v1/diagram/get/project/${projectId}`);return (Array.isArray(list)?list:[]).map(d=>({id:d.id,title:d.title,outputType:d.outputType}))}
async function createDiagram(projectId,title,diagramType){
 await api(`/api/v1/diagram/add/${projectId}`,json('POST',{title,diagramType}));
 const created=(await listDiagrams(projectId)).filter(d=>d.outputType===diagramType).sort((a,b)=>b.id-a.id)[0];
 if(!created)throw new Error('Diagram created but could not be found; reload the project');
 return created;
}
function toElement(node,outputId,storyMap,entityId,rawNode){
 const pr=node.properties||{};
 const base={id:node.id,nodeId:node.id,outputId,type:toUiType(node.type),name:pr.label||node.type,description:pr.description||pr.shortDescription||pr.narrativeSignificance||'',notes:pr.notes||pr.writerNotes||'',x:node.positionX??100,y:node.positionY??100,properties:pr,raw:pr,rawNode};
 if(!storyMap)return base;
 return {...base,entityId,role:pr.role||'',age:pr.age||'',locationType:pr.locationType||'',eventType:pr.eventType||'',presentationOrder:pr.presentationOrder,chronologicalOrder:pr.chronologicalOrder};
}
// Old SANA creativity projects stored their actual characters and relations in
// ProjectOutput(type=CREATIVITY). New diagram APIs deliberately omit that legacy
// output from /diagram/get/project, which made populated projects look empty.
const LEGACY_CREATIVE_TYPES=['CHARACTER','WORLD','LOCATION','WORLD_RULE','STORY_EVENT'];
const LEGACY_EDGE_TYPES=['CUSTOM','FAMILY','FRIENDSHIP','RIVALRY','WORK','ROMANTIC','PARTICIPATES_IN','CAUSES','AFFECTED_BY','LIVES_IN','BELONGS_TO','VISITS','LEADS_TO','PRECEDES','SIMULTANEOUS_WITH','NARRATIVE_LINK','INSIDE','CONNECTED_TO','NEAR','APPLIES_TO','RESTRICTS','AFFECTS'];
function parseNodeData(raw){try{return JSON.parse(raw||'{}')||{}}catch{return {}}}
function legacyNodeType(type){const s=String(type||'').toUpperCase().replace(/^SANA_/,'');return LEGACY_CREATIVE_TYPES.includes(s)?toUiType(s):null}
async function legacyCreativityGraph(output,searchResults=null){
 const nodes=Array.isArray(output.nodes)?output.nodes:[];
 const edges=Array.isArray(output.edges)?output.edges:[];
 const found=searchResults??await api(`/api/v1/creativity/search/${output.id}?type=ALL`);
 const entities=Array.isArray(found)?found:[];
 const nodeMap=new Map(nodes.map(n=>[n.id,n]));
 const elements=entities.map(e=>{
   const node=nodeMap.get(e.nodeId)||{id:e.nodeId,type:e.type,positionX:e.positionX,positionY:e.positionY};
   const data=parseNodeData(node.dataJson);
   return {id:e.nodeId,nodeId:e.nodeId,entityId:e.id,outputId:output.id,
      type:toUiType(e.type),name:e.title||data.name||e.type,
      description:e.description||data.description||'',notes:data.notes||'',
      x:e.positionX??node.positionX??100,y:e.positionY??node.positionY??100,
      raw:data,rawNode:node,legacyCreative:true};
 });
 const seen=new Set(elements.map(e=>e.nodeId));
 // Show unmatched generic nodes rather than silently lose any legacy data.
 for(const node of nodes){
   if(seen.has(node.id))continue;
   const type=legacyNodeType(node.type);
   if(!type)continue;
   const data=parseNodeData(node.dataJson);
   elements.push({id:node.id,nodeId:node.id,outputId:output.id,type,
     name:data.name||data.title||node.type,description:data.description||'',notes:data.notes||'',
     x:node.positionX??100,y:node.positionY??100,raw:data,rawNode:node,
     legacyCreative:true,entityId:null});
 }
 const idSet=new Set(elements.map(e=>e.nodeId));
 const relations=edges.filter(e=>e.type!=='BRANCH_CHOICE').map(e=>({
   id:e.id,source:e.sourceNode?.id??e.sourceNodeId,
   target:e.targetNode?.id??e.targetNodeId,type:e.type,label:e.label,outputId:output.id
 })).filter(e=>idSet.has(e.source)&&idSet.has(e.target));
 return {elements,relations};
}
// Both legacy and modern outputs appear in the selector. Existing project data
// is never copied, reset, deleted, or moved when the user opens a project.
async function getApiProject(id){
 const p=await api(`/api/v1/project/get/${id}`);
 const domain=toUiDomain(p);
 const creativity=domain==='games'||domain==='writing';
 const types=await diagramTypes();
 let diagrams=await listDiagrams(id);
 let oldOutputs=[];
 const legacySearchCache=new Map();
 if(creativity){
   oldOutputs=await api(`/api/v1/project-output/get/project/${id}`);
   if(!Array.isArray(oldOutputs))oldOutputs=[];
   const legacy=oldOutputs.filter(o=>o.outputType==='CREATIVITY'&&o.representationType==='GRAPH');
   for(const o of legacy){
     if(!diagrams.some(d=>d.id===o.id))diagrams.push({id:o.id,title:o.title||'Creative workspace',outputType:'CREATIVITY',legacyCreative:true});
     // A backend may omit lazy-loaded ProjectOutput.nodes during JSON serialization.
     // Search still returns the persisted creative entities, so use both signals.
     const search=await api(`/api/v1/creativity/search/${o.id}?type=ALL`);
     legacySearchCache.set(o.id,Array.isArray(search)?search:[]);
   }
 }
 const remembered=storage.get(activeDiagramKey(id));
 const oldWithNodes=oldOutputs.find(o=>o.outputType==='CREATIVITY'&&o.representationType==='GRAPH'&&((Array.isArray(o.nodes)&&o.nodes.length>0)||(legacySearchCache.get(o.id)||[]).length>0));
 // The branching editor has its own selected-output key. Never use an older
 // BRANCHING_STORY preference as the project's character/story canvas.
 // Otherwise STORY_MAP characters appear to vanish after opening an AI tree.
 const canvasDiagrams=diagrams.filter(d=>d.outputType!=='BRANCHING_STORY');
 let active=canvasDiagrams.find(d=>String(d.id)===remembered)||
   (oldWithNodes&&canvasDiagrams.find(d=>d.id===oldWithNodes.id))||
   canvasDiagrams.find(d=>d.outputType===PRIMARY_DIAGRAM[domain])||
   canvasDiagrams[0]||diagrams[0];
 if(!active){
   // Never write to the database on opening a project. Creating diagrams is an
   // explicit action via the New Diagram control.
   return {...p,backendDomain:p.domain,domain,outputs:oldOutputs.length?oldOutputs:p.outputs||[],
     diagrams,activeOutputId:null,activeType:null,graphRevision:null,storyMap:false,
     legacyCreative:false,nodeTypes:typesForDomain(domain),edgeTypes:[],
     diagramTypes:types.filter(t=>t.domain===backendDomain(domain)&&t.diagramType!=='BRANCHING_STORY').map(t=>t.diagramType),
     elements:[],relations:[]};
 }
 if(active.legacyCreative||active.outputType==='CREATIVITY'){
   const legacy=oldOutputs.find(o=>o.id===active.id);
   if(!legacy)throw new Error('Legacy creativity output was not returned by the backend');
   const {elements,relations}=await legacyCreativityGraph(legacy,legacySearchCache.get(legacy.id));
   return {...p,backendDomain:p.domain,domain,outputs:oldOutputs,diagrams,
     activeOutputId:active.id,activeType:'CREATIVITY',graphRevision:legacy.graphRevision??0,
     storyMap:true,legacyCreative:true,
     nodeTypes:LEGACY_CREATIVE_TYPES.map(toUiType),edgeTypes:LEGACY_EDGE_TYPES,
     diagramTypes:types.filter(t=>t.domain===backendDomain(domain)&&t.diagramType!=='BRANCHING_STORY').map(t=>t.diagramType),
     elements,relations};
 }
 const rules=types.find(t=>t.diagramType===active.outputType)||{nodeTypes:[],edgeTypes:[]};
 const {graphRevision,graph}=await api(`/api/v1/diagram/graph/get/${active.id}`);
 const storyMap=active.outputType==='STORY_MAP';
 const entityIds=new Map();
 if(storyMap){
   const found=await api(`/api/v1/creativity/search/${active.id}?type=ALL`);
   for(const r of (Array.isArray(found)?found:[]))entityIds.set(r.nodeId,r.id);
 }
 const rawNodes=new Map((oldOutputs.length?oldOutputs:p.outputs||[]).flatMap(o=>o.nodes||[]).map(n=>[n.id,n]));
 const elements=(graph.nodes||[]).map(n=>toElement(n,active.id,storyMap,entityIds.get(n.id),rawNodes.get(n.id)));
 const idByKey=new Map((graph.nodes||[]).map(n=>[n.key,n.id]));
 const relations=(graph.edges||[]).map(e=>({id:e.id,source:idByKey.get(e.sourceKey),target:idByKey.get(e.targetKey),type:e.type,label:e.label,outputId:active.id,properties:e.properties}));
 return {...p,backendDomain:p.domain,domain,outputs:oldOutputs.length?oldOutputs:p.outputs||[],diagrams,
   activeOutputId:active.id,activeType:active.outputType,graphRevision,graphProperties:graph.properties||{},storyMap,
   legacyCreative:false,nodeTypes:rules.nodeTypes.map(toUiType),edgeTypes:rules.edgeTypes,
   diagramTypes:types.filter(t=>t.domain===backendDomain(domain)&&t.diagramType!=='BRANCHING_STORY').map(t=>t.diagramType),
   elements,relations};
}
// ID of the project's diagram of a type, created when missing (branching tab, character balance)
export async function diagramIdFor(project,diagramType,create=false){
 if(!API_MODE)return 'demo';
 const known=diagramType==='BRANCHING_STORY'
  ?selectedBranchingDiagram(project)
  :(project.diagrams||[]).find(d=>d.outputType===diagramType);
 if(known)return known.id;
 const available=(await listDiagrams(project.id)).filter(d=>d.outputType===diagramType);
 const chosen=diagramType==='BRANCHING_STORY'
  ?available.find(d=>String(d.id)===storage.get(branchingDiagramKey(project.id)))||available.sort((a,b)=>Number(b.id)-Number(a.id))[0]
  :available[0];
 if(!chosen&&!create){const error=new Error('No diagram exists for this tool. Create one explicitly.');error.code='DIAGRAM_REQUIRED';throw error;}
 const diagram=chosen||await createDiagram(project.id,project.name,diagramType);
 if(!(project.diagrams||[]).some(d=>d.id===diagram.id))project.diagrams=[...(project.diagrams||[]),diagram];
 if(diagramType==='BRANCHING_STORY')rememberBranchingOutput(project.id,diagram.id);
 return diagram.id;
}

// ---------- API writes ----------
const revision=p=>`?expectedRevision=${p.graphRevision}`;
async function writeApiNode(projectId,type,values,oldId){
 const p=await getApiProject(projectId),existing=oldId?p.elements.find(e=>String(e.id)===String(oldId)):null;
 const outputId=p.activeOutputId;
 if(outputId==null)throw new Error("Create or select a diagram before adding elements");
 const x=Number(values.x??existing?.x??140),y=Number(values.y??existing?.y??135);
 if(p.storyMap){
   if(!CREATIVE_TYPES.includes(type))throw new Error('Story maps hold characters, worlds, locations, rules and events only');
   const detailPath=type==='characters'?`/api/v1/character-profile/get/by/${existing?.entityId}`:
     `/api/v1/${entityPaths[type]}/get/${existing?.entityId}`;
   const previous=existing?.entityId?await api(detailPath):{};
   const payload={positionX:x,positionY:y,...(type==='events'?{title:values.name}:{name:values.name}),
     description:values.description||'—',notes:values.notes||''};
   // Keep advanced text fields already stored by the backend when editing.
   for(const field of ['aliases','appearance','traits','strengths','weaknesses','skills','history','goals','fears','innerConflict','secrets','characterValues','characterArc','lore','atmosphere','scope','conditions','exceptions','consequences','inWorldTime'])if(values.properties?.[field]!=null)payload[field]=values.properties[field];else if(previous?.[field]!=null)payload[field]=previous[field];
   if(type==='characters'){payload.shortDescription=values.description||'—';payload.role=values.role||previous.role||'OTHER';payload.age=Number(values.age)||Number(previous.age)||18;payload.writerNotes=values.notes||previous.writerNotes||'';}
   if(type==='locations'){payload.locationType=values.locationType||'';payload.narrativeSignificance=values.description||'';}
   if(type==='events'){payload.eventType=values.eventType||'';payload.presentationOrder=Number(values.presentationOrder)||1;payload.chronologicalOrder=values.chronologicalOrder?Number(values.chronologicalOrder):null;}
   await api(`/api/v1/${entityPaths[type]}/${existing?`update/${existing.entityId}`:`add/${outputId}`}`,json(existing?'PUT':'POST',payload));
   if(type==='locations'&&existing?.entityId){for(const [kind,target]of Object.entries(values.locationLinks||{}))if(target)await api(`/api/v1/location/update/${existing.entityId}/${kind}/${target}`,{method:'PUT'})}
 }else{
   // Keep properties this form does not show (for example ERD attributes or AI-added fields)
   const data={...(existing?.properties||{}),...(values.properties||{})};delete data.label;
   data.description=values.description||'';if(values.notes)data.notes=values.notes;else delete data.notes;
   const backendType=toBackendType(type);
   if(backendType==='ENTITY'&&(!Array.isArray(data.attributes)||!data.attributes.some(a=>a.primaryKey)))throw Error('ERD entities require attributes and a primary key.');
   const body={type:backendType,label:values.name,positionX:x,positionY:y,data};
   await api(existing?`/api/v1/diagram/node/update/${outputId}/${existing.nodeId}${revision(p)}`:`/api/v1/diagram/node/add/${outputId}${revision(p)}`,json(existing?'PUT':'POST',body));
 }
 return getApiProject(projectId);
}

// ---------- AI (n8n through the backend) ----------
// "Start with SANA AI": the backend asks n8n which domain and diagram fit an idea; nothing is created yet.
const QUICK_START_DOMAIN={TECHNOLOGY:'technology',BUSINESS:'business',EDUCATION:'education',WRITING:'writing',GAMES:'games'};
export const quickStartApi={
 async classify(idea,lang){
  const suggestion=await api('/api/v1/ai/quick-start/classify',json('POST',{idea,language:lang==='en'?'en':'ar'}));
  return {...suggestion,domain:QUICK_START_DOMAIN[suggestion.category]||'writing'};
 },
 // Diagram types the user may switch to in each domain (branching stories have their own Studio tab)
 async diagramOptions(){
  const types=await diagramTypes(),options={};
  for(const domain of Object.values(QUICK_START_DOMAIN))options[domain]=types.filter(t=>t.domain===backendDomain(domain)&&t.diagramType!=='BRANCHING_STORY').map(t=>t.diagramType);
  return options;
 },
};
// Business tools (structured reports) are executed by the backend's Business tool endpoint.
export const businessApi={
 execute:body=>api('/api/business/tools',json('POST',body)),
};
export const aiApi={
 history:projectId=>api(`/api/projects/${projectId}/ai/requests`),
 // The same requests with their prompts and answers, to rebuild the AI panel's chat
 chat:projectId=>api(`/api/projects/${projectId}/ai/requests?messages=true`),
 start:(projectId,body,domain)=>api(
  ['writing','games'].includes(domain)?`/api/projects/${projectId}/creativity/ai/requests`:`/api/projects/${projectId}/ai/requests`,
  json('POST',body)),
 status:statusUrl=>{if(!/^\/api\/projects\/\d+\/(?:creativity\/)?ai\/requests\/[A-Za-z0-9-]+$/.test(statusUrl))throw Error('Invalid AI status address');return api(statusUrl)},
 proposal:(projectId,proposalId)=>api(`/api/projects/${projectId}/ai/proposals/${proposalId}`),
 accept:(projectId,proposalId)=>api(`/api/projects/${projectId}/ai/proposals/${proposalId}/accept`,{method:'POST'}),
 reject:(projectId,proposalId)=>api(`/api/projects/${projectId}/ai/proposals/${proposalId}/reject`,{method:'POST'}),
};

export const repository={
 // Resolve a tool's own diagram without changing the user's open diagram; create it when missing.
 ensureDiagram:(project,diagramType)=>diagramIdFor(project,diagramType,true),
 async linkLocation(id,kind,target){return api(`/api/v1/location/update/${id}/${kind}/${target}`,{method:'PUT'})},
 async renameDiagram(projectId,title){const p=await getApiProject(projectId);await api(`/api/v1/diagram/update/${p.activeOutputId}${revision(p)}`,json('PUT',{title,diagramType:p.activeType}));return getApiProject(projectId)},
 async deleteDiagram(projectId){const p=await getApiProject(projectId);await api(`/api/v1/diagram/delete/${p.activeOutputId}${revision(p)}`,{method:'DELETE'});return getApiProject(projectId)},
 async deleteImpact(projectId,element){const p=await getApiProject(projectId);if(!API_MODE||!p.storyMap||p.legacyCreative)return null;return api(`/api/v1/creativity/relations/delete/preview/${p.activeOutputId}/node/${element.nodeId}`)},
 async list(){return API_MODE?(await getApiProjects()).map(projectWithCounts):load().map(projectWithCounts)},
 async get(id){return API_MODE?getApiProject(id):load().find(p=>String(p.id)===String(id))||null},
 async createProject(payload){
  if(API_MODE){
   const before=await getApiProjects();const domain=payload.domain||'writing';
   const me=await authApi.me();
   await api(`/api/v1/project/add/${me.id}`,json('POST',{name:payload.name,description:payload.description||'',domain:backendDomain(domain),status:'DRAFT'}));
   const after=await getApiProjects();const ids=new Set(before.map(x=>x.id));const p=after.find(x=>!ids.has(x.id));
   if(!p)throw new Error('Project created but could not resolve its ID; refresh projects before retrying');
   storage.set(uiDomainKey(p.id),domain);
   if(payload.aiFirst)return {...p,domain};
   const diagram=await createDiagram(p.id,payload.name,PRIMARY_DIAGRAM[domain]);storage.set(activeDiagramKey(p.id),diagram.id);
   const newNodeIds=[];
   for(const node of starterNodes(payload.templateId,payload.lang)){
    // Identify the newly created node by comparing actual server IDs, not names.
    const before=await getApiProject(p.id),ids=new Set(before.elements.map(e=>String(e.id)));
    const afterNode=await writeApiNode(p.id,node.type,node);
    const added=afterNode.elements.find(e=>!ids.has(String(e.id)));
    if(!added)throw Error('Template node created, but its ID could not be found. Reload before retrying.');
    newNodeIds.push(added.id);
   }
   for(const link of starterConnections(payload.templateId,payload.lang)){
    await repository.createRelation(p.id,{source:newNodeIds[link.source],target:newNodeIds[link.target],type:link.type,label:link.label});
   }
   return {...p,domain};
  }
  const elements=starterNodes(payload.templateId,payload.lang).map(n=>({...n,id:makeId()}));
  const relations=starterConnections(payload.templateId,payload.lang).map(link=>({id:makeId(),source:elements[link.source].id,target:elements[link.target].id,type:link.type,label:link.label}));
  const projects=load(),p={id:makeId(),name:payload.name,description:payload.description||'',domain:payload.domain||'writing',templateId:payload.templateId||null,status:'DRAFT',createdAt:now(),updatedAt:now(),elements,relations};persist([p,...projects]);return p;
 },
 async updateProject(id,patch){
  // The domain cannot change after creation, so the stored backend domain is sent back as it is
  if(API_MODE){const old=await api(`/api/v1/project/get/${id}`);await api(`/api/v1/project/update/${id}`,json('PUT',{name:patch.name??old.name,description:patch.description??old.description,domain:old.domain,status:patch.status??old.status,version:old.version}));return {...old,...patch,domain:toUiDomain(old)};}
  const all=load().map(p=>String(p.id)===String(id)?{...p,...patch,updatedAt:now()}:p);persist(all);return all.find(p=>String(p.id)===String(id));
 },
 async removeProject(id){if(API_MODE)return api(`/api/v1/project/delete/${id}`,{method:'DELETE'});persist(load().filter(p=>String(p.id)!==String(id)));},
 async saveElement(projectId,type,values,oldId){
  if(API_MODE)return writeApiNode(projectId,type,values,oldId);
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));if(!p)throw new Error('Project not found');
  if(oldId){p.elements=p.elements.map(e=>String(e.id)===String(oldId)?{...e,...values,type}:e)}
  else p.elements.push({id:makeId(),type,...values,x:values.x??120+(p.elements.length%3)*260,y:values.y??130+Math.floor(p.elements.length/3)*165});
  p.updatedAt=now();persist(all);return p;
 },
 async removeElement(projectId,element){
  if(API_MODE){
   const p=await getApiProject(projectId);
   if(p.storyMap)await api(`/api/v1/${entityPaths[element.type]}/delete/${element.entityId}`,{method:'DELETE'});
   else await api(`/api/v1/diagram/node/delete/${p.activeOutputId}/${element.nodeId}${revision(p)}`,{method:'DELETE'});
   return getApiProject(projectId);
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));p.elements=p.elements.filter(e=>String(e.id)!==String(element.id));p.relations=p.relations.filter(r=>r.source!==element.id&&r.target!==element.id);p.updatedAt=now();persist(all);return p;
 },
 async moveNode(projectId,id,x,y){
  if(API_MODE){
   const p=await getApiProject(projectId),e=p.elements.find(e=>String(e.id)===String(id));if(!e)throw new Error('Node not found');
   const px=Math.round(x),py=Math.round(y);
   // Story map nodes are plain ProjectNodes (the entity row holds the fields); other diagrams use the revision-checked endpoint
   if(p.storyMap){if(!e.rawNode)throw new Error('Node coordinates unavailable');await api(`/api/v1/node/update/${e.nodeId}`,json('PUT',{type:e.rawNode.type,dataJson:e.rawNode.dataJson||null,positionX:px,positionY:py}));return;}
   const data={...e.properties};delete data.label;
   await api(`/api/v1/diagram/node/update/${p.activeOutputId}/${e.nodeId}${revision(p)}`,json('PUT',{type:toBackendType(e.type),label:e.name,positionX:px,positionY:py,data}));return;
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));const e=p.elements.find(e=>String(e.id)===String(id));if(e){e.x=x;e.y=y;persist(all)}
 },
 async allowedRelationTypes(projectId,sourceId,targetId,editingId){
  if(!API_MODE)return ['CUSTOM','FAMILY','FRIENDSHIP','RIVALRY','WORK','ROMANTIC','PARTICIPATES_IN','CAUSES','AFFECTED_BY','LIVES_IN','BELONGS_TO','VISITS','LEADS_TO','PRECEDES','SIMULTANEOUS_WITH','NARRATIVE_LINK','INSIDE','CONNECTED_TO','NEAR','APPLIES_TO','RESTRICTS','AFFECTS'];
  const p=await getApiProject(projectId),a=p.elements.find(e=>String(e.id)===String(sourceId)),b=p.elements.find(e=>String(e.id)===String(targetId));
  if(!a||!b)return p.edgeTypes;
  if(!p.storyMap)return p.edgeTypes.filter(type=>{
 const source=a.type.toUpperCase(),target=b.type.toUpperCase();
 if(p.activeType==='ARCHITECTURE'&&type==='DATA_FLOW')return [source,target].some(t=>['DATABASE','QUEUE'].includes(t));
 if(p.activeType==='USE_CASE'){if(type==='ASSOCIATION')return source==='ACTOR'&&target==='USE_CASE';if(['INCLUDE','EXTEND'].includes(type))return source==='USE_CASE'&&target==='USE_CASE';if(type==='GENERALIZATION')return source===target;}
 if(p.activeType==='CLASS_DIAGRAM'){if(type==='INHERITANCE'){const compatible=source==='INTERFACE'?target==='INTERFACE':['CLASS','ABSTRACT_CLASS'].includes(source)&&['CLASS','ABSTRACT_CLASS'].includes(target);const seen=new Set();const reaches=id=>{if(String(id)===String(sourceId))return true;if(seen.has(String(id)))return false;seen.add(String(id));return p.relations.filter(r=>r.type==='INHERITANCE'&&String(r.id)!==String(editingId)&&String(r.source)===String(id)).some(r=>reaches(r.target))};return compatible&&!reaches(targetId)}if(type==='IMPLEMENTATION')return source!=='INTERFACE'&&target==='INTERFACE';}
 if(p.activeType==='MIND_MAP'&&type==='BRANCH')return target!=='CENTRAL_TOPIC'&&!p.relations.some(r=>r.type==='BRANCH'&&String(r.target)===String(targetId)&&String(r.id)!==String(editingId));
 return true;
 });
  const types=await api(`/api/v1/creativity/relations/get/types/${p.activeOutputId}/${a.nodeId}/${b.nodeId}`);
  return Array.isArray(types)?types.map(t=>typeof t==='string'?t:t.name||String(t)):['CUSTOM'];
 },
 async createRelation(projectId,data){
  if(API_MODE){
   const p=await getApiProject(projectId),a=p.elements.find(e=>String(e.id)===String(data.source)),b=p.elements.find(e=>String(e.id)===String(data.target));
   if(!a||!b)throw new Error('Both nodes must be on the open diagram');
   const type=data.type||p.edgeTypes[0];
   if(p.storyMap){const label=data.label||(type==='CUSTOM'?'Related':type.replaceAll('_',' '));await api(`/api/v1/creativity/relations/add/${p.activeOutputId}/${a.nodeId}/${b.nodeId}`,json('POST',{type,label}));}
   else await api(`/api/v1/diagram/edge/add/${p.activeOutputId}${revision(p)}`,json('POST',{type,label:String(data.label||'').trim()||type.replaceAll('_',' '),sourceNodeId:a.nodeId,targetNodeId:b.nodeId,data:data.properties||{}}));
   return getApiProject(projectId);
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));if(!p)throw new Error('Project not found');p.relations.push({id:makeId(),source:Number(data.source),target:Number(data.target),type:data.type||'CUSTOM',label:data.label||'Related'});p.updatedAt=now();persist(all);return p;
 },
 async updateRelation(projectId,data){if(!API_MODE){const all=load(),p=all.find(p=>String(p.id)===String(projectId));const edge=p.relations.find(e=>String(e.id)===String(data.id));Object.assign(edge,data);persist(all);return p;}const p=await getApiProject(projectId);if(p.storyMap)throw Error('Story relationship updates are not supported by this editor.');const a=p.elements.find(e=>String(e.id)===String(data.source)),b=p.elements.find(e=>String(e.id)===String(data.target));if(!a||!b)throw Error('Both nodes must be on the open diagram');await api(`/api/v1/diagram/edge/update/${p.activeOutputId}/${data.id}${revision(p)}`,json('PUT',{type:data.type,label:String(data.label||'').trim()||data.type.replaceAll('_',' '),sourceNodeId:a.nodeId,targetNodeId:b.nodeId,data:data.properties||{}}));return getApiProject(projectId)},
 async removeRelation(projectId,r){
  if(API_MODE){
   const p=await getApiProject(projectId);
   if(p.storyMap)await api(`/api/v1/edge/delete/${r.id}`,{method:'DELETE'});
   else await api(`/api/v1/diagram/edge/delete/${p.activeOutputId}/${r.id}${revision(p)}`,{method:'DELETE'});
   return getApiProject(projectId);
  }
  const all=load(),p=all.find(p=>String(p.id)===String(projectId));p.relations=p.relations.filter(x=>String(x.id)!==String(r.id));p.updatedAt=now();persist(all);return p;
 },
 // Show another diagram of the project on the map
 async setActiveDiagram(projectId,outputId){storage.set(activeDiagramKey(projectId),outputId);return getApiProject(projectId)},
 // Add a new empty diagram to the project and show it
 async addDiagram(projectId,title,diagramType){const d=await createDiagram(projectId,title,diagramType);storage.set(activeDiagramKey(projectId),d.id);return getApiProject(projectId)},
 async importProject(data){if(API_MODE)throw new Error('Import available in demo only');if(!data||typeof data.name!=='string'||!Array.isArray(data.elements)||!Array.isArray(data.relations))throw new Error('Invalid JSON');const p=clone(data),ids=new Map();p.id=makeId();p.elements=p.elements.map(e=>{const id=makeId();ids.set(e.id,id);return {...e,id}});p.relations=p.relations.filter(r=>ids.has(r.source)&&ids.has(r.target)).map(r=>({...r,id:makeId(),source:ids.get(r.source),target:ids.get(r.target)}));p.updatedAt=now();persist([p,...load()]);return p}
};
