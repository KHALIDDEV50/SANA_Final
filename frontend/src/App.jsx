import React, {useCallback,useEffect,useMemo,useRef,useState} from 'react';
import {createRoot} from 'react-dom/client';
import {
  ArrowLeft,ArrowRight,ArrowUpLeft,ArrowUpRight,BookOpen,Bookmark,BriefcaseBusiness,Code2,Database,Server,Target,Wallet,Megaphone,Boxes,GripVertical,LayoutTemplate,Check,CheckCheck,ChevronDown,ChevronLeft,ChevronRight, CircleHelp,
  Clock3,CloudOff,Compass,Download,Edit3,ExternalLink,FileText,FolderClosed,FolderOpen,Gamepad2,Globe2,Heart,Home,Layers3,
  LayoutDashboard,Lightbulb,Link2,ListFilter,LockKeyhole,MapPin,Menu,MessageCircle,Minus,MoreHorizontal,MousePointer2,Network,
  Plus,Search,Settings2,ShieldCheck, Sparkles,Sun,Trash2,Upload,Users, WandSparkles, Workflow, X, ZoomIn,ZoomOut,CheckCircle2,
  PenLine,Eye,History, PanelRightClose, CircleDot, RotateCcw, Copy, Info, BookMarked, Image as ImageIcon, GitBranch, GraduationCap, Send
} from 'lucide-react';
import {API_MODE,repository,TYPES,elementTypes,aiApi,authApi,quickStartApi,selectedBranchingDiagram,rememberBranchingOutput} from './lib/data';
import {TEMPLATE_CATALOG,typesForDomain,domainGroup,findTemplate} from './lib/templates';
import {tr} from './lib/i18n';
import {Moodboard,BranchingNarrative,GameBalance} from './components/CreativeExtras';
import {routeCreativityChat} from './lib/creativityChatRouting';
import {moodboardAi,creativeTools as creativeStore} from './lib/creativeTools';
import MoodboardChatPreview from './components/creativity/MoodboardChatPreview';
import './styles.css';
import './theme-v3.css';
import './theme-v4.css';
import './creative-studio.css';
import './home-landing.css';
import AccountExperience from './components/AccountExperience';
import StudioWorkspace,{useDialogFocus} from './components/StudioWorkspace';
import {DiagramNodeContent,DiagramPropertiesEditor,graphPresentation,organizedGraph,edgeEndpoints,techEdgeEndpoints,techNodeBounds} from './components/DiagramPresentation';
import DomainPanels,{ProjectSummary,BookInspiration} from './components/DomainPanels';
import AdminStudio from './components/AdminStudio';
import BillingExperience from './components/BillingExperience';
import {billingApi,billingEnabled,membershipLimit} from './lib/billing';
import SpecializedDiagramBoard,{SPECIAL_BOARD_TYPES} from './components/SpecializedDiagramBoard';

const ICONS={characters:Users,worlds:Globe2,locations:MapPin,rules:ShieldCheck,events:Workflow,client:Users,application:LayoutDashboard,service:Server,database:Database,queue:Layers3,component:Boxes,external_system:Globe2,entity:Database,class:Code2,abstract_class:Code2,interface:Code2,enum:ListFilter,actor:Users,use_case:CircleDot,customer_segment:Users,value_proposition:Target,channels:Megaphone,customer_relationships:Heart,revenue_streams:Wallet,key_resources:Boxes,key_activities:CheckCircle2,key_partnerships:Link2,cost_structure:Wallet,central_topic:Lightbulb,topic:CircleDot,subtopic:CircleDot,note:FileText};
const COLORS={characters:'sage',worlds:'sky',locations:'amber',rules:'violet',events:'coral',client:'sage',application:'violet',service:'sage',database:'sky',queue:'amber',component:'violet',external_system:'coral',entity:'sky',class:'sage',abstract_class:'violet',interface:'amber',enum:'coral',actor:'sage',use_case:'sky',customer_segment:'sage',value_proposition:'violet',channels:'sky',customer_relationships:'coral',revenue_streams:'amber',key_resources:'sky',key_activities:'sage',key_partnerships:'violet',cost_structure:'coral',central_topic:'amber',topic:'sage',subtopic:'sky',note:'violet'};
const publicError=(value,lang='ar')=>{const message=String(value||'');return /n8n|spring boot|webhook|callback|org\.springframework|localhost|stacktrace|exception in thread|HTTP 5\d\d|https?:\/\/[^ ]+\/api\//i.test(message)?(lang==='ar'?'تعذر إكمال الطلب الآن. حاول مرة أخرى.':'The request could not be completed. Please try again.'):message};
const humanize=value=>/^[a-z]+(_[a-z]+)*$/.test(value)?value.charAt(0).toUpperCase()+value.slice(1).replaceAll('_',' '):value;
const TECH_DIAGRAM_LABELS={ARCHITECTURE:'System Architecture',CLASS_DIAGRAM:'UML Class Diagram',ERD:'Database / ERD',USE_CASE:'Use Case Diagram'};
const TECH_NODE_LABELS={client:'Client / UI',application:'Application',service:'Service / API',database:'Database',queue:'Message Queue',component:'Component',external_system:'External System',entity:'Table',class:'Class',abstract_class:'Abstract Class',interface:'Interface',enum:'Enum',actor:'Actor',use_case:'Use Case'};
// Optional per-character palette, stored on this device until a server-side color field is available.
const CHARACTER_PALETTE=[
 {name:'Lavender',color:'#9875B5',tint:'#F4ECFA'},
 {name:'Sage',color:'#568F80',tint:'#ECF6F1'},
 {name:'Sky',color:'#548CA6',tint:'#EAF5F9'},
 {name:'Gold',color:'#B88C3B',tint:'#FFF5DF'},
 {name:'Coral',color:'#BF796D',tint:'#FFF0EB'},
 {name:'Rose',color:'#B57598',tint:'#FBEFFA'},
 {name:'Indigo',color:'#6F7EB7',tint:'#EDF0FC'}
];
const characterColorKey=(projectId,nodeId)=>`sana-character-color:${projectId}:${nodeId}`;
function readCharacterColor(projectId,nodeId){try{return CHARACTER_PALETTE.find(p=>p.color===localStorage.getItem(characterColorKey(projectId,nodeId)))?.color||null}catch{return null}}
function saveCharacterColor(projectId,nodeId,color){try{if(color)localStorage.setItem(characterColorKey(projectId,nodeId),color);else localStorage.removeItem(characterColorKey(projectId,nodeId))}catch{}}
const characterColorStyle=(projectId,node)=>{if(node.type!=='characters')return undefined;const color=readCharacterColor(projectId,node.id);const choice=CHARACTER_PALETTE.find(c=>c.color===color);return choice?{'--sana-character-accent':choice.color,'--sana-character-tint':choice.tint,'--sana-story-accent':choice.color}:undefined};
const REL_TYPES=['CUSTOM','FRIENDSHIP','FAMILY','RIVALRY','PARTICIPATES_IN','VISITS','LIVES_IN','LEADS_TO','PRECEDES','CONNECTED_TO','APPLIES_TO','AFFECTS'];
function parseRoute(){const h=window.location.hash.replace(/^#\/?/,'').split('/').filter(Boolean);if(h[0]==='studio')return {page:'studio',id:h[1],tab:h[2]||'map'};if(h[0]==='billing-result')return {page:'billing-result',resultStatus:['success','pending','failed','cancelled'].includes(h[1])?h[1]:'pending'};return {page:h[0]||'home',domain:h[0]==='library'?h[1]:undefined};}
const dateLabel=(d,lang)=>{try{return new Date(d).toLocaleDateString(lang==='ar'?'ar-SA':'en-GB',{month:'short',day:'numeric'})}catch{return '—'}};
function Logo({small=false,onClick}){return <button className={`logo-lockup logo-symbol-only sana-animated-mark ${small?'logo-small':''}`} onClick={onClick} aria-label="SANA home"><span className="sana-mark-light" aria-hidden="true"/><img src="/sana-logo.webp" alt="شعار سنا · SANA" /><span className="sana-mark-flare" aria-hidden="true">✦</span></button>}
const AMBIENT_SPARKS=[[5,13,9,0],[13,34,7,1.4],[91,14,11,2.1],[86,39,7,3.2],[9,77,10,1.3],[95,81,8,2.7],[23,7,6,4.3],[77,8,7,1.8],[22,91,8,3.7],[73,91,9,0.4],[47,12,6,2.8],[55,85,6,1.1],[2,54,6,1.9],[98,54,8,4.1],[37,4,7,3.1],[62,96,6,2.2]];
function AmbientStars(){return <div className="sana-ambient-stars" aria-hidden="true">{AMBIENT_SPARKS.map(([x,y,size,delay],i)=><span key={i} style={{left:`${x}%`,top:`${y}%`,fontSize:`${size}px`,animationDelay:`-${delay}s`}}>✦</span>)}</div>}
function IconButton({children,onClick,label, className='',disabled=false}){return <button type="button" className={`icon-btn ${className}`} onClick={onClick} aria-label={label||'Action'} title={label} disabled={disabled}>{children}</button>}
function Empty({icon:Icon=Sparkles,title,description,action}){return <div className="empty-state"><span className="empty-icon"><Icon size={25}/></span><h3>{title}</h3><p>{description}</p>{action}</div>}
function SectionTitle({eyebrow,title,description,action}){return <div className="section-heading"><div>{eyebrow&&<span className="eyebrow"><span className="eyebrow-dot"/>{eyebrow}</span>}<h2>{title}</h2>{description&&<p>{description}</p>}</div>{action}</div>}
// Do not let a failed tool view replace the whole workspace with a blank screen.
class StudioToolBoundary extends React.Component {
 constructor(props){super(props);this.state={error:false}}
 static getDerivedStateFromError(){return {error:true}}
 render(){if(this.state.error)return <section className="sana-tool-recovery" role="alert"><ShieldCheck size={22}/><div><strong>{this.props.lang==='ar'?'تعذّر عرض الأداة':'This tool could not be displayed'}</strong><p>{this.props.lang==='ar'?'بقية المشروع محفوظة. يمكنك إعادة محاولة فتح الأداة.':'Your project is still available. You can retry opening this tool.'}</p></div><button type="button" onClick={()=>this.setState({error:false})}>{this.props.lang==='ar'?'إعادة المحاولة':'Retry'}</button></section>;return this.props.children}
}
function Btn({children,onClick,variant='primary',className='',type='button',disabled=false,icon:Icon}){return <button type={type} disabled={disabled} onClick={onClick} className={`btn btn-${variant} ${className}`}>{Icon&&<Icon size={17}/>}<span>{children}</span></button>}
function DomainPill({domain,t}){const group=domainGroup(domain),Icon=group==='technology'?Code2:group==='business'?BriefcaseBusiness:group==='education'?GraduationCap:domain==='games'?Gamepad2:BookOpen;return <span className={`domain-pill domain-${group}`}><Icon size={13}/>{t(domain==='games'?'games':group==='creativity'?'creativity':group)}</span>}
function Cover({domain='writing',variant='card',children}){return <div className={`cover cover-${domain} cover-${variant}`}><span className="cover-orbit a"/><span className="cover-orbit b"/><span className="cover-crescent"/><span className="cover-glow"/>{domain==='games'?<Gamepad2 className="cover-symbol" size={43} strokeWidth={1.2}/>:domain==='technology'?<Code2 className="cover-symbol" size={43} strokeWidth={1.2}/>:domain==='business'?<BriefcaseBusiness className="cover-symbol" size={43} strokeWidth={1.2}/>:domain==='education'?<GraduationCap className="cover-symbol" size={43} strokeWidth={1.2}/>:<BookOpen className="cover-symbol" size={43} strokeWidth={1.2}/>}<span className="cover-star">✦</span>{children}</div>}
function ProjectCard({project,onClick,t,lang,actions}){return <article className="project-card" onClick={onClick} role="button" tabIndex={0} onKeyDown={e=>{if(e.key==='Enter')onClick()}}><Cover domain={project.domain}/><div className="project-card-body"><div className="card-topline"><DomainPill domain={project.domain} t={t}/><span className="card-date"><Clock3 size={12}/>{dateLabel(project.updatedAt,lang)}</span></div><h3>{project.name}</h3><p>{project.description||'—'}</p><div className="card-bottom"><span><Layers3 size={15}/>{project.elementCount??project.elements?.length??'—'} {t('elements')}</span><button className="card-open" type="button" onClick={e=>{e.stopPropagation();onClick()}} aria-label={t('openProject')}>{lang==='ar'?<ArrowLeft size={19}/>:<ArrowRight size={19}/>}</button></div></div></article>}
function Stat({icon:Icon,num,label,shade}){return <div className="stat-item"><span className={`stat-icon ${shade}`}><Icon size={20}/></span><div><strong>{num}</strong><small>{label}</small></div></div>}
function Toast({toast,onClose}){if(!toast)return null;return <div className={`toast ${toast.error?'toast-error':''}`} role="status"><span className="toast-mark">{toast.error?<Info size={17}/>:<CheckCircle2 size={17}/>}</span><span>{toast.message}</span><button onClick={onClose} aria-label="Close"><X size={16}/></button></div>}
function Modal({title,subtitle,onClose,children,wide=false,drawer=false}){const dialog=useRef(null);useDialogFocus(dialog,onClose);useEffect(()=>{const fn=e=>{if(e.key==='Escape'&&!e.defaultPrevented)onClose()};window.addEventListener('keydown',fn);return()=>window.removeEventListener('keydown',fn)},[onClose]);return <div className={`modal-backdrop ${drawer?'modal-as-drawer':''}`} onMouseDown={e=>{if(e.target===e.currentTarget)onClose()}}><section ref={dialog} className={`modal-card ${wide?'modal-wide':''}`} role="dialog" aria-modal="true" aria-label={title}><header className="modal-head"><div><h2>{title}</h2>{subtitle&&<p>{subtitle}</p>}</div><IconButton label="Close" onClick={onClose}><X size={20}/></IconButton></header>{children}</section></div>}
function FormField({label,children,hint}){return <label className="form-field"><span>{label}</span>{children}{hint&&<small>{hint}</small>}</label>}

function ProjectModal({t,lang,onClose,onSave,initial,busy}){
 const [name,setName]=useState(initial?.name||''),[description,setDesc]=useState(initial?.description||''),[domain,setDomain]=useState(initial?.domain||'writing');const editing=Boolean(initial?.id);
 return <Modal title={editing?t('updateProject'):t('newProject')} subtitle={t('ideaName')} onClose={onClose}><form className="modal-form" onSubmit={e=>{e.preventDefault();if(name.trim())onSave({name:name.trim(),description:description.trim(),domain,templateId:editing?null:initial?.templateId,aiFirst:!editing&&!!initial?.aiFirst,lang})}}><FormField label={`${t('projectName')} *`}><input required maxLength={100} autoFocus placeholder={lang==='ar'?'مثل: مدينة أركان':'e.g. The Lost City'} value={name} onChange={e=>setName(e.target.value)}/></FormField><FormField label={t('projectDescription')}><textarea rows={3} maxLength={5000} value={description} onChange={e=>setDesc(e.target.value)} placeholder={t('projectDescription')}/></FormField><div className="form-field"><span>{t('selectDomain')}</span><div className="choice-grid">{[{id:"writing",icon:BookOpen,desc:lang==="ar"?"قصص وشخصيات وعوالم":"Stories & characters"},{id:"games",icon:Gamepad2,desc:lang==="ar"?"حبكات ومغامرات":"Game narratives"},{id:"technology",icon:Code2,desc:lang==="ar"?"أنظمة و APIs":"Systems & APIs"},{id:"business",icon:BriefcaseBusiness,desc:lang==="ar"?"أعمال وخطط":"Business plans"},{id:"education",icon:GraduationCap,desc:lang==="ar"?"خرائط ذهنية وتعلّم":"Mind maps & learning"}].map(item=>{const Icon=item.icon;return <button key={item.id} type="button" disabled={editing} className={`choice-tile ${domain===item.id?"selected":""}`} onClick={()=>setDomain(item.id)}><Icon/><strong>{t(item.id)}</strong><small>{item.desc}</small></button>})}</div></div><div className="modal-actions"><Btn variant="ghost" onClick={onClose}>{t('cancel')}</Btn><Btn type="submit" icon={editing?Check:Plus} disabled={busy||!name.trim()}>{editing?t('save'):t('create')}</Btn></div></form>{editing&&<ProjectSummary key={initial.id} projectId={initial.id} lang={lang}/>}</Modal>
}
function ElementModal({t,lang,onClose,onSave,onDelete,onGameBalance,initial,initialType='characters',position,availableTypes=TYPES,busy,project}){
 const technical=project?.domain==='technology';
 const [characterColor,setCharacterColor]=useState(()=>initial?.id?readCharacterColor(project?.id,initial.id):'#9875B5');
 const [linkedPlayable,setLinkedPlayable]=useState(null);
 useEffect(()=>{if(!API_MODE||initial?.type!=='characters'||!initial?.entityId)return;let active=true;
  creativeStore.fighters(project).then(rows=>{if(active)setLinkedPlayable((Array.isArray(rows)?rows:[]).find(f=>String(f.characterProfileId)===String(initial.entityId))||false)}).catch(()=>{});
  return()=>{active=false};
 },[project?.id,initial?.entityId]);
 const editTitle=initial?t('editElement'):t('createElement');
 const [locationLinks,setLocationLinks]=useState({});
 const [properties,setProperties]=useState(()=>Object.fromEntries(Object.entries(initial?.properties||initial?.raw||{}).map(([k,v])=>[k,Array.isArray(v)?(['attributes','methods','values'].includes(k)?JSON.stringify(v,null,2):v.join(', ')):v])));
 const [type,setType]=useState(initial?.type||initialType),[name,setName]=useState(initial?.name||''),[description,setDesc]=useState(initial?.description||''),[role,setRole]=useState(initial?.role||''),[age,setAge]=useState(initial?.age||''),[locationType,setLocationType]=useState(initial?.locationType||''),[eventType,setEventType]=useState(initial?.eventType||''),[presentationOrder,setOrder]=useState(initial?.presentationOrder||1),[chronologicalOrder,setChrono]=useState(initial?.chronologicalOrder||''),[notes,setNotes]=useState(initial?.notes||'');
 return <Modal title={editTitle} subtitle={t('workspaceSubtitle')} onClose={onClose} drawer><form className="modal-form" onSubmit={e=>{e.preventDefault();if(name.trim()){let parsed={...properties};try{for(const k of ['attributes','methods','values'])if(typeof parsed[k]==='string')parsed[k]=JSON.parse(parsed[k]||'[]')}catch{window.alert(lang==='ar'?'تحقق من تنسيق الحقول':'Check field format');return;}onSave(type,{characterColor,locationLinks,properties:parsed,name:name.trim(),description:description.trim(),role,age:Number(age)||undefined,locationType,eventType,presentationOrder:Number(presentationOrder)||1,chronologicalOrder:Number(chronologicalOrder)||undefined,notes,x:position?.x??initial?.x,y:position?.y??initial?.y},initial?.id)}}}><FormField label={t('kind')}><select value={type} onChange={e=>setType(e.target.value)} disabled={!!initial}>{availableTypes.map(k=><option key={k} value={k}>{technical?(TECH_NODE_LABELS[k]||t(k)):t(k)}</option>)}</select></FormField><FormField label={`${t('name')} *`}><input autoFocus required maxLength={180} value={name} onChange={e=>setName(e.target.value)} placeholder={technical?'e.g. UserService':lang==='ar'?'أعطِ العنصر اسمًا':'Give it a name'}/></FormField><FormField label={t('description')}><textarea rows={3} value={description} onChange={e=>setDesc(e.target.value)} placeholder={t('descPlaceholder')}/></FormField>{type==='characters'&&initial?.entityId&&linkedPlayable!==null&&<div className="sana-character-play-status"><strong>{linkedPlayable?(lang==='ar'?'مستخدمة في توازن الشخصيات':'Used in character balance'):(lang==='ar'?'الشخصية محفوظة في القصة':'Story character saved')}</strong><span>{linkedPlayable?(linkedPlayable.name||'—'):(lang==='ar'?'يمكن استخدام هذه الشخصية في التوازن دون حذفها من الخريطة.':'You can use this character in balance without removing it from the map.')}</span><button type="button" onClick={onGameBalance}>{lang==='ar'?'فتح توازن الشخصيات':'Open Character Balance'}</button></div>}{type==='characters'&&<fieldset className="sana-character-palette"><legend>{lang==='ar'?'لون الشخصية':'Character color'}</legend><div className="sana-character-swatches">{CHARACTER_PALETTE.map(option=><button key={option.color} type="button" className={characterColor===option.color?'selected':''} style={{'--swatch':option.color}} onClick={()=>setCharacterColor(option.color)} title={option.name} aria-label={option.name} aria-pressed={characterColor===option.color}><span style={{background:option.color}}/>{characterColor===option.color&&<Check size={14}/>}</button>)}</div><small>{lang==='ar'?'اختر اللون الذي يميز الشخصية داخل الخريطة.':'Choose the color that best represents this character on the map.'}</small></fieldset>}{type==='characters'&&<div className="form-split"><FormField label={t('characterRole')}><input value={role} onChange={e=>setRole(e.target.value)} placeholder={lang==='ar'?'البطل، المساعد...':'Protagonist, ally...'}/></FormField><FormField label={t('age')}><input type="number" min="1" value={age} onChange={e=>setAge(e.target.value)} placeholder="24"/></FormField></div>}{type==='locations'&&<FormField label={t('placeType')}><input value={locationType} onChange={e=>setLocationType(e.target.value)} placeholder={lang==='ar'?'مدينة، قلعة، مكتبة...':'City, castle, library...'}/></FormField>}{type==='events'&&<><FormField label={t('eventType')}><input value={eventType} onChange={e=>setEventType(e.target.value)} placeholder={lang==='ar'?'اكتشاف، صراع، تحول...':'Discovery, conflict...'}/></FormField><div className="form-split"><FormField label={t('displayOrder')}><input type="number" min="1" value={presentationOrder} onChange={e=>setOrder(e.target.value)}/></FormField><FormField label={t('chronologicalOrder')}><input type="number" min="1" value={chronologicalOrder} onChange={e=>setChrono(e.target.value)}/></FormField></div></> }{type==='locations'&&initial?.entityId&&<details className="spark-properties"><summary>{t('worldReference')} / {t('parentLocation')}</summary>{[['worlds','world','worldReference'],['locations','parent','parentLocation']].map(([kind,path,key])=><label className="form-field" key={path}><span>{t(key)}</span><select defaultValue="" value={locationLinks[path]||''} onChange={e=>setLocationLinks({...locationLinks,[path]:e.target.value})}><option value="">{lang==='ar'?'دون تغيير':'Keep existing link'}</option>{project.elements.filter(e=>e.type===kind&&e.entityId&&e.id!==initial.id).map(e=><option key={e.id} value={e.entityId}>{e.name}</option>)}</select></label>)}</details>}<DiagramPropertiesEditor type={type} properties={properties} onChange={setProperties} lang={lang} creative={['writing','games'].includes(project?.domain)}/><FormField label={`${t('notes')} · ${t('optional')}`}><textarea rows={2} value={notes} onChange={e=>setNotes(e.target.value)}/></FormField><div className="modal-actions">{initial&&<Btn variant="danger" onClick={()=>onDelete?.(initial)} icon={Trash2} disabled={busy}>{t('delete')}</Btn>}<Btn variant="ghost" onClick={onClose}>{t('cancel')}</Btn><Btn type="submit" icon={Check} disabled={busy||!name.trim()}>{t('save')}</Btn></div></form></Modal>
}
function RelationModal({t,lang,project,elements,onClose,onSave,busy,initial}){
 const technical=project?.domain==='technology';const [source,setSource]=useState(String(initial?.source||'')),[target,setTarget]=useState(String(initial?.target||'')),[type,setType]=useState(initial?.type||'CUSTOM'),[label,setLabel]=useState(initial?.label||'');const [properties,setProperties]=useState(initial?.properties||{});const [choices,setChoices]=useState(['CUSTOM']);const [typesBusy,setTypesBusy]=useState(false);useEffect(()=>{if(!source||!target||source===target)return;let active=true;setTypesBusy(true);repository.allowedRelationTypes(project.id,source,target,initial?.id).then(result=>{if(active){setChoices(result);setType(result.includes(initial?.type)?initial.type:result.includes('CUSTOM')?'CUSTOM':result[0]||'')}}).catch(()=>{if(active){setChoices([]);setType('')}}).finally(()=>{if(active)setTypesBusy(false)});return()=>{active=false}},[source,target,project.id]);
 return <Modal title={t('newRelation')} subtitle={lang==='ar'?'حدد عنصرين، ثم نوع العلاقة بينهما':'Choose two elements and how they connect'} onClose={onClose} drawer><form className="modal-form" onSubmit={e=>{e.preventDefault();if(source&&target&&source!==target)onSave({source,target,type,label:label.trim()||(project.domain==='education'&&lang==='ar'?(type==='BRANCH'?'يتفرع إلى':type==='RELATED'?'مرتبط بـ':''):''),properties,id:initial?.id})}}><FormField label={t('source')}><select required value={source} onChange={e=>setSource(e.target.value)}><option value="">— {t('source')} —</option>{elements.map(e=><option key={e.id} value={e.id}>{e.name} · {t(e.type)}</option>)}</select></FormField><FormField label={t('target')}><select required value={target} onChange={e=>setTarget(e.target.value)}><option value="">— {t('target')} —</option>{elements.filter(e=>String(e.id)!==source).map(e=><option key={e.id} value={e.id}>{e.name} · {t(e.type)}</option>)}</select></FormField><FormField label={t('relationType')}><select value={type} onChange={e=>setType(e.target.value)}>{choices.map(v=><option key={v} value={v}>{technical?v.replaceAll('_',' '):t(`relation_${v}`)}</option>)}</select></FormField>{['ARCHITECTURE','CLASS_DIAGRAM'].includes(project.activeType)&&<FormField label={technical?(project.activeType==='ARCHITECTURE'?'Protocol':'Multiplicity'):t(project.activeType==='ARCHITECTURE'?'protocol':'multiplicity')}><input maxLength={project.activeType==='ARCHITECTURE'?255:50} value={properties[project.activeType==='ARCHITECTURE'?'protocol':'multiplicity']||''} onChange={e=>setProperties({...properties,[project.activeType==='ARCHITECTURE'?'protocol':'multiplicity']:e.target.value})}/></FormField>}<FormField label={`${t('relationLabel')} · ${t('optional')}`}><input maxLength={50} value={label} onChange={e=>setLabel(e.target.value)} placeholder={technical?'e.g. REST API / depends on':lang==='ar'?'مثال: يرتبط بـ':'e.g. Related to'}/></FormField><div className="modal-actions"><Btn variant="ghost" onClick={onClose}>{t('cancel')}</Btn><Btn type="submit" icon={Link2} disabled={busy||typesBusy||!type||!choices.length||!source||!target||source===target}>{t('create')}</Btn></div></form></Modal>
}
function ConfirmModal({t,lang,title,description,onCancel,onConfirm,busy,impact}){return <Modal title={title||t('confirmDelete')} onClose={onCancel}><div className="confirm-panel"><span className="danger-symbol"><Trash2 size={24}/></span><p>{description||t('confirmDelete')}</p>{impact&&<div className="spark-delete-impact"><strong>{t('deleteImpact')}</strong><p>{impact.relationsCount??impact.relationCount??impact.relations?.length??0} {t('relations')}</p>{(impact.blockingReasons||[]).map((reason,i)=><p key={i}>{reason}</p>)}</div>}<div className="modal-actions"><Btn variant="ghost" onClick={onCancel}>{t('cancel')}</Btn><Btn variant="danger" onClick={onConfirm} disabled={busy||impact?.canDelete===false} icon={Trash2}>{t('delete')}</Btn></div></div></Modal>}
function Scene({onStart,t}){return <div className="hero-scene" aria-label="SANA creative universe"><div className="scene-arch scene-arch-outer"/><div className="scene-arch scene-arch-inner"/><div className="scene-rays"/><div className="scene-sun"/><div className="scene-hill hill-back"/><div className="scene-hill hill-front"/><div className="scene-stars"><i>✦</i><i>✧</i><i>✦</i><i>✧</i><i>✦</i></div><img className="hero-brand" src="/sana-logo.webp" alt="SANA"/><div className="orbit-card card-one"><Lightbulb size={17}/><span>{t('stepOne')}</span></div><div className="orbit-card card-two"><Network size={17}/><span>{t('stepThree')}</span></div><div className="orbit-card card-three"><Sparkles size={17}/><span>{t('stepFour')}</span></div><span className="scene-caption">SANA · CREATIVE SPACE</span></div>}
// Home: write an idea in the hero and "Start with SANA AI" runs right here (suggest → approve → draw → open the Studio).
// The orbit around the SANA mark reacts: it listens while the user types and spins up while SANA is thinking.
// Ported from docs/frontend "SANA User Flow – الرئيسية — Home (Arabic, RTL)".
const HOME_EXAMPLES=[
 {ar:'حكاية مدينة ضاعت في الرمل',en:'A tale of a city lost in the sand'},
 {ar:'تطبيق يرتّب مواعيد عيادة',en:'An app that organizes clinic appointments'},
 {ar:'عربة قهوة على الكورنيش',en:'A coffee cart on the corniche'},
 {ar:'رحلة مذاكرة داخل الخلية',en:'A study trip inside the cell'}
];
// [rotation°, height, width, duration s, delay s] of each light ray
const HOME_RAYS=[[8,120,2,6.2,-1.1],[47,96,3,7.8,-4.0],[83,132,2,5.4,-2.6],[121,104,2,8.6,-6.3],[162,118,3,6.9,-0.4],[199,90,2,7.3,-3.7],[236,128,2,5.9,-5.2],[268,100,3,9.1,-2.0],[307,140,2,6.5,-4.8],[341,110,2,8.0,-1.9]];
const HOME_STARS=[
 [{left:30,top:46,fontSize:16},-0.6,'✦'],[{right:26,top:30,fontSize:12},-2.1,'✧'],[{right:8,top:132,fontSize:18,color:'#9CD6CC'},-3.4,'✦'],
 [{left:8,top:150,fontSize:11},-1.5,'✧'],[{left:92,bottom:6,fontSize:13},-2.8,'✦'],[{right:96,top:4,fontSize:10,color:'#9CD6CC'},-0.2,'✦'],[{right:74,bottom:16,fontSize:9},-3.9,'✧']
];
function Landing({t,lang,projects,onNavigate,onNew,onOpen,guest=false,onLogin,canCreateProject,onReady}){
 const {idea,setIdea,suggestion,choice,setChoice,changing,setChanging,options,error,stage,clarification,answer,setAnswer,advice,fixing,
  working,locked,classify,changeDomain,draw,accept,answerClarification,domains,openProject}=useQuickStart({lang,canCreateProject,onReady});
 const say=(ar,en)=>lang==='ar'?ar:en;
 const [typing,setTyping]=useState(false),[focused,setFocused]=useState(false),[phase,setPhase]=useState(0),[off,setOff]=useState(false);
 // Lite mode for weak devices: 4 GB of memory or less, or 4 CPU cores or fewer
 const [lite]=useState(()=>{const n=typeof navigator==='object'?navigator:{};return (n.deviceMemory>0&&n.deviceMemory<=4)||(n.hardwareConcurrency>0&&n.hardwareConcurrency<=4)});
 const stageEl=useRef(null),heroEl=useRef(null),typeTimer=useRef(0);
 const motion=useRef({rate:1,think:0,focus:0,pulse:0,targetRate:1,targetThink:0,targetFocus:0,raf:0,anims:[],written:{},off:false});
 const noIdea=!idea.trim();
 // Orbit mood: idle drifts, typing holds still to listen, a pause ponders, SANA's work spins it up
 const mode=working?'thinking':['suggested','ready','clarify','advice','failed'].includes(stage)?'settled':typing?'listening':noIdea?'idle':'pondering';
 // The easing loop only runs while something is still changing; once the orbit settles it sleeps until the next change
 const tick=useRef(null);
 tick.current=()=>{
  const m=motion.current,el=stageEl.current;m.raf=0;
  if(!el||m.off)return;
  m.rate+=(m.targetRate-m.rate)*.035;m.think+=(m.targetThink-m.think)*.04;m.focus+=(m.targetFocus-m.focus)*.06;m.pulse*=.95;
  const settled=Math.abs(m.targetRate-m.rate)<.002&&Math.abs(m.targetThink-m.think)<.002&&Math.abs(m.targetFocus-m.focus)<.002&&m.pulse<.002;
  if(settled){m.rate=m.targetRate;m.think=m.targetThink;m.focus=m.targetFocus;m.pulse=0}
  for(const a of m.anims)if(a.playbackRate!==m.rate)a.playbackRate=m.rate;
  // Write a CSS variable only when its rounded value actually changes
  const write=(name,v)=>{const text=v.toFixed(3);if(m.written[name]!==text){m.written[name]=text;el.style.setProperty(name,text)}};
  write('--think',m.think);write('--focus',m.focus);write('--glow',Math.max(m.think,m.focus));write('--pulse',m.pulse);
  if(!settled)m.raf=requestAnimationFrame(()=>tick.current());
 };
 const wake=()=>{const m=motion.current;if(m.raf||!stageEl.current||m.off)return;m.anims=stageEl.current.getAnimations?stageEl.current.getAnimations({subtree:true}):[];m.raf=requestAnimationFrame(()=>tick.current())};
 const bump=value=>{motion.current.pulse=Math.min(1,value);wake()};
 useEffect(()=>{const m=motion.current;m.targetRate={idle:1,listening:.12,pondering:.45,thinking:3,settled:.8}[mode];m.targetThink=mode==='thinking'?1:0;m.targetFocus=focused?1:0;wake()},[mode,focused]);
 useEffect(()=>{if(stage==='suggested')bump(1)},[stage]);
 // Pause every hero animation while the hero is scrolled out of view
 useEffect(()=>{
  const m=motion.current;let observer;
  if(typeof IntersectionObserver==='function'&&heroEl.current){
   observer=new IntersectionObserver(entries=>{const isOff=!entries[entries.length-1].isIntersecting;m.off=isOff;setOff(isOff);if(!isOff)wake()});
   observer.observe(heroEl.current);
  }
  return()=>{cancelAnimationFrame(m.raf);m.raf=0;observer?.disconnect();clearTimeout(typeTimer.current)};
 },[]);
 // A guest's idea survives signing in
 useEffect(()=>{try{const saved=sessionStorage.getItem('sana-home-idea');if(saved){sessionStorage.removeItem('sana-home-idea');setIdea(saved)}}catch{}},[]);
 // Speech bubbles follow the real work: reading and classifying the idea, then drawing it
 useEffect(()=>{
  if(stage==='classifying'){setPhase(0);const timer=setTimeout(()=>setPhase(1),2400);return()=>clearTimeout(timer)}
  if(stage==='creating'||stage==='drawing')setPhase(2);
 },[stage]);
 const steps=[say('نقرأ ما بين سطورك','Reading between your lines'),say('نلقى لفكرتك موطنها','Finding where your idea belongs'),say('نرسم أولى ملامحها','Sketching its first lines')];
 const captions={
  listening:say('سنا تسمعك…','SANA is listening…'),
  pondering:say('على راحتك، الفكرة تنضج على مهل','Take your time, the idea is ripening'),
  thinking:fixing?say('سنا تضبط تفاصيل الرسم…','SANA is refining the details…'):say('نحيك فكرتك نجمةً نجمة…','Weaving your idea, star by star…'),
  settled:say('وهذي شرارتك ✦','And here is your spark ✦')
 };
 // While SANA reads the idea, its first words lift out of the box and are drawn into the orbit
 const flyWords=stage==='classifying'?idea.trim().split(/\s+/).filter(Boolean).slice(0,6).map((text,i,all)=>({
  text,sx:Math.round((lang==='ar'?-1:1)*(all.length>1?-200+(400*i)/(all.length-1):0)),sy:440+((i*37)%60),delay:(0.15+i*0.2).toFixed(2)
 })):[];
 const onIdea=e=>{
  bump(motion.current.pulse+.3);clearTimeout(typeTimer.current);
  typeTimer.current=setTimeout(()=>setTyping(false),1400);setTyping(true);setIdea(e.target.value);
 };
 async function submit(e){
  e.preventDefault();if(noIdea||locked)return;
  clearTimeout(typeTimer.current);setTyping(false);bump(1);
  // Guests sign in first; the idea is waiting in the box when they come back
  if(guest){try{sessionStorage.setItem('sana-home-idea',idea)}catch{}onLogin?.();return}
  if(!API_MODE){onNew();return}
  await classify();
 }
 const recent=guest?[]:[...projects].sort((a,b)=>new Date(b.updatedAt||0)-new Date(a.updatedAt||0)).slice(0,3);
 const Forward=lang==='ar'?ArrowLeft:ArrowRight;
 return <div className={['sh-home',lite?'sh-lite':'',off?'sh-off':''].join(' ').trim()}>
  <section ref={heroEl} id="top" className="sh-hero">
   <div className="sh-space" aria-hidden="true"><div className="sh-nebula"/><div className="sh-band"><div className="sh-dust"/></div><div className="sh-stars sh-stars-s"/><div className="sh-stars sh-stars-m"/><div className="sh-stars sh-stars-l"/><div className="sh-shoot"/></div>
   <span aria-hidden="true" className="sh-decor" style={{left:'9%',top:'18%',color:'#FAE3AE',opacity:.55,fontSize:18}}>✦</span>
   <span aria-hidden="true" className="sh-decor" style={{right:'12%',top:'12%',color:'#FAE3AE',opacity:.4,fontSize:13}}>✧</span>
   <span aria-hidden="true" className="sh-decor" style={{right:'7%',bottom:'22%',color:'#41B7B4',opacity:.5,fontSize:16}}>✦</span>
   <span aria-hidden="true" className="sh-decor" style={{left:'15%',bottom:'14%',color:'#FAE3AE',opacity:.35,fontSize:12}}>✧</span>
   <div className="sh-hero-content">
    <div className="sh-stage">
     <div className="sh-fx" aria-hidden="true">
      {working&&<><span className="sh-wave"/><span className="sh-wave" style={{animationDelay:'1.05s'}}/></>}
      {flyWords.map((w,i)=><span key={i} className="sh-word" style={{'--sx':`${w.sx}px`,'--sy':`${w.sy}px`,animationDelay:`${w.delay}s`}}><span><span>{w.text}</span></span></span>)}
      {working&&<span key={phase} className={`sh-bubble ${phase===1?'sh-bubble-l':'sh-bubble-r'}`}>{steps[phase]}</span>}
      {stage==='suggested'&&<span className="sh-bloom"/>}
     </div>
     <div className="sh-orbit" ref={stageEl} aria-hidden="true">
      <div className="sh-halo"/><div className="sh-halo-hot"/>
      <div className="sh-rays">{HOME_RAYS.map(([rotate,height,width,duration,delay],i)=><i key={i} className="sh-ray" style={{rotate:`${rotate}deg`,height,width,marginLeft:-width/2,animationDuration:`${duration}s`,animationDelay:`${delay}s`}}/>)}</div>
      <div className="sh-aura"/>
      {['c','b','a','d'].map(k=><div key={k} className={`sh-ring sh-ring-${k}`}><div className="sh-spin"/></div>)}
      <img className="sh-mark" src="/sana-mark-original.png" alt=""/>
      {['a','b'].map(k=><span key={k} className={`sh-track sh-track-${k}`}><span className="sh-rx"><span className="sh-ry"><span className="sh-rider"><span>✦</span></span></span></span></span>)}
      <div className="sh-starset">{HOME_STARS.map(([style,delay,glyph],i)=><span key={i} className="sh-star" style={{...style,animationDelay:`${delay}s`}}>{glyph}</span>)}</div>
     </div>
     <div className="sh-caption" aria-live="polite" style={{opacity:captions[mode]?1:0}}>{captions[mode]||' '}</div>
    </div>
    <h1 className="sh-title">{say('في كل فكرة سنا','In every idea, a spark')}<br/><span>{say('ينتظر من يوقده','waiting to be lit')}</span></h1>
    <p className="sh-lead">{say('أطلق العنان لأفكارك وعبّر عمّا في خاطرك، وسنا تحوّله نور يرسم لفكرتك الطريق','Let your ideas out and say what is on your mind; SANA turns it into light that shows your idea the way.')}</p>
    <form className={`sh-prompt ${working?'sh-busy':''}`} onSubmit={submit}>
     <label htmlFor="sh-idea">{say('وش الفكرة اللي تشغل بالك؟','What idea is on your mind?')}</label>
     <textarea id="sh-idea" className="sh-idea" rows={3} maxLength={5000} value={idea} disabled={locked}
      onChange={onIdea} onFocus={()=>{bump(1);setFocused(true)}} onBlur={()=>setFocused(false)}
      placeholder={say('مثلًا: حكاية مدينة ضاعت بين كثبان الربع الخالي، ورسّام خرايط يلقاها صدفة ذات فجر…','For example: a city lost among the dunes of the Empty Quarter, found by chance one dawn by a mapmaker…')}/>
     <div className="sh-prompt-actions">
      <button type="submit" className="sh-gold" disabled={noIdea||locked}>{stage==='classifying'?say('سنا تقرأ فكرتك…','SANA is reading your idea…'):say('أطلق الشرارة مع سنا','Ignite it with SANA')}<Forward size={17}/></button>
     </div>
    </form>
    {stage==='idle'&&<div className="sh-examples">{HOME_EXAMPLES.map(ex=><button type="button" key={ex.en} onClick={()=>{bump(.8);setIdea(say(ex.ar,ex.en))}}>{say(ex.ar,ex.en)}</button>)}</div>}
    {working&&<div role="status" className="sh-sr">{steps[phase]}</div>}
    {suggestion&&choice&&<div role="status" className="sh-reveal sh-card">
     <div className="sh-card-row">
      <div className="sh-card-main">
       <span className="sh-card-kicker">{say('من وحي فكرتك','Inspired by your idea')}</span>
       {changing&&!locked
        ?<input className="sh-card-input" aria-label={say('اسم المشروع','Project name')} value={choice.name||''} maxLength={100} onChange={e=>setChoice(c=>({...c,name:e.target.value}))}/>
        :<span className="sh-card-name">{choice.name}</span>}
       {changing&&!locked
        ?<div className="sh-card-selects">
          <select aria-label={say('المجال','Domain')} value={choice.domain} onChange={e=>changeDomain(e.target.value)}>{domains.map(d=><option key={d} value={d}>{t(d)}</option>)}</select>
          <select aria-label={say('المخطط','Diagram')} value={choice.diagramType} onChange={e=>setChoice(c=>({...c,diagramType:e.target.value}))}>{(options[choice.domain]||[]).map(type=><option key={type} value={type}>{t(`diagram_${type}`)}</option>)}</select>
         </div>
        :<span className="sh-card-meta">{t(choice.domain)} · {t(`diagram_${choice.diagramType}`)}</span>}
      </div>
      {stage==='suggested'&&<div className="sh-card-actions">
       <button type="button" className="sh-ghost" onClick={()=>setChanging(v=>!v)}>{changing?say('تم','Done'):say('غيّرها','Change it')}</button>
       <button type="button" className="sh-gold" onClick={accept}>{say('تمام، ارسمها','Great, draw it')}</button>
      </div>}
     </div>
     {stage==='suggested'&&suggestion.reason&&<p className="sh-card-reason">{suggestion.reason}</p>}
     {['creating','drawing','ready'].includes(stage)&&<p className="sh-card-status">{stage==='creating'?say('نجهّز مساحة مشروعك…','Preparing your project space…')
      :stage==='ready'?say('اكتملت! نفتح لك مساحة العمل…','Done! Opening your workspace…')
      :fixing?say('سنا تضبط تفاصيل الرسم…','SANA is refining the drawing…')
      :say('سنا ترسم فكرتك الآن، وتنتقل إليها فور اكتمالها','SANA is drawing your idea; it opens as soon as it is ready')}</p>}
     {stage==='clarify'&&clarification&&<div className="sh-card-clarify">
      {clarification.message&&<p>{clarification.message}</p>}
      <strong>{clarification.question}</strong>
      {clarification.answers.length>0&&<div className="sh-examples">{clarification.answers.map((text,i)=><button type="button" key={i} onClick={()=>answerClarification(text)}>{text}</button>)}</div>}
      <form onSubmit={e=>{e.preventDefault();answerClarification(answer)}}><input aria-label={say('إجابتك','Your answer')} value={answer} onChange={e=>setAnswer(e.target.value)} maxLength={1500} placeholder={say('أو اكتب إجابتك','Or type your answer')}/><button type="submit" className="sh-gold" disabled={!answer.trim()}>{say('كمّل','Continue')}</button></form>
     </div>}
     {stage==='advice'&&<div className="sh-card-clarify"><p>{advice}</p><div className="sh-card-actions"><button type="button" className="sh-ghost" onClick={openProject}>{say('افتح المشروع','Open project')}</button><button type="button" className="sh-gold" onClick={draw}>{say('حاول الرسم مرة ثانية','Try drawing again')}</button></div></div>}
     {stage==='failed'&&<div className="sh-card-actions"><button type="button" className="sh-ghost" onClick={openProject}>{say('افتح المشروع','Open project')}</button><button type="button" className="sh-gold" onClick={draw}>{say('حاول مرة ثانية','Try again')}</button></div>}
    </div>}
    {error&&<p role="alert" className="sh-error">{error}</p>}
    <div className="sh-divider"><span/>{say('أو امشِ الدرب بنفسك','Or find your own way')}<span/></div>
    <div className="sh-secondary">
     <button type="button" className="sh-outline" onClick={()=>onNew()}><Plus size={17}/>{say('ابدأ مشروعك من أول سطر','Start your project from scratch')}</button>
     <button type="button" className="sh-link" onClick={()=>onNavigate('library')}>{say('استلهم من القوالب','Get inspired by templates')}</button>
    </div>
   </div>
  </section>
  {recent.length>0&&<section className="sh-recent">
   <div className="sh-recent-head"><h2>{say('كمّل من حيث وقفت','Pick up where you left off')}</h2><button type="button" onClick={()=>onNavigate('projects')}>{say('كل مشاريعك ←','All your projects →')}</button></div>
   <div className="sh-recent-grid">{recent.map(p=><button type="button" key={p.id} onClick={()=>onOpen(p.id)}>
    <span className="sh-recent-domain">{t(p.domain)}</span>
    <span className="sh-recent-name">{p.name}</span>
    <span className="sh-recent-date">{say('آخر لمسة','Last touched')} {dateLabel(p.updatedAt,lang)}</span>
   </button>)}</div>
  </section>}
 </div>;
}
function Projects({user,projects,t,lang,onOpen,onNew,onDelete,onEdit,onImport}){const [query,setQuery]=useState(''),[domain,setDomain]=useState('all');let recent=[];try{recent=JSON.parse(localStorage.getItem(`sana-recent-${user?.id||'local'}`)||'[]').map(id=>projects.find(p=>String(p.id)===id)).filter(Boolean)}catch{}const shown=projects.filter(p=>(domain==='all'||p.domain===domain)&&`${p.name} ${p.description}`.toLowerCase().includes(query.toLowerCase()));return <div className="main-shell inner-page page-enter"><div className="page-intro spark-project-intro"><div className="spark-project-intro-copy"><span className="eyebrow"><span className="eyebrow-dot"/>{t('mySpace')}</span><h1>{user?.name?`${t('welcome')}، ${user.name}`:t('allProjects')}<span className="spark-heading-star" aria-hidden="true">✦</span></h1><p>{t('personalWelcome')}</p><div className="spark-intro-actions"><Btn icon={Plus} onClick={onNew}>{t('newProject')}</Btn><span className="spark-intro-counter"><FolderClosed size={16}/>{projects.length} {t('projects')}</span><button className="sana-my-plan-link" onClick={()=>window.location.hash='/membership'}><Sparkles size={15}/>{lang==='ar'?'باقتي':'My plan'}</button></div></div><div className="spark-dashboard-art" aria-hidden="true"><span className="spark-dashboard-art-core">✦</span><i/><i/><i/><b> SANA </b></div></div>{recent.length>0&&<div className="spark-resume"><div><small>{t('recentOnDevice')}</small><strong>{recent[0].name}</strong></div><Btn icon={ArrowUpLeft} onClick={()=>onOpen(recent[0].id)}>{t('resume')}</Btn></div>}<div className="toolbar surface-toolbar"><div className="search-field"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder={t('search')} aria-label={t('search')}/></div><div className="filter-pills">{[['all','all'],['writing','writing'],['games','games'],['technology','technology'],['business','business'],['education','education']].map(([v,l])=><button key={v} className={domain===v?'active':''} onClick={()=>setDomain(v)}>{t(l)}</button>)}</div><div className="projects-toolbar-actions">{!API_MODE&&<Btn icon={Upload} variant="outline-dark" onClick={onImport}>{t('import')}</Btn>}<Btn icon={Plus} onClick={onNew}>{t('newProject')}</Btn></div></div>{shown.length?<div className="projects-grid projects-page-grid">{shown.map(p=><div key={p.id} className="project-wrap"><ProjectCard project={p} t={t} lang={lang} onClick={()=>onOpen(p.id)}/><div className="project-actions"><IconButton label={t('edit')} onClick={()=>onEdit(p)}><Edit3 size={15}/></IconButton><IconButton label={t('delete')} className="danger-hover" onClick={()=>onDelete(p)}><Trash2 size={15}/></IconButton></div></div>)}</div>:<Empty title={t('noItems')} description={t('noProjectsBody')} action={<Btn icon={Plus} onClick={onNew}>{t('newProject')}</Btn>}/>}</div>}
function ToolsPage({t,lang,onNew,onOpen,projects,onNavigate}){
 const groups=[{key:'creativity',icon:BookOpen,types:['characters','worlds','events','moodboard','branching','balance'],desc:lang==='ar'?'الشخصيات، العوالم، لوحات الإلهام، وأدوات الألعاب':'Characters, worlds, moodboards & game tools'},{key:'technology',icon:Code2,types:['service','database','client'],desc:lang==='ar'?'البنية البرمجية والتخطيط التقني':'Architecture and systems'},{key:'business',icon:BriefcaseBusiness,types:['customer_segment','value_proposition','channels'],desc:lang==='ar'?'نموذج الأعمال وخطة الوصول':'Business modeling and market plans'},{key:'education',icon:GraduationCap,types:['central_topic','topic','note'],desc:lang==='ar'?'خرائط ذهنية للتعلّم والمذاكرة':'Mind maps for learning'}];
 return <div className="main-shell inner-page page-enter"><div className="page-intro"><span className="eyebrow">{t('toolkit')}</span><h1>{t('projectTypes')}</h1><p>{t('projectTypesDesc')}</p></div><div className="domain-feature-grid">{groups.map(g=>{const Icon=g.icon;return <article key={g.key} className={`domain-feature domain-feature-${g.key}`}><span className="domain-feature-icon"><Icon size={31}/></span><h2>{t(g.key)}</h2><p>{g.desc}</p><div className="domain-feature-tags">{g.types.map(k=><span key={k}>{t(k)}</span>)}</div><Btn onClick={()=>onNavigate('library')} icon={LayoutTemplate}>{t('templateRoute')}</Btn></article>})}</div><section className="extra-featured-tools"><div className="section-heading"><div><span className="eyebrow">SANA CREATIVE LAB</span><h2>{lang==='ar'?'أدوات إبداعية تستحق التجربة':'Explore creative tools'}</h2><p>{lang==='ar'?'ادخلي الأداة من مشروع موجود، أو ابدئي مشروعك الأول.':'Open a tool in an existing project, or create a new one.'}</p></div></div><div className="extra-feature-cards">{[{tab:'moodboard',domain:'writing',icon:ImageIcon,desc:lang==='ar'?'لوحات صور وملاحظات وألوان':'Image and note boards'},{tab:'branching',domain:'games',icon:GitBranch,desc:lang==='ar'?'شجرة حوارات واختيارات اللاعب':'Dialogue and story choices'},{tab:'balance',domain:'games',icon:Gamepad2,desc:lang==='ar'?'إحصائيات وتوازن الشخصيات':'Character stats and balance'}].map(item=>{const Icon=item.icon;const existing=projects.find(p=>p.domain===item.domain);return <article key={item.tab} className={`extra-feature-card extra-${item.tab}`}><span><Icon size={27}/></span><h3>{t(item.tab)}</h3><p>{item.desc}</p><Btn icon={ArrowUpLeft} onClick={()=>existing?onNavigate('studio',existing.id,item.tab):onNew({domain:item.domain})}>{existing?(lang==='ar'?'افتح الأداة':'Open tool'):(lang==='ar'?'أنشئ مشروعًا':'Create project')}</Btn></article>})}</div></section></div>
}
function LibraryPage({t,lang,onNew,initialDomain='all'}){
 const [domain,setDomain]=useState(['all','creativity','technology','business','education'].includes(initialDomain)?initialDomain:'all');const groups=[['all','allDomains'],['creativity','creativity'],['technology','technology'],['business','business'],['education','education']];
 const shown=TEMPLATE_CATALOG.filter(item=>domain==='all'||item.group===domain);
 return <div className="main-shell inner-page page-enter"><div className="page-intro"><span className="eyebrow">SANA · TEMPLATE LIBRARY</span><h1>{t('templatesTitle')}</h1><p>{t('templatesSubtitle')}</p></div><div className="template-filter" role="group" aria-label={t('domain')}>{groups.map(([id,key])=><button key={id} className={domain===id?'active':''} onClick={()=>setDomain(id)}>{t(key)}</button>)}</div><div className="template-grid">{shown.map(item=>{const Icon=item.group==='technology'?Code2:item.group==='business'?BriefcaseBusiness:item.group==='education'?GraduationCap:item.domain==='games'?Gamepad2:BookOpen;return <article className={`template-card template-${item.group}`} key={item.id}><div className="template-preview"><span className="template-corner">SANA / {item.group.toUpperCase()}</span><Icon size={42} strokeWidth={1.5}/><span className="template-ornament">✦</span></div><div className="template-body"><DomainPill domain={item.domain} t={t}/><h3>{lang==='ar'?item.ar:item.en}</h3><p>{lang==='ar'?item.arDesc:item.enDesc}</p><Btn icon={ArrowUpLeft} onClick={()=>onNew({templateId:item.id,domain:item.domain,name:lang==='ar'?item.ar:item.en,description:lang==='ar'?item.arDesc:item.enDesc})}>{t('startProject')}</Btn></div></article>})}</div></div>
}
// "Start with SANA AI", shared by the Home page and the Start page: SANA suggests a domain, diagram and name for an
// idea; after the user approves, the project is created, the AI draws the diagram, the result is applied and the
// Studio opens only when the diagram is ready.
function useQuickStart({lang,canCreateProject,onReady}){
 const [idea,setIdeaText]=useState(''),[suggestion,setSuggestion]=useState(null),[choice,setChoice]=useState(null);
 const [changing,setChanging]=useState(false),[options,setOptions]=useState({}),[error,setError]=useState('');
 // The stages mirror the real quick-start API: classify, review, create, draw, or clarify.
 const [stage,setStage]=useState('idle'),[clarification,setClarification]=useState(null),[answer,setAnswer]=useState(''),[advice,setAdvice]=useState(''),[fixing,setFixing]=useState(false);
 const created=useRef(null),turns=useRef([]),alive=useRef(true);
 useEffect(()=>{alive.current=true;return()=>{alive.current=false}},[]);
 const say=(ar,en)=>lang==='ar'?ar:en;
 const working=['classifying','creating','drawing'].includes(stage),locked=working||Boolean(created.current);
 // Editing the idea discards an earlier suggestion
 const setIdea=value=>{setIdeaText(value);setSuggestion(null);setStage('idle')};
 const resetSuggestion=()=>{setSuggestion(null);setStage('idle')};
 async function classify(){
  setError('');setStage('classifying');
  try{
   const [result,diagramOptions]=await Promise.all([quickStartApi.classify(idea,lang),quickStartApi.diagramOptions()]);
   if(!alive.current)return;
   setOptions(diagramOptions);setSuggestion(result);setChanging(false);
   setChoice({domain:result.domain,diagramType:result.diagramType,name:result.projectName});setStage('suggested');
  }catch(err){if(alive.current){setError(err.message||say('تعذر على سنا استكشاف الفكرة الآن. حاول مرة أخرى.','SANA could not explore your idea right now. Please try again.'));setStage('idle')}}
 }
 const changeDomain=domain=>setChoice(c=>({...c,domain,diagramType:(options[domain]||[])[0]||c.diagramType}));
 const fullPrompt=()=>[idea.trim(),...turns.current.map(turn=>`${say('سؤال','Question')}: ${turn.question}\n${say('جواب','Answer')}: ${turn.answer}`)].join('\n\n').slice(0,10000);
 async function draw(){
  const project=created.current;if(!project)return;
  setStage('drawing');setError('');setFixing(false);
  try{
   let run,feedback='';
   for(let attempt=1;attempt<=2;attempt++){
    const body={task:'CREATE',prompt:(fullPrompt()+feedback).slice(0,10000),diagramType:choice.diagramType};
    if(['writing','games'].includes(project.domain))body.conversationHistory=[];
    run=await aiApi.start(project.id,body,project.domain);
    for(let i=0;i<120&&alive.current&&['PENDING','RUNNING'].includes(run.status);i++){
     await new Promise(r=>setTimeout(r,2000));if(!alive.current)return;
     run=await aiApi.status(run.statusUrl);
    }
    if(!alive.current)return;
    if(run.status==='FAILED'&&run.error?.code==='INVALID_PROPOSAL'&&attempt<2){
     feedback=`\n\nYour previous diagram for this request was rejected: ${run.error.message}. Draw it again and follow the graph rules in the context exactly.`;
     setFixing(true);continue;
    }
    break;
   }
   setFixing(false);
   if(run.status!=='COMPLETED')throw new Error(run.error?.message||say('تعذر إكمال تصميم الفكرة الآن. حاول مرة أخرى.','SANA could not complete the design right now. Try again.'));
   const result=run.result;
   if(result.type==='CLARIFICATION'){
    setClarification({message:result.message,question:result.clarification?.question||'',answers:result.clarification?.suggestedAnswers||[]});
    setAnswer('');setStage('clarify');return;
   }
   if(result.type!=='PROPOSAL'||!run.proposalId){setAdvice(result.message||'');setStage('advice');return}
   // The user explicitly approved the project vision before creation; only then apply the AI draft.
   const applied=await aiApi.accept(project.id,run.proposalId);
   await repository.setActiveDiagram(project.id,applied.outputId);
   if(!alive.current)return;
   setStage('ready');
   // A brief *post-success* visual handoff (not a fabricated backend progress step).
   if(!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches)
    await new Promise(r=>setTimeout(r,650));
   if(alive.current)onReady(project.id);
  }catch(err){if(alive.current){setError(err.message);setStage('failed')}}
 }
 async function accept(){
  if(!choice?.name?.trim()){setError(say('اكتب اسم المشروع.','Enter a project name.'));return}
  setError('');
  if(!created.current){
   if(!await canCreateProject())return;
   setStage('creating');
   try{created.current=await repository.createProject({name:choice.name.trim(),description:idea.trim().slice(0,5000),domain:choice.domain,aiFirst:true})}
   catch(err){if(alive.current){setError(err.message);setStage('suggested')}return}
  }
  await draw();
 }
 function answerClarification(text){
  const value=String(text||'').trim();if(!value||!clarification||working)return;
  turns.current=[...turns.current,{question:clarification.question,answer:value}];setClarification(null);draw();
 }
 const domains=['technology','business','education','writing','games'].filter(d=>(options[d]||[]).length);
 const openProject=()=>{if(created.current?.id)onReady(created.current.id)};
 return {idea,setIdea,resetSuggestion,suggestion,choice,setChoice,changing,setChanging,options,error,setError,stage,clarification,answer,setAnswer,advice,fixing,
  working,locked,classify,changeDomain,draw,accept,answerClarification,domains,openProject};
}

// "Start with SANA AI": the idea goes straight to the AI, which suggests the domain, diagram and project name.
// After the user accepts, this page creates the project, lets the AI draw the diagram and applies it,
// and opens the Studio only when the diagram is ready.
function AIStartPage({t,lang,onTemplates,onNew,projects,onContinue,canCreateProject,onReady}){
 const [selected,setSelected]=useState('');
 const {idea,setIdea,resetSuggestion,suggestion,choice,setChoice,changing,setChanging,options,error,setError,stage,clarification,answer,setAnswer,advice,fixing,
  working,locked,classify,changeDomain,draw,accept,answerClarification,domains,openProject}=useQuickStart({lang,canCreateProject,onReady});
 const say=(ar,en)=>lang==='ar'?ar:en;
 const inMotion=working||stage==='ready';
 const stars=useMemo(()=>Array.from({length:25},(_,i)=>({
  x:4+((i*37+19)%93),y:3+((i*31+11)%92),size:i%6===0?3:2,
  delay:-((i*1.7)%8),duration:3.3+(i%5)*.7
 })),[]);
 const stageLabel=stage==='classifying'?say('سنا تستكشف الفكرة','SANA is discovering your idea')
  :stage==='creating'?say('بدأ تكوين عالمك','Your world is taking shape')
  :stage==='drawing'?(fixing?say('سنا تضبط تفاصيل التصور','SANA is refining the vision'):say('تترابط ملامح مشروعك الآن','Your project is coming together'))
  :stage==='ready'?say('عالمك الجديد أصبح جاهزًا','Your new world is ready')
  :stage==='clarify'?say('تفصيل صغير يصنع فرقًا','One more detail will help')
  :stage==='suggested'?say('اتضحت ملامح فكرتك','Your idea has taken shape')
  :say('كل عالم يبدأ بشرارة','Every world begins with a spark');
 async function submit(e){
  e.preventDefault();if(locked||!idea.trim())return;setError('');
  if(selected){onContinue(selected,idea);return}
  if(!API_MODE){sessionStorage.setItem('sana-ai-pending',idea);onNew({description:idea,aiFirst:true});return}
  await classify();
 }
 return <section className={`ai-start-page sana-cosmos sana-cosmos-${stage} page-enter`} aria-label={say('رحلة الفكرة مع سنا','SANA idea journey')}>
  <div className="sana-cosmos-backdrop" aria-hidden="true">
   <div className="sana-cosmos-aurora"/>
   <div className="sana-cosmos-stars">{stars.map((star,i)=><i key={i} style={{left:`${star.x}%`,top:`${star.y}%`,width:star.size,height:star.size,animationDelay:`${star.delay}s`,animationDuration:`${star.duration}s`}}/>)}</div>
   <div className="sana-cosmos-orbit sana-cosmos-orbit-a"/><div className="sana-cosmos-orbit sana-cosmos-orbit-b"/><div className="sana-cosmos-orbit sana-cosmos-orbit-c"/>
   <span className="sana-cosmos-drift sana-cosmos-drift-a">✦</span><span className="sana-cosmos-drift sana-cosmos-drift-b">✧</span>
  </div>
  <div className="sana-cosmos-content">
   <div className={`sana-cosmos-symbol ${inMotion?'is-awakening':''}`} aria-hidden="true">
    <div className="sana-cosmos-symbol-halo"/><div className="sana-cosmos-symbol-ring"/>
    <span className="sana-cosmos-symbol-star">✦</span>
   </div>
   <div className="sana-cosmos-overline"><span>✧</span> SANA · CREATIVE JOURNEY <span>✧</span></div>
   <h1>{stageLabel}</h1>
   <p className="sana-cosmos-intro">{working||stage==='ready'
    ?say('من شرارة صغيرة تتشكل فكرة تستحق أن ترى النور.','A small spark is becoming something real.')
    :say('احكِ لسنا ما يدور في بالك، ودع الفكرة تتحول إلى مشروع يمكنك تطويره.','Tell SANA what you imagine and watch it become a project you can grow.')}</p>
   <div className="sana-cosmos-steps" aria-label={say('مراحل الرحلة','Journey steps')}>
    {[{ar:'الشرارة',en:'Spark'},{ar:'الاستكشاف',en:'Explore'},{ar:'التصور',en:'Vision'},{ar:'التكوين',en:'Create'}].map((step,i)=>{
     const current=stage==='idle'?0:stage==='classifying'?1:['suggested','clarify'].includes(stage)?2:stage==='ready'?4:stage==='advice'||stage==='failed'?3:3;
     return <span key={i} className={i<current?'is-done':i===current?'is-active':''}><b>{String(i+1).padStart(2,'0')}</b>{say(step.ar,step.en)}</span>;
    })}
   </div>
   {!suggestion&&<form className="sana-cosmos-entry" onSubmit={submit}>
    <label htmlFor="sana-cosmos-idea"><Sparkles size={16}/>{say('اكتب الفكرة التي تريد أن تبدأ بها','Describe the idea you want to bring to life')}</label>
    <textarea id="sana-cosmos-idea" rows={5} maxLength={6000} required disabled={locked} value={idea}
     onChange={e=>setIdea(e.target.value)}
     placeholder={say('مثلاً: لعبة غموض في بلدة ساحلية، تتفرع أحداثها حسب قرارات اللاعب...','For example: a mystery game in a coastal town, with choices that shape the story...')}/>
    <div className="sana-cosmos-entry-options">
     <label htmlFor="sana-cosmos-existing">{say('أين تبدأ الفكرة؟','Where should the idea begin?')}</label>
     <select id="sana-cosmos-existing" disabled={locked} value={selected} onChange={e=>{setSelected(e.target.value);resetSuggestion()}}>
      <option value="">{say('مشروع جديد','A new project')}</option>{projects.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}
     </select>
    </div>
    <p className="sana-cosmos-entry-hint">{selected
      ?say('ستنتقل الفكرة إلى مساعد سنا داخل مشروعك الحالي.','Your idea will open in SANA inside your existing project.')
      :say('سنا تقترح التصور الأنسب، ولا يبدأ إنشاء المشروع إلا بعد موافقتك.','SANA suggests a direction first. Your project is created only after you approve.')}</p>
    <button type="submit" className="sana-cosmos-primary" disabled={working||!idea.trim()}>
     {stage==='classifying'?<><span className="sana-cosmos-button-spark"/>{say('سنا تستكشف فكرتك...','SANA is exploring your idea...')}</>
      :<><Sparkles size={18}/>{selected?say('تابع داخل المشروع','Continue in project'):say('أطلق شرارة الفكرة','Ignite your idea')}<ArrowUpLeft size={17}/></>}
    </button>
   </form>}
   {stage==='classifying'&&<div className="sana-cosmos-working" role="status" aria-live="polite">
    <div className="sana-cosmos-working-orbit" aria-hidden="true"><span>✦</span></div>
    <strong>{say('سنا تتعرف على اتجاه الفكرة...','SANA is discovering the direction of your idea...')}</strong>
    <small>{say('سيظهر التصور المقترح هنا عند اكتماله.','Your suggested vision will appear here when it is ready.')}</small>
   </div>}
   {suggestion&&choice&&<section className="sana-cosmos-proposal" aria-live="polite" aria-label={say('تصور المشروع المقترح','Suggested project vision')}>
    <div className="sana-cosmos-proposal-head"><span className="sana-cosmos-proposal-mark"><Sparkles size={22}/></span><div><span className="sana-cosmos-kicker">SANA · PROJECT VISION</span><h2>{say('تصور فكرتك جاهز','Your vision is ready')}</h2><p>{say('راجع الاتجاه الذي تقترحه سنا. لا يبدأ الإنشاء إلا بعد الموافقة.','Review SANA’s suggested direction. Creation begins only after approval.')}</p></div></div>
    <div className="sana-cosmos-proposal-name"><small>{say('اسم المشروع المقترح','Suggested project name')}</small><input aria-label={say('اسم المشروع','Project name')} value={choice.name||''} maxLength={100} disabled={locked} onChange={e=>setChoice(c=>({...c,name:e.target.value}))}/><span className="sana-cosmos-name-hint">{say('يمكن تعديل الاسم قبل البدء','You can change the name before starting')}</span></div>
    {changing&&!locked?<div className="sana-cosmos-proposal-edit">
      <label>{say('نوع المشروع','Project type')}<select value={choice.domain} onChange={e=>changeDomain(e.target.value)}>{domains.map(d=><option key={d} value={d}>{t(d)}</option>)}</select></label>
      <label>{say('طريقة تنظيمه','How it will be organized')}<select value={choice.diagramType} onChange={e=>setChoice(c=>({...c,diagramType:e.target.value}))}>{(options[choice.domain]||[]).map(type=><option key={type} value={type}>{t(`diagram_${type}`)}</option>)}</select></label>
    </div>:<div className="sana-cosmos-proposal-grid"><div><Layers3 size={21}/><small>{say('مجال المشروع','Project field')}</small><strong>{t(choice.domain)}</strong></div><div><Network size={21}/><small>{say('التصور المناسب','Suggested structure')}</small><strong>{t(`diagram_${choice.diagramType}`)}</strong></div></div>}
    {suggestion.reason&&<div className="sana-cosmos-reason"><Lightbulb size={20}/><div><strong>{say('لماذا هذا التصور؟','Why this direction?')}</strong><p>{suggestion.reason}</p></div></div>}
    {stage==='suggested'&&<div className="sana-cosmos-proposal-actions">
     <button type="button" className="sana-cosmos-secondary" onClick={()=>setChanging(v=>!v)}><Edit3 size={17}/>{changing?say('تم التعديل','Done'):say('تعديل التصور','Adjust the vision')}</button>
     <button type="button" className="sana-cosmos-primary" onClick={accept}><Sparkles size={18}/>{say('اعتماد وبدء التكوين','Approve and create')}<ArrowUpLeft size={16}/></button>
    </div>}
    {['creating','drawing','ready'].includes(stage)&&<div className={`sana-cosmos-formation ${stage==='ready'?'is-ready':''}`} role="status" aria-live="polite">
      <div className="sana-cosmos-formation-symbol" aria-hidden="true"><span>✦</span></div>
      <div><strong>{stage==='creating'?say('بدأ تكوين مساحة مشروعك','Creating your project space')
       :stage==='ready'?say('عالمك الجديد أصبح جاهزًا!','Your new world is ready!')
       :fixing?say('سنا تعيد ترتيب التفاصيل لتكتمل الفكرة','SANA is refining the details')
       :say('سنا تربط عناصر فكرتك ببعضها','SANA is connecting the elements of your idea')}</strong>
       <p>{stage==='ready'?say('يجري فتح مساحة العمل الآن...','Opening your workspace now...')
        :say('ستنتقل إلى مساحة العمل عندما يكتمل التصميم ويُحفظ.','Your workspace will open after the design is complete and saved.')}</p></div>
    </div>}
    {stage==='clarify'&&clarification&&<div className="sana-cosmos-clarification"><div className="sana-cosmos-question-title"><MessageCircle size={21}/><strong>{say('سنا تحتاج تفصيلًا واحدًا','SANA needs one more detail')}</strong></div>
      {clarification.message&&<p>{clarification.message}</p>}
      <h3>{clarification.question}</h3>
      {clarification.answers.length>0&&<div className="sana-cosmos-quick-answers">{clarification.answers.map((text,i)=><button type="button" key={i} onClick={()=>answerClarification(text)}>{text}</button>)}</div>}
      <form onSubmit={e=>{e.preventDefault();answerClarification(answer)}}><label htmlFor="sana-cosmos-answer">{say('أو اكتب إجابتك بطريقتك','Or write your own answer')}</label><div><input id="sana-cosmos-answer" value={answer} onChange={e=>setAnswer(e.target.value)} placeholder={say('اكتب التوضيح هنا...','Your answer...')} maxLength={1500}/><button type="submit" className="sana-cosmos-primary" disabled={!answer.trim()}><Send size={17}/>{say('متابعة الفكرة','Continue')}</button></div></form>
    </div>}
    {stage==='advice'&&<div className="sana-cosmos-clarification"><div className="sana-cosmos-question-title"><MessageCircle size={21}/><strong>{say('رد سنا على فكرتك','SANA’s response')}</strong></div><p>{advice}</p><div className="sana-cosmos-proposal-actions"><button type="button" className="sana-cosmos-secondary" onClick={draw}><RotateCcw size={17}/>{say('إعادة المحاولة','Try again')}</button><button type="button" className="sana-cosmos-primary" onClick={openProject}><FolderOpen size={17}/>{say('فتح المشروع','Open project')}</button></div></div>}
    {stage==='failed'&&<div className="sana-cosmos-proposal-actions"><button type="button" className="sana-cosmos-secondary" onClick={openProject}><FolderOpen size={17}/>{say('فتح المشروع','Open project')}</button><button type="button" className="sana-cosmos-primary" onClick={draw}><RotateCcw size={17}/>{say('إعادة المحاولة','Try again')}</button></div>}
   </section>}
   {error&&<div role="alert" className="sana-cosmos-error"><Info size={17}/><span>{error}</span></div>}
   <div className="sana-cosmos-bottom"><span>{say('✦ كل فكرة تستحق فرصة للتألق','✦ Every idea deserves to shine')}</span><button type="button" onClick={onTemplates}><LayoutTemplate size={17}/>{say('أو ابدأ بتصميم يدوي','Or start designing manually')}</button></div>
  </div>
 </section>;
}
function AISidebar({t,lang,onClose}){return <aside className="ai-sidebar"><div className="ai-side-head"><div className="ai-gem"><Sparkles size={23}/></div><div><strong>{t('ai')}</strong><span>{t('aiSoon')}</span></div><IconButton onClick={onClose} label={t('close')}><X size={18}/></IconButton></div><div className="ai-body"><span className="ai-avatar"><img src="/sana-logo.webp" alt="SANA" /></span><div className="ai-message">{lang==='ar'?'مرحبًا! أنا سنا، مساعدك الإبداعي. ✨':'Hello! I’m SANA, your creative companion. ✨'}<br/><br/>{t('aiNotice')}</div><div className="ai-ideas"><span>{lang==='ar'?'قريبًا يمكنك أن تطلب:':'Soon you can ask:'}</span>{(lang==='ar'?['طوّر لي هذه الشخصية','هل في قصتي تعارض؟','اقترح حدثًا جديدًا']:['Develop this character','Find plot contradictions','Suggest a new event']).map(text=><div key={text}><Sparkles size={13}/>{text}</div>)}</div></div><div className="ai-bottom"><div className="fake-input"><LockKeyhole size={16}/>{t('aiPlaceholder')}</div><small>{t('aiSoon')}</small></div></aside>}
function ElementTile({element,t,onEdit,onDelete,onFocus}){const Icon=ICONS[element.type]||FileText;return <article className="element-tile"><div className={`element-tile-icon ${COLORS[element.type]}`}><Icon size={20}/></div><div className="element-tile-main"><small>{t(element.type)}</small><h3>{element.name}</h3><p>{element.description||'—'}</p></div><div className="element-tools"><IconButton label={t('edit')} onClick={()=>onEdit(element)}><Edit3 size={15}/></IconButton><IconButton label={t('viewMap')} onClick={()=>onFocus(element.id)}><Network size={15}/></IconButton><IconButton label={t('delete')} onClick={()=>onDelete(element)}><Trash2 size={15}/></IconButton></div></article>}
// Keep story edges attached to the visible card outlines after the Creativity redesign.
const SANA_STORY_NODE_WIDTH=246, SANA_STORY_NODE_HEIGHT=176;
function creativeEdgeEndpoints(a,b,width=SANA_STORY_NODE_WIDTH,height=SANA_STORY_NODE_HEIGHT){
 const cx=a.x+width/2,cy=a.y+height/2,tx=b.x+width/2,ty=b.y+height/2;
 const dx=tx-cx,dy=ty-cy;
 if(!dx&&!dy)return {x1:cx,y1:cy,x2:tx,y2:ty};
 const k=Math.min((width/2)/Math.max(Math.abs(dx),.001),(height/2)/Math.max(Math.abs(dy),.001));
 return {x1:cx+dx*k,y1:cy+dy*k,x2:tx-dx*k,y2:ty-dy*k};
}
function MapView({t,lang,project,onSelect,onMove,onAdd,onAddRelation,onDeleteRelation,initialFocus}){
 const [filter,setFilter]=useState('all'),[focus,setFocus]=useState(initialFocus??null),[zoom,setZoom]=useState(1),[positions,setPositions]=useState({}),[connectionPoint,setConnectionPoint]=useState(null);
 const dragging=useRef(null);const pan=useRef(null),scrollRef=useRef(null),[connection,setConnection]=useState(null),[selectedEdge,setSelectedEdge]=useState(null),[organized,setOrganized]=useState(false),[areas,setAreas]=useState([]),[layoutBusy,setLayoutBusy]=useState(false),[layoutError,setLayoutError]=useState('');const stageRef=useRef(null);const selection=useMemo(()=>new Set(!focus?project.elements.map(e=>e.id):[focus,...project.relations.filter(r=>r.source===focus||r.target===focus).flatMap(r=>[r.source,r.target])]),[focus,project]);
 const availableTypes=elementTypes(project);const nodes=project.elements.filter(e=>(filter==='all'||e.type===filter)&&selection.has(e.id));const nodeIds=new Set(nodes.map(e=>e.id));
 const coord=(e)=>({x:positions[e.id]?.x??e.x??120,y:positions[e.id]?.y??e.y??120});
 const techDiagram=['ARCHITECTURE','ERD','CLASS_DIAGRAM','USE_CASE'].includes(project.activeType);
 const creativeDiagram=['writing','games'].includes(project.domain)&&['STORY_MAP','CREATIVITY'].includes(project.activeType);
 const techName=type=>techDiagram?(TECH_NODE_LABELS[type]||t(type)):t(type);
 const W=Math.max(techDiagram?1750:1600,...areas.map(a=>a.x+a.width+450),...project.elements.map(e=>coord(e).x+650)),H=Math.max(1050,...areas.map(a=>a.y+a.height+320),...project.elements.map(e=>coord(e).y+(techDiagram?techNodeBounds(e,project.activeType,project.relations).height+350:530)));
 const caseNodes=project.elements.filter(e=>e.type==='use_case');
 const useCaseBoundary=caseNodes.length?(()=>{const left=Math.max(20,Math.min(...caseNodes.map(e=>coord(e).x))-34),top=Math.max(24,Math.min(...caseNodes.map(e=>coord(e).y))-60);return {left,top,width:Math.min(W-left-25,Math.max(340,Math.max(...caseNodes.map(e=>coord(e).x+255))-left+30)),height:Math.min(H-top-15,Math.max(230,Math.max(...caseNodes.map(e=>coord(e).y+166))-top+26))}})():{left:310,top:32,width:800,height:525};
 const organize=()=>{setLayoutError('');const result=organizedGraph(project);setPositions(result.positions);setAreas(result.areas);setOrganized(true);setFocus(null);setFilter('all');const box=scrollRef.current;if(box)box.scrollTo({left:0,top:0,behavior:'smooth'})};
 const saveLayout=async()=>{setLayoutBusy(true);setLayoutError('');try{for(const n of project.elements){const p=positions[n.id];if(p)await onMove(n.id,p.x,p.y)}setOrganized(false)}catch(e){setLayoutError(e.message)}finally{setLayoutBusy(false)}};
 function pointerDown(e,el){if(e.button!==0||e.target.closest('button'))return;dragging.current={id:el.id,startX:e.clientX,startY:e.clientY,x:coord(el).x,y:coord(el).y,moved:false};e.currentTarget.setPointerCapture(e.pointerId)}
 function pointerMove(e){const d=dragging.current;if(!d)return;let deltaX=(e.clientX-d.startX)/zoom,deltaY=(e.clientY-d.startY)/zoom;if(Math.abs(deltaX)+Math.abs(deltaY)>4)d.moved=true;if(d.moved)setPositions(p=>({...p,[d.id]:{x:Math.max(12,Math.round(d.x+deltaX)),y:Math.max(12,Math.round(d.y+deltaY))}}))}
 async function pointerUp(e,el){const d=dragging.current;if(!d||d.id!==el.id)return;dragging.current=null;if(d.moved){const pos={x:Math.max(12,Math.round(d.x+(e.clientX-d.startX)/zoom)),y:Math.max(12,Math.round(d.y+(e.clientY-d.startY)/zoom))};try{await onMove(el.id,pos.x,pos.y)}catch{setPositions(p=>{const next={...p};delete next[el.id];return next})} }else onSelect(el)}
 const changeZoom=(next,clientX,clientY)=>{const box=scrollRef.current;const limited=Math.max(.2,Math.min(2,Math.round(next*100)/100));if(!box){setZoom(limited);return}const r=box.getBoundingClientRect(),px=(clientX??(r.left+r.width/2))-r.left,py=(clientY??(r.top+r.height/2))-r.top;const originalX=(box.scrollLeft+px)/zoom,originalY=(box.scrollTop+py)/zoom;setZoom(limited);requestAnimationFrame(()=>{box.scrollLeft=originalX*limited-px;box.scrollTop=originalY*limited-py})};
 const wheelZoom=e=>{if(!(e.ctrlKey||e.metaKey||e.altKey))return;e.preventDefault();const factor=e.deltaY>0?.91:1.09;changeZoom(zoom*factor,e.clientX,e.clientY)};
 const dropTool=(e)=>{e.preventDefault();const type=e.dataTransfer.getData('text/x-sana-node');if(!availableTypes.includes(type))return;const rect=stageRef.current?.getBoundingClientRect();if(!rect)return;onAdd(type,{x:Math.max(12,Math.round((e.clientX-rect.left)/zoom-78)),y:Math.max(12,Math.round((e.clientY-rect.top)/zoom-45))});};
 return <div className={`map-wrapper spark-graph-${graphPresentation[project.activeType]||'story'} spark-domain-${project.domain||'writing'}`}><div className="canvas-tool-shelf"><div className="shelf-label"><GripVertical size={17}/><span>{t('canvasHint')}</span></div><div className="canvas-tools">{availableTypes.map(type=>{const Icon=ICONS[type]||Plus;return <button key={type} type="button" className={`canvas-tool ${COLORS[type]||'sage'}`} draggable onDragStart={e=>{e.dataTransfer.setData('text/x-sana-node',type);e.dataTransfer.effectAllowed='copy'}} onClick={()=>onAdd(type)} title={techName(type)}><Icon size={18}/><span>{techName(type)}</span><GripVertical size={13} className="grip"/></button>})}</div></div><div className="map-controls"><div className="map-hint"><MousePointer2 size={16}/>{connection?(techDiagram?'Connect nodes':t('connectHandle')):(lang==='ar'?'اسحب العقد لتحريكها، واسحب الخلفية للتنقل · Ctrl + عجلة الماوس للتكبير': 'Drag nodes or the background to move · Ctrl + mouse wheel to zoom')}{connection&&<button onClick={()=>setConnection(null)}>{t('cancel')}</button>}</div><div className="map-control-actions"><button className="map-text-button" disabled={layoutBusy} onClick={organize}>{techDiagram?'Preview layout':lang==='ar'?'معاينة ترتيب':'Preview layout'}</button>{organized&&<><button className="map-text-button" disabled={layoutBusy} onClick={saveLayout}>{t('save')}</button><button className="map-text-button" disabled={layoutBusy} onClick={()=>{setPositions({});setAreas([]);setOrganized(false)}}>{t('cancel')}</button></>}<select value={filter} onChange={e=>setFilter(e.target.value)} aria-label={t('filter')}><option value="all">{techDiagram?'All types':t('all')}</option>{availableTypes.map(k=><option key={k} value={k}>{techName(k)}</option>)}</select>{focus&&<button className="map-text-button" onClick={()=>setFocus(null)}>{t('showAll')}</button>}<IconButton label={t('fitView')} onClick={()=>{setFocus(null);setFilter('all');const box=scrollRef.current;if(!box)return;const all=project.elements;const minX=all.length?Math.max(0,Math.min(...all.map(n=>coord(n).x))-70):0,minY=all.length?Math.max(0,Math.min(...all.map(n=>coord(n).y))-70):0,maxX=Math.max(450,...all.map(n=>coord(n).x+300)),maxY=Math.max(320,...all.map(n=>coord(n).y+(techDiagram?techNodeBounds(n,project.activeType,project.relations).height+55:200)));const next=Math.max(.2,Math.min(1.6,Math.min(box.clientWidth/(maxX-minX+90),box.clientHeight/(maxY-minY+90))*.94));setZoom(next);requestAnimationFrame(()=>box.scrollTo({left:minX*next,top:minY*next,behavior:'smooth'}))}}><Network size={17}/></IconButton><IconButton label={t('resetMap')} onClick={()=>{setFocus(null);setFilter('all');setZoom(1);scrollRef.current?.scrollTo({left:0,top:0,behavior:'smooth'})}}><RotateCcw size={16}/></IconButton><IconButton label="Zoom out" disabled={zoom<=0.2} onClick={()=>changeZoom(zoom-.1)}><ZoomOut size={17}/></IconButton><span className="zoom-label">{Math.round(zoom*100)}%</span><IconButton label="Zoom in" disabled={zoom>=2} onClick={()=>changeZoom(zoom+.1)}><ZoomIn size={17}/></IconButton></div></div>{layoutError&&<p role="alert" className="ux-error">{layoutError}</p>}{selectedEdge&&<div className="spark-edge-inspector"><strong>{selectedEdge.label||selectedEdge.type}</strong><span>{Object.entries(selectedEdge.properties||{}).filter(([k])=>k!=='label').map(([k,v])=>k+': '+String(v)).join(' · ')}</span>{!project.storyMap&&<button onClick={()=>onAddRelation(selectedEdge)}>{t('edit')}</button>}<button onClick={()=>onDeleteRelation(selectedEdge)}>{t('delete')}</button><button onClick={()=>setSelectedEdge(null)}>{t('close')}</button></div>}<div ref={scrollRef} className="map-scroll sana-free-canvas" onWheel={wheelZoom} onPointerDown={e=>{if(e.button===1||(e.button===0&&!e.target.closest('.map-node,button,text,path'))){e.preventDefault();pan.current={x:e.clientX,y:e.clientY,left:e.currentTarget.scrollLeft,top:e.currentTarget.scrollTop};e.currentTarget.setPointerCapture(e.pointerId)}}} onPointerMove={e=>{if(pan.current){e.currentTarget.scrollLeft=pan.current.left-(e.clientX-pan.current.x);e.currentTarget.scrollTop=pan.current.top-(e.clientY-pan.current.y)}else if(connection&&stageRef.current){const r=stageRef.current.getBoundingClientRect();setConnectionPoint({x:(e.clientX-r.left)/zoom,y:(e.clientY-r.top)/zoom})}}} onPointerUp={()=>{pan.current=null;if(connection)setConnection(null);setConnectionPoint(null)}} onPointerCancel={()=>{pan.current=null;setConnection(null);setConnectionPoint(null)}} onDragOver={e=>{e.preventDefault();e.dataTransfer.dropEffect="copy"}} onDrop={dropTool}><div style={{width:W*zoom,height:H*zoom,position:'relative'}}><div ref={stageRef} className="map-stage" style={{width:W,height:H,transform:`scale(${zoom})`,transformOrigin:'top left'}}><div className="map-grain"/>{project.activeType==='USE_CASE'&&<div className="spark-usecase-boundary" style={useCaseBoundary}><span>{project.graphProperties?.systemName||project.diagrams?.find(d=>d.id===project.activeOutputId)?.title}</span></div>}{areas.map(area=><div key={area.type} className={`spark-canvas-area spark-area-${area.type}`} style={{left:area.x,top:area.y,width:area.width,height:area.height}}><strong>{area.title?(lang==='ar'?area.title.ar:area.title.en):t(area.type)}</strong></div>)}{nodes.length===0&&<div className="canvas-empty-message"><Sparkles size={29}/><strong>{t('canvasBlank')}</strong></div>}<svg className="map-lines" width={W} height={H} viewBox={`0 0 ${W} ${H}`}><defs><marker id="map-arrow" markerWidth="7" markerHeight="7" refX="6" refY="3.5" orient="auto"><path d="M0 0L7 3.5L0 7" fill="none" stroke="#b79d6b" strokeWidth="1.1"/></marker><marker id="spark-triangle" markerWidth="12" markerHeight="12" refX="11" refY="6" orient="auto"><path d="M1 1L11 6L1 11Z" fill="#fffdf9" stroke="#65898e"/></marker><marker id="spark-diamond" markerWidth="16" markerHeight="12" refX="15" refY="6" orient="auto"><path d="M1 6L8 1L15 6L8 11Z" fill="#65898e" stroke="#65898e"/></marker><marker id="spark-diamond-open" markerWidth="16" markerHeight="12" refX="15" refY="6" orient="auto"><path d="M1 6L8 1L15 6L8 11Z" fill="#fffdf9" stroke="#65898e"/></marker><marker id="sana-erd-many" markerWidth="17" markerHeight="17" refX="13" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 2 8 L 13 2 M 2 8 L 13 8 M 2 8 L 13 14" stroke="#497b87" strokeWidth="1.7" fill="none"/></marker><marker id="sana-erd-one" markerWidth="12" markerHeight="16" refX="9" refY="8" orient="auto" markerUnits="userSpaceOnUse"><path d="M 9 1 L 9 15" stroke="#497b87" strokeWidth="1.8"/></marker></defs>{project.relations.filter(r=>nodeIds.has(r.source)&&nodeIds.has(r.target)).map(rel=>{const a=project.elements.find(e=>e.id===rel.source),b=project.elements.find(e=>e.id===rel.target);if(!a||!b)return null;const p=coord(a),q=coord(b);const {x1,y1,x2,y2}=techDiagram?techEdgeEndpoints(p,q,a,b,project.activeType,project.relations):creativeDiagram?creativeEdgeEndpoints(p,q):edgeEndpoints(p,q,220,120);return <g key={rel.id}><path className={`spark-edge spark-edge-${rel.type}`} d={`M${x1} ${y1} C${x1+(x2-x1)*.48} ${y1},${x1+(x2-x1)*.48} ${y2},${x2} ${y2}`} stroke="#c9b58b" strokeWidth="1.6" fill="none" markerStart={project.activeType==='ERD'?(rel.type==='MANY_TO_MANY'?'url(#sana-erd-many)':'url(#sana-erd-one)'):undefined} markerEnd={project.activeType==='ERD'?(rel.type==='ONE_TO_ONE'?'url(#sana-erd-one)':'url(#sana-erd-many)'):['INHERITANCE','IMPLEMENTATION','GENERALIZATION'].includes(rel.type)?'url(#spark-triangle)':rel.type==='COMPOSITION'?'url(#spark-diamond)':rel.type==='AGGREGATION'?'url(#spark-diamond-open)':['ASSOCIATION','ONE_TO_ONE','ONE_TO_MANY','MANY_TO_MANY'].includes(rel.type)?undefined:'url(#map-arrow)'}/><path className="spark-edge-hit" d={`M${x1} ${y1} C${x1+(x2-x1)*.48} ${y1},${x1+(x2-x1)*.48} ${y2},${x2} ${y2}`} fill="none" stroke="transparent" strokeWidth="16" onClick={()=>setSelectedEdge(rel)}/><text x={(x1+x2)/2} y={(y1+y2)/2-9} className="map-edge-text" textAnchor="middle">{rel.label||(techDiagram?rel.type.replaceAll('_',' '):t(`relation_${rel.type}`))}{rel.properties?.protocol?` · ${rel.properties.protocol}`:''}{rel.properties?.multiplicity?` · ${rel.properties.multiplicity}`:''}</text>{project.activeType!=='ERD'&&['ONE_TO_ONE','ONE_TO_MANY','MANY_TO_MANY'].includes(rel.type)&&<><text x={x1+20} y={y1-12} className="map-edge-text">{rel.type==='MANY_TO_MANY'?'N':'1'}</text><text x={x2-20} y={y2-12} className="map-edge-text">{rel.type==='ONE_TO_ONE'?'1':'N'}</text></>}</g>})}{connection&&connectionPoint&&(()=>{const source=project.elements.find(n=>String(n.id)===String(connection));if(!source)return null;const start=coord(source),sx=techDiagram?start.x+(source.type==='actor'&&project.activeType==='USE_CASE'?112:techNodeBounds(source,project.activeType,project.relations).width):creativeDiagram?start.x+SANA_STORY_NODE_WIDTH:start.x+210,sy=techDiagram?start.y+(source.type==='actor'&&project.activeType==='USE_CASE'?68:techNodeBounds(source,project.activeType,project.relations).height/2):creativeDiagram?start.y+SANA_STORY_NODE_HEIGHT/2:start.y+57;return <g className="spark-live-connection" aria-hidden="true"><path d={`M${sx} ${sy} Q${(sx+connectionPoint.x)/2} ${(sy+connectionPoint.y)/2-35} ${connectionPoint.x} ${connectionPoint.y}`} fill="none" stroke="#c29a52" strokeWidth="2.8" strokeDasharray="8 5"/><circle cx={connectionPoint.x} cy={connectionPoint.y} r="7" fill="#d8aa5c"/></g>})()}</svg>{nodes.map(el=>{const Icon=ICONS[el.type]||FileText,p=coord(el);return <div key={el.id} className={`map-node spark-node-${el.type} ${COLORS[el.type]||'sage'} ${dragging.current?.id===el.id?'dragging':''} ${connection&&connection!==el.id?'spark-connection-target':''}`} data-node-id={el.id} tabIndex={0} role="group" aria-label={el.name} onKeyDown={e=>{if(e.target===e.currentTarget&&e.key==='Enter'){e.preventDefault();onSelect(el)}}} style={{left:p.x,top:p.y,...characterColorStyle(project.id,el)}} onPointerDown={e=>pointerDown(e,el)} onPointerMove={pointerMove} onPointerUp={e=>{if(connection){e.stopPropagation();if(String(connection)!==String(el.id))onAddRelation({source:connection,target:el.id});setConnection(null)}else pointerUp(e,el)}} onPointerCancel={()=>{dragging.current=null}}><span className="map-node-icon"><Icon size={19}/></span><DiagramNodeContent element={el} diagramType={project.activeType} t={t} lang={lang} nodes={project.elements} relations={project.relations}/><button type="button" className="spark-connect-handle" title={t('connectHandle')} aria-label={t('connectHandle')} onClick={e=>e.stopPropagation()} onPointerDown={e=>{e.stopPropagation();setConnectionPoint(null);setConnection(el.id)}} onPointerUp={e=>{e.stopPropagation();const target=document.elementFromPoint(e.clientX,e.clientY)?.closest('[data-node-id]');if(target&&target.dataset.nodeId!==String(el.id))onAddRelation({source:el.id,target:target.dataset.nodeId});setConnection(null)}}>+</button></div>})}</div></div></div>{project.relations?.length>0&&<div className="map-relation-list"><strong>{t('relations')} · {project.relations.length}</strong><div>{project.relations.map(r=>{const a=project.elements.find(e=>e.id===r.source),b=project.elements.find(e=>e.id===r.target);return <span className="relation-chip" key={r.id}><Link2 size={12}/><span>{a?.name||'?'} — {r.label||r.type} — {b?.name||'?'}</span><button onClick={()=>onDeleteRelation(r)} aria-label={t('delete')} title={t('delete')}><X size={13}/></button></span>})}</div></div>}
<footer className="map-footer">{project.domain==='technology'?<><span><i className="legend-dot sage"/>{lang==='ar'?'مكوّنات وخدمات':'Components & services'}</span><span><i className="legend-dot sky"/>{lang==='ar'?'بيانات وأنظمة':'Data & systems'}</span><span><i className="legend-dot amber"/>{lang==='ar'?'علاقات تقنية':'Technical relationships'}</span></>:<><span><i className="legend-dot sage"/>{t('characters')}</span><span><i className="legend-dot sky"/>{t('worlds')}</span><span><i className="legend-dot amber"/>{t('locations')}</span><span><i className="legend-dot coral"/>{t('events')}</span><span><i className="legend-dot violet"/>{t('rules')}</span></>}<button onClick={onAddRelation}><Link2 size={16}/>{t('newRelation')}</button></footer></div>
}
// Picks which diagram of the project the map shows, or adds a new one (API mode)
function DiagramBar({t,project,onDiagram,onNewDiagram}){
 const [newType,setNewType]=useState(project.diagramTypes?.[0]||'');
 const shown=(project.diagrams||[]).filter(d=>d.outputType!=='BRANCHING_STORY');
 return <div className="diagram-bar" style={{display:'flex',gap:8,alignItems:'center',flexWrap:'wrap'}}>
  <select value={project.activeOutputId??''} disabled={!shown.length} onChange={e=>onDiagram(Number(e.target.value))} aria-label={t('diagram')}>{shown.map(d=><option key={d.id} value={d.id}>{d.title}</option>)}</select>
  <select value={newType} onChange={e=>setNewType(e.target.value)} aria-label={t('newDiagram')}>{(project.diagramTypes||[]).map(type=><option key={type} value={type}>{project.domain==='technology'?(TECH_DIAGRAM_LABELS[type]||type):t(`diagram_${type}`)}</option>)}</select>
  <Btn variant="soft" disabled={!project.activeOutputId} onClick={async()=>{const title=window.prompt(t('renameDiagram'),project.diagrams?.find(d=>d.id===project.activeOutputId)?.title||'');if(title?.trim())try{await repository.renameDiagram(project.id,title.trim());onDiagram(project.activeOutputId)}catch(e){window.alert(e.message)}}}>{t('renameDiagram')}</Btn><Btn variant="ghost" disabled={!project.activeOutputId} onClick={async()=>{if(window.confirm(t('deleteDiagram')))try{await repository.deleteDiagram(project.id);onDiagram(null)}catch(e){window.alert(e.message)}}}>{t('deleteDiagram')}</Btn><Btn variant="soft" icon={Plus} disabled={!newType} onClick={()=>onNewDiagram(`${project.name} · ${t(`diagram_${newType}`)}`,newType)}>{t('newDiagram')}</Btn>
 </div>
}
// Requests for a brand-new character must go through STORY_MAP approval before
// the branching editor creates scenes. A name appearing in dialogue is NOT a character profile.
function requestsNewStoryCharacter(value){
 const text=String(value||'').toLowerCase().replace(/[إأآ]/g,'ا').replace(/ة/g,'ه').replace(/[\u064b-\u065f]/g,'').replace(/\s+/g,' ');
 if(/بدون شخصيه جديده|لا (?:تضيف|تنشئ|تخترع) شخصيه|without (?:a )?new character|no new character/.test(text))return false;
 return /شخصيه.{0,22}جديده|شخصيه.{0,22}(?:غير موجوده|مو موجوده)|(?:اضيف|ضيف|انشئ|اصنع|ابتكر).{0,28}شخصيه|(?:new character|create a character|add a character|introduce a character|create new npc|add new npc)/i.test(text);
}
function characterOnlyRequest(value){
 const text=String(value||'').toLowerCase().replace(/[إأآ]/g,'ا').replace(/ة/g,'ه');
 return requestsNewStoryCharacter(text)&&!/نهايه|حوار|مشهد|شجره|قصه|ending|dialogue|scene|branch|story/i.test(text);
}
// SANA AI panel (API mode): advice on the open diagram, an edit of it, or a new diagram. Proposals are applied only on Accept.
function AIPanel({t,lang,project,onClose,onApplied,onMoodboardSaved,onNewCharacter,branchingMode=false}){
 const existingBranch=project.diagrams?.find(d=>d.outputType==='BRANCHING_STORY');
 const [task,setTask]=useState(branchingMode?(existingBranch?'EDIT':'CREATE'):sessionStorage.getItem(`sana-ai-seed-${project.id}`)?'CREATE':project.activeOutputId?'ADVISE':'CREATE'),[diagramType,setDiagramType]=useState(branchingMode?'BRANCHING_STORY':project.diagramTypes?.[0]||project.activeType),[prompt,setPrompt]=useState(()=>branchingMode?sessionStorage.getItem(`sana-branching-pending-${project.id}`)||'':sessionStorage.getItem(`sana-ai-seed-${project.id}`)||''),[messages,setMessages]=useState([]),[proposal,setProposal]=useState(null),[busy,setBusy]=useState(false);
 const [moodboardPreview,setMoodboardPreview]=useState(null),[moodboardQuestion,setMoodboardQuestion]=useState(null),[moodboardError,setMoodboardError]=useState('');
 // New story characters are AI-proposed on STORY_MAP, reviewed independently,
 // and are never auto-written by the branching-story AI.
 const [characterDraft,setCharacterDraft]=useState(false);
 const [pendingCharacterIdea,setPendingCharacterIdea]=useState('');
 const approvedCharacterRequest=useRef(null);
 useEffect(()=>{if(branchingMode){setDiagramType('BRANCHING_STORY');setTask(project.diagrams?.some(d=>d.outputType==='BRANCHING_STORY')?'EDIT':'CREATE')}},[branchingMode,project.id]);
 const moodText=(ar,en)=>lang==='ar'?ar:en;
 const alive=useRef(true);useEffect(()=>{alive.current=true;return()=>{alive.current=false}},[]);
 const say=(role,text,extra={})=>setMessages(m=>[...m,{role,text:role==='error'?publicError(text,lang):text,...extra}]);
 async function previewMoodboard(text){
  setMoodboardQuestion(null);
  if(moodboardPreview){say('notice',moodText('راجعي المسودة الحالية ووافقي عليها أو ارفضيها أولًا.','Please approve or reject the current moodboard draft first.'));return;}
  if(!API_MODE){say('notice',moodText('إنشاء لوحة الإلهام غير متاح الآن. حاول مجددًا لاحقًا.','Creating moodboards is unavailable right now. Please try again later.'));return;}
  setBusy(true);setMoodboardError('');
  try{
   const pending=await moodboardAi.generate(project.id,text,lang);
   if(alive.current)say('notice',moodText('سنا تجهّز لوحة الإلهام. ستظهر المعاينة قبل حفظ أي شيء.', 'SANA is preparing your moodboard. You can review it before saving.'));
   let result=pending;
   for(let i=0;i<180&&alive.current&&result.status==='GENERATING';i++){
    await new Promise(r=>setTimeout(r,2000));
    if(!alive.current)return;
    result=await moodboardAi.get(project.id,pending.draftId);
   }
   if(!alive.current)return;
   if(result.status==='GENERATING')throw new Error(moodText('استغرق إعداد اللوحة وقتًا أطول من المتوقع. حاول مرة أخرى.', 'Preparing your moodboard took longer than expected. Please try again.'));
   if(result.status!=='PENDING')throw new Error(moodText('لم يصل مقترح مودبورد جاهز.', 'No reviewable moodboard draft was returned.'));
   setMoodboardPreview(result);
  }catch(e){if(alive.current)say('error',e.message);}
  finally{if(alive.current)setBusy(false);}
 }
 async function decideMoodboard(accept){
  if(!moodboardPreview||busy)return;
  setBusy(true);setMoodboardError('');
  try{
   if(accept){const saved=await moodboardAi.approve(project.id,moodboardPreview.draftId);if(!alive.current)return;setMoodboardPreview(null);say('notice',moodText('حُفظت لوحة الإلهام. يمكنك تعديلها في صفحة اللوحة.','Moodboard saved. You can edit it in the Moodboard page.'));await onMoodboardSaved(saved.boardId);}
   else{await moodboardAi.reject(project.id,moodboardPreview.draftId);if(alive.current){setMoodboardPreview(null);say('notice',moodText('رُفضت المسودة؛ لوحاتك الحالية لم تتغير.','Draft rejected; your existing boards were not changed.'));}}
  }catch(e){if(alive.current)setMoodboardError(e.message);}
  finally{if(alive.current)setBusy(false);}
 }
 // Chat messages for a finished run; the same shape say() produces
 const runMessages=(run,sourcePrompt=run.prompt)=>{
  if(run.status==='FAILED'||run.status==='TIMED_OUT')return [{role:'error',text:publicError(run.error?.message||moodText('تعذر إكمال الطلب، حاول مرة أخرى.','Could not complete the request. Please try again.'),lang)}];
  if(run.status!=='COMPLETED'||!run.result)return [{role:'error',text:publicError(t('aiStillRunning'),lang)}];
  const result=run.result;
  if(result.type==='CLARIFICATION'){
   // Branching stories: a question about adding a character gets an approval action (sourcePrompt is what the user asked)
   const clarificationText=[result.message,result.clarification?.question,...(result.clarification?.suggestedAnswers||[])].join(' ');
   const needsCharacter=branchingMode&&/شخصي|character/i.test(clarificationText)&&/إضا[فف]|أنش|اصنع|موافق|إذن|approval|add|creat|save/i.test(clarificationText);
   return [{role:'ai',text:`${result.message}\n\n${result.clarification?.question||''}`,answers:result.clarification?.suggestedAnswers||[],kind:'CLARIFICATION',characterApproval:needsCharacter,sourcePrompt}];
  }
  return [{role:'ai',text:result.message,kind:result.type}];
 };
 // Rebuild the chat from the saved AI requests, so a page reload does not lose it.
 // A pending proposal comes back with Accept/Reject, and a request that is still running is followed again.
 useEffect(()=>{
  if(!API_MODE)return;
  let cancelled=false;
  (async()=>{
   try{
    const runs=(await aiApi.chat(project.id)).filter(run=>run.prompt&&!run.prompt.startsWith('SANA_MOODBOARD_PLAN')&&!run.businessToolType
     &&(run.diagramType==='BRANCHING_STORY')===Boolean(branchingMode)).reverse();
    if(cancelled||!alive.current||!runs.length)return;
    const restored=[];
    for(const run of runs){
     // Quick start adds a note for the AI when it retries a rejected diagram; it is not part of what the user wrote
     restored.push({role:'user',text:run.prompt.split('\n\nYour previous diagram for this request was rejected:')[0]});
     if(['PENDING','RUNNING'].includes(run.status))continue;
     restored.push(...runMessages(run));
     if(run.proposalReviewStatus==='ACCEPTED')restored.push({role:'ai',text:t('aiAccepted')});
     if(run.proposalReviewStatus==='REJECTED')restored.push({role:'ai',text:t('aiRejected')});
    }
    // Keep anything the user typed while the history was loading
    setMessages(current=>[...restored,...current]);
    const latest=runs[runs.length-1];
    if(['PENDING','RUNNING'].includes(latest.status)){
     setBusy(true);
     try{const run=await waitFor(latest);if(!cancelled&&alive.current)await showRun(run)}finally{if(alive.current)setBusy(false)}
    }else{
     const pending=[...runs].reverse().find(run=>run.proposalId&&run.proposalReviewStatus==='PENDING');
     if(pending){const preview=await aiApi.proposal(project.id,pending.proposalId);if(!cancelled&&alive.current)setProposal(current=>current||preview)}
    }
   }catch{/* The chat still works without its history */}
  })();
  return()=>{cancelled=true};
 },[project.id,branchingMode]);
 // Show a finished run in the chat, and its proposal for review
 async function showRun(run,sourcePrompt){
  runMessages(run,sourcePrompt).forEach(m=>setMessages(list=>[...list,m]));
  if(run.status==='COMPLETED'&&run.result?.type==='PROPOSAL'&&run.proposalId)setProposal(await aiApi.proposal(project.id,run.proposalId));
 }
 // Poll the request every 2 seconds until n8n has answered (the backend gives up after 3 minutes)
 async function waitFor(run){let current=run;for(let i=0;i<120&&alive.current&&['PENDING','RUNNING'].includes(current.status);i++){await new Promise(r=>setTimeout(r,2000));current=await aiApi.status(current.statusUrl)}return current}
 // Explicit approval starts a *separate* STORY_MAP character proposal.
 // The existing review/accept endpoint saves the character only after approval.
 async function proposeApprovedCharacter(idea){
  if(busy||proposal||!branchingMode)return;
  if(idea)setPendingCharacterIdea(idea);
  const story=project.diagrams?.find(d=>d.outputType==='STORY_MAP');
  if(!story){say('notice',moodText('لا توجد خريطة قصة لاستقبال الشخصية؛ افتحي نموذج إضافتها بدلًا من ذلك.','There is no story map for a new character. Use the character form instead.'));onNewCharacter(idea);return;}
  setBusy(true);
  say('notice',moodText('بموافقتك، سأطلب من سنا تجهيز شخصية جديدة للمراجعة في خريطة القصة. لن تُحفظ قبل قبولك للمقترح.','With your permission, SANA will draft a character in the story map for review. Nothing is saved before acceptance.'));
  try{
   const request=await aiApi.start(project.id,{
    task:'EDIT',outputId:story.id,conversationHistory:[],
    prompt:`The user explicitly approved drafting ONE new project character for a dialogue tree. Add only a new CHARACTER node to the existing STORY_MAP. Preserve EVERY existing node, edge, ID, coordinate and graph property unchanged. No other modifications, no deletions, no new edges. Infer a fitting properties.label, nonempty properties.shortDescription, properties.role (up to 50 characters), and positive integer properties.age from the established story and this user request: ${idea}. Return a full PROPOSAL graph for review. Do not claim it was saved.`
   },project.domain);
   const run=await waitFor(request);
   if(!alive.current)return;
   if(run.status!=='COMPLETED'||run.result?.type!=='PROPOSAL'||!run.proposalId){
    say('notice',moodText('سنا لم تُجهّز مقترح شخصية قابلًا للحفظ. يمكن إعادة المحاولة أو إضافتها بالنموذج.','SANA did not return a reviewable character. Try again or use the form.'));
    return;
   }
   const draft=await aiApi.proposal(project.id,run.proposalId);
   const changes=Array.isArray(draft?.changes)?draft.changes:[];
   const onlyNewCharacters=String(draft.outputId)===String(story.id) && changes.length>0 &&
    changes.every(change=>change.operation==='CREATE'&&change.targetType==='NODE'&&change.after?.type==='CHARACTER');
   if(!onlyNewCharacters){
    await aiApi.reject(project.id,run.proposalId);
    say('notice',moodText('رفضتُ المسودة لأنها قد تغيّر عناصر غير الشخصية الجديدة. لم يُحفظ شيء.','I rejected the draft because it included changes beyond the new character. Nothing was saved.'));
    return;
   }
   setPendingCharacterIdea(idea);
   setCharacterDraft(true);
   setProposal(draft);
   say('ai',moodText('جهزت لك سنا شخصية جديدة. راجعي بياناتها أدناه واضغطي «موافقة» فقط إذا ناسبتك. بعدها نرجع لنكمل الحوار.','SANA drafted a new character. Review the proposal below and approve it only if it fits. Then continue the dialogue.'));
  }catch(err){if(alive.current)say('error',err.message)}
  finally{if(alive.current)setBusy(false)}
 }
 async function send(e){
  e?.preventDefault();const text=prompt.trim();if(!text||busy)return;
  // Intercept only Creativity moodboards, before graph checks or proposal resets.
  const route=routeCreativityChat({domain:project.domain,text});
  if(route!=='existing'){
   say('moodboard-user',text);setPrompt('');
   if(route==='clarify')setMoodboardQuestion(text);
   else await previewMoodboard(text);
   return;
  }
  setMoodboardQuestion(null);
  // The opt-in development demo can exercise routing without a backend.
  if(!API_MODE){say('notice',moodText('المساعد غير متاح في وضع المعاينة.','The assistant is unavailable in preview mode.'));return;}
  if(project.legacyCreative&&!branchingMode&&task!=='CREATE'){
    say('error', t('legacyDiagramAiWarning'));
    return;
  }
  if(proposal){say('notice',moodText('راجعي المقترح الحالي أولًا.','Review the current proposal first.'));return;}
  // Never let a new-character request skip its separate reviewed STORY_MAP proposal.
  // The branching graph only stores scenes/choices and cannot create a CharacterProfile.
  if(branchingMode&&requestsNewStoryCharacter(text)&&approvedCharacterRequest.current?.request!==text){
   setPendingCharacterIdea(text);
   say('user',text);setPrompt('');
   say('ai',moodText('قبل إضافة المشهد، لازم نحفظ الشخصية الجديدة في خريطة الشخصيات. أقدر أجهز لك مقترح الشخصية وتوافقين عليه أولًا، وبعدها نكمل الحوار.','Before creating the scene, I need to draft the new character in the Story Map for your approval. Then we can continue the dialogue.'),{characterApproval:true,sourcePrompt:text});
   return;
  }
  const branchOutputId=branchingMode?selectedBranchingDiagram(project)?.id:null;
  if(task!=='CREATE'&&!(branchingMode?branchOutputId:project.activeOutputId)){say('error',moodText('أنشئ شجرة الحوارات أولًا أو اختر إنشاء شجرة جديدة.','Create a dialogue tree first or choose to create a new one.'));return;}
  sessionStorage.removeItem(`sana-ai-seed-${project.id}`);if(branchingMode)sessionStorage.removeItem(`sana-branching-pending-${project.id}`);setBusy(true);say('user',text);setPrompt('');
  try{
   const creativity=['writing','games'].includes(project.domain);
   // The dedicated Creativity API accepts conversation turns; other domains stay untouched.
   const history=creativity?messages.filter(m=>m.role==='user'||m.role==='ai')
     .slice(-12).map(m=>({role:m.role==='ai'?'assistant':'user',content:m.text.slice(0,1500)})):[];
   const approvedCharacter=branchingMode&&approvedCharacterRequest.current?.request===text?approvedCharacterRequest.current:null;
   const modelPrompt=approvedCharacter
    ? `${text}\n\nProject character approval is complete: the character "${approvedCharacter.name}" was approved and saved in STORY_MAP. Retrieve updated context.projectCharacters and reuse its real ID as speakerCharacterId when that character speaks. Do not invent another character or add a duplicate. If this character is not found in projectCharacters, ask for clarification instead of proposing a graph.`
    : text;
   const body=task==='CREATE'?{task,prompt:modelPrompt,diagramType:branchingMode?'BRANCHING_STORY':diagramType}:{task,prompt:modelPrompt,outputId:branchingMode?branchOutputId:project.activeOutputId};
   if(creativity)body.conversationHistory=history;
   const run=await waitFor(await aiApi.start(project.id,body,project.domain));if(!alive.current)return;
   await showRun(run,text);
  }catch(err){say('error',err.message)}finally{if(alive.current)setBusy(false)}
 }
 async function decide(accept){
  setBusy(true);
  try{
   if(accept){
    const characterOnly=characterDraft;
    const r=await aiApi.accept(project.id,proposal.proposalId);
    if(branchingMode&&!characterOnly)rememberBranchingOutput(project.id,r.outputId);
    setProposal(null);setCharacterDraft(false);
    if(branchingMode&&!characterOnly)setTask('EDIT');
    if(characterOnly){
     const created=proposal.changes.filter(c=>c.operation==='CREATE'&&c.targetType==='NODE'&&c.after?.type==='CHARACTER');
     const name=created[0]?.after?.properties?.label||created[0]?.after?.label||'';
     approvedCharacterRequest.current={request:pendingCharacterIdea,name};
     setPrompt(characterOnlyRequest(pendingCharacterIdea)?'':pendingCharacterIdea);
     setPendingCharacterIdea('');
     say('notice',characterOnlyRequest(pendingCharacterIdea)
      ? moodText('انحفظت الشخصية في خريطة القصة. النهاية السابقة لم تتغير؛ افتح مشهدها واختر الشخصية في حقل المتحدث لربطهما.','The character is saved in the Story Map. Your existing ending is unchanged; open the scene and choose the character as its speaker.')
      : moodText('انحفظت الشخصية الجديدة في خريطة القصة. طلب الحوار السابق جاهز؛ اضغطي إرسال، وسنا بتستخدم الشخصية المحفوظة.','The new character is saved in the Story Map. Your dialogue request is ready; send it to reuse the saved character.'));
    }else say('ai',t('aiAccepted'));
    await onApplied(r.outputId,{characterOnly});
   }
   else{await aiApi.reject(project.id,proposal.proposalId);setProposal(null);setCharacterDraft(false);setPendingCharacterIdea('');say('ai',t('aiRejected'))}
  }catch(err){say('error',err.message)}finally{setBusy(false)}
 }
 const counts=proposal?['CREATE','UPDATE','DELETE'].map(op=>[op,proposal.changes.filter(c=>c.operation===op).length]):[];
 const changeLabel=c=>(c.after||c.before)?.properties?.label||(c.after||c.before)?.label||c.targetType;
 return <aside className="ai-sidebar">
  <div className="ai-side-head"><div className="ai-gem"><Sparkles size={23}/></div><div><strong>{t('ai')}</strong><span>{branchingMode?(lang==='ar'?'شجرة الحوارات':'Dialogue tree'):t(`diagram_${project.activeType}`)}</span></div><IconButton onClick={onClose} label={t('close')}><X size={18}/></IconButton></div>
  <div className="ai-body" style={{overflowY:'auto'}}>
   {messages.length===0&&<div className="ai-message">{sessionStorage.getItem(`sana-ai-seed-${project.id}`)?(lang==='ar'?'وصلت فكرتك! راجع النص الجاهز بالأسفل واضغط «إرسال» لتبدأ سنا بمساعدتك.':'Your idea is ready below. Review it and press Send to begin with SANA.'):project.legacyCreative?t('legacyDiagramAiWarning'):t('aiIntro')}</div>}
   {messages.map((m,i)=><div key={i} className="ai-message" style={{whiteSpace:'pre-wrap',...(m.role==='user'?{opacity:.75}:{}),...(m.role==='error'?{color:'#b42318'}:{})}}>
    {m.kind&&<small className="spark-result-kind">{m.kind==='ADVICE'?(lang==='ar'?'نصيحة':'Advice'):m.kind==='CLARIFICATION'?(lang==='ar'?'توضيح':'Clarification'):(lang==='ar'?'مقترح':'Proposal')}</small>}{m.role==='user'||m.role==='moodboard-user'?'› ':''}{m.text}
    {m.characterApproval&&<div className="ai-ideas"><Btn disabled={busy||Boolean(proposal)} onClick={()=>proposeApprovedCharacter(m.sourcePrompt||'')}>{moodText('أوافق، خلي سنا تقترح الشخصية للمراجعة','I approve — let SANA draft the character')}</Btn><Btn variant="ghost" disabled={busy||Boolean(proposal)} onClick={()=>onNewCharacter(m.sourcePrompt||'')}>{moodText('أبي أضيف الشخصية بنفسي','I prefer to add it myself')}</Btn></div>}
    {m.answers?.length>0&&<div className="ai-ideas">{m.answers.map(a=><div key={a} role="button" tabIndex={0} style={{cursor:'pointer'}} onKeyDown={e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();if(m.characterApproval&&/أوافق|approve/i.test(a))proposeApprovedCharacter(m.sourcePrompt||'');else setPrompt(p=>(p?p+'\n':'')+a)}}} onClick={()=>{if(m.characterApproval&&/أوافق|approve/i.test(a))proposeApprovedCharacter(m.sourcePrompt||'');else setPrompt(p=>(p?p+'\n':'')+a)}}><Sparkles size={13}/>{a}</div>)}</div>}
   </div>)}
   {moodboardQuestion&&<div className="ai-message creativity-mood-chat-notice" role="status"><p>{moodText('هل تقصدين إنشاء لوحة إلهام فقط؟ الطلب غير واضح أو يجمع أكثر من مهمة؛ لم يُرسل بعد.','Do you mean to create only a moodboard? This request is ambiguous or combines tasks; it has not been sent.')}</p><div className="modal-actions"><Btn variant="ghost" disabled={busy} onClick={()=>previewMoodboard(moodboardQuestion)}>{moodText('لوحة إلهام فقط','Moodboard only')}</Btn><Btn variant="ghost" onClick={()=>{setPrompt(moodboardQuestion);setMoodboardQuestion(null);}}>{moodText('تعديل رسالتي','Edit my message')}</Btn><Btn variant="ghost" onClick={()=>setMoodboardQuestion(null)}>{moodText('إلغاء','Cancel')}</Btn></div></div>}
   {moodboardPreview&&<MoodboardChatPreview key={moodboardPreview.draftId} preview={moodboardPreview} lang={lang} busy={busy} error={moodboardError} renderActions={()=> <><Btn variant="ghost" disabled={busy} onClick={()=>decideMoodboard(false)}>{t('aiReject')}</Btn><Btn icon={Check} disabled={busy} onClick={()=>decideMoodboard(true)}>{moodText('موافقة وحفظ','Approve and save')}</Btn></>}/>}
   {busy&&!proposal&&<div className="ai-message spark-thinking" role="status"><span aria-hidden="true">✦</span>{t('aiWorking')}</div>}
   {proposal&&<div className="ai-message"><strong>{proposal.title}</strong><p style={{whiteSpace:'pre-wrap'}}>{proposal.description}</p>
    <div>{counts.filter(([,n])=>n>0).map(([op,n])=><span key={op} style={{marginInlineEnd:10}}>{t(`aiOp_${op}`)}: {n}</span>)}</div>
    <ul style={{margin:'6px 0',paddingInlineStart:18}}>{proposal.changes.slice(0,12).map((c,i)=><li key={i}>{t(`aiOp_${c.operation}`)} · {changeLabel(c)}</li>)}</ul>
    {proposal.reviewStatus==='PENDING'&&<div className="modal-actions"><Btn variant="ghost" disabled={busy} onClick={()=>decide(false)}>{t('aiReject')}</Btn><Btn icon={Check} disabled={busy} onClick={()=>decide(true)}>{t('aiAccept')}</Btn></div>}
   </div>}
  </div>
  <form className="ai-bottom" onSubmit={send}>
   <div style={{display:'flex',gap:6,flexWrap:'wrap',marginBottom:6}}>
    <select value={task} onChange={e=>setTask(e.target.value)} aria-label={t('aiTask')}>{['ADVISE','EDIT','CREATE'].map(k=><option key={k} value={k}>{t(`aiTask_${k}`)}</option>)}</select>
    {task==='CREATE'&&(branchingMode?<span className="sana-ai-target-tag">{moodText('شجرة الحوارات','Dialogue tree')}</span>:<select value={diagramType} onChange={e=>setDiagramType(e.target.value)} aria-label={t('diagram')}>{(project.diagramTypes||[]).map(k=><option key={k} value={k}>{t(`diagram_${k}`)}</option>)}</select>)}
   </div>
   <textarea rows={3} value={prompt} onChange={e=>setPrompt(e.target.value)} placeholder={t('aiAsk')} style={{width:'100%'}} onKeyDown={e=>{if(e.key==='Enter'&&(e.metaKey||e.ctrlKey))send(e)}}/>
   <Btn type="submit" icon={Send} disabled={busy||!prompt.trim()}>{busy?t('aiWorking'):t('aiSend')}</Btn>
  </form>
 </aside>
}
function Studio({t,lang,project,tab,onTab,onBack,onAdd,onEdit,onDelete,onRelation,onEditProject,onMove,onExport,onOpenAI,onDeleteRelation,onDiagram,onNewDiagram,moodboardFocus,branchRefreshKey,assistantOpen=false,assistantDock=null,assistantDockRef=null,assistantCompact=false}){
 const [query,setQuery]=useState(''),[focus,setFocus]=useState(null),[localTab,setLocalTab]=useState(''),[toolsOpen,setToolsOpen]=useState(false),[boardMode,setBoardMode]=useState('board');const currentTab=tab==='books'&&project.domain!=='writing'?'map':tab==='integrations'&&!['writing','games','business'].includes(project.domain)?'overview':tab||'map';useEffect(()=>{if(!['map','overview'].includes(currentTab))setToolsOpen(true)},[currentTab]);const elements=(project.elements||[]).filter(e=>(currentTab==='overview'||currentTab==='map'||e.type===currentTab)&&`${e.name} ${e.description}`.toLowerCase().includes(query.toLowerCase()));const hasExtraTools=project.domain==='business';const specialTabs=[...(hasExtraTools?['integrations']:[]),...(['writing','games'].includes(project.domain)?[...(project.domain==='writing'?['books']:[]),'timeline','moodboard',...(project.domain==='games'?['branching','balance']:[])]:[])];const tabs=['map','overview',...elementTypes(project),...specialTabs];
 const tabIcon={integrations:Compass,books:BookOpen,timeline:History,overview:LayoutDashboard,...ICONS,map:Network,moodboard:ImageIcon,branching:GitBranch,balance:Gamepad2,swot_strength:ShieldCheck,swot_weakness:ShieldCheck,swot_opportunity:Sparkles,swot_threat:ShieldCheck,
 risk:ShieldCheck,risk_cause:Network,risk_mitigation:CheckCircle2,competitor:BriefcaseBusiness,competitor_product:Boxes,competitor_feature:Sparkles,
 target_audience:Target,customer_persona:Users};const viewMap=id=>{setFocus(id);onTab('map')};
 return <div className="studio-page page-enter"><div className="studio-topbar"><button onClick={onBack} className="breadcrumb-link"><FolderOpen size={16}/>{t('projects')}</button><span className="breadcrumb-sep">/</span><strong>{project.name}</strong></div><div className="studio-layout"><aside className="studio-sidebar"><div className="studio-identity"><span className="studio-monogram"><Sparkles size={23}/></span><div><strong>{t('studio')}</strong><small>SANA WORKSPACE</small></div></div><div className="studio-nav-label">{lang==='ar'?'لوحة المشروع':'PROJECT SPACE'}</div><div className="studio-nav">{['map','overview'].map(k=>{const Icon=tabIcon[k]||Layers3;return <button key={k} className={currentTab===k?'selected':''} title={t(k)} aria-label={t(k)} onClick={()=>{setQuery('');onTab(k)}}><Icon size={18}/><span>{t(k)}</span></button>})}<button type="button" title={lang==='ar'?'أدوات المشروع':'Project tools'} aria-label={lang==='ar'?'أدوات المشروع':'Project tools'} className={`studio-tools-toggle ${toolsOpen?'opened':''}`} onClick={()=>setToolsOpen(x=>!x)} aria-expanded={toolsOpen}><Layers3 size={18}/><span>{lang==='ar'?'أدوات المشروع':'Project tools'}</span><ChevronDown size={17}/></button>{(toolsOpen||assistantOpen)&&<div className="studio-grouped-tools">{tabs.filter(k=>!['map','overview'].includes(k)).map(k=>{const Icon=tabIcon[k];return <button key={k} className={currentTab===k?'selected':''} title={k==='books'?(lang==='ar'?'الكتب الملهمة':'Inspiring books'):k==='timeline'?(lang==='ar'?'التسلسل الزمني':'Timeline'):t(k)} aria-label={k==='books'?(lang==='ar'?'الكتب الملهمة':'Inspiring books'):k==='timeline'?(lang==='ar'?'التسلسل الزمني':'Timeline'):t(k)} onClick={()=>{setQuery('');onTab(k)}}><Icon size={18}/><span>{k==='integrations'?(project.domain==='business'?(lang==='ar'?'تحليلات إضافية':'Additional analysis'):(lang==='ar'?'أدوات إضافية':'Extra tools')):k==='books'?(lang==='ar'?'الكتب الملهمة':'Inspiring books'):k==='timeline'?(lang==='ar'?'التسلسل الزمني':'Timeline'):t(k)}</span>{!['moodboard','branching','balance','integrations'].includes(k)&&<small>{project.elements?.filter(e=>e.type===k).length||0}</small>}</button>})}</div>}</div><div className="sidebar-ai-call" onClick={onOpenAI} role="button" tabIndex={0} onKeyDown={e=>e.key==='Enter'&&onOpenAI()}><div className="ai-icon"><Sparkles size={21}/></div><strong>{t('ai')}</strong><p>{API_MODE?t('aiCardLive'):(lang==='ar'?'مساحة المساعد جاهزة، والربط قريبًا.':'Your AI companion is coming soon.')}</p><span>{API_MODE?t('aiOpen'):t('aiSoon')} <ArrowUpLeft size={15}/></span></div></aside><main className="studio-main">{!specialTabs.includes(currentTab)&&<div className="studio-intro"><div><span className="eyebrow"><span className="eyebrow-dot"/>{t(project.domain==='games'?'games':domainGroup(project.domain))}</span><h1>{currentTab==='overview'?project.name:currentTab==='map'&&project.domain==='technology'?(TECH_DIAGRAM_LABELS[project.activeType]||'Technology Diagram'):t(currentTab)}</h1><p>{currentTab==='overview'?(project.description||t('workspaceSubtitle')):currentTab==='map'&&project.domain==='technology'?(lang==='ar'?'اسحب العناصر لتغيير ترتيبها، وحدد عنصرًا لتعديل خصائصه أو إنشاء علاقة.':'Drag components, inspect properties and connect relationships'):currentTab==='map'?t('dragHint'):t('workspaceSubtitle')}</p></div><DomainPill domain={project.domain} t={t}/></div>}
 {currentTab!=='map'&&<div className="sana-workspace-utilities sana-workspace-utilities-compact">
  <IconButton label={t('projectSettings')} onClick={()=>onEditProject(project)}><Settings2 size={18}/></IconButton>
  <Btn icon={Download} variant="outline-dark" onClick={onExport}>{lang==='ar'?'تصدير المخطط':'Export diagram'}</Btn>
 </div>}
 {currentTab==='books'?<BookInspiration project={project} lang={lang}/>:currentTab==='timeline'?<DomainPanels project={project} lang={lang} mode="timeline" onViewEvent={async event=>{const story=project.diagrams?.find(d=>d.outputType==='STORY_MAP');if(story?.id&&String(story.id)!==String(project.activeOutputId))await onDiagram(story.id);viewMap(event?.nodeId??null)}}/>:currentTab==='integrations'?<StudioToolBoundary key={`${project.id}-${currentTab}`} lang={lang}><DomainPanels project={project} lang={lang}/></StudioToolBoundary>:currentTab==='moodboard'?<Moodboard project={project} lang={lang} onOpenAI={onOpenAI} initialBoardId={moodboardFocus?.projectId===project.id?moodboardFocus.boardId:null}/>:currentTab==='branching'?<BranchingNarrative key={`${project.id}-${branchRefreshKey}`} project={project} lang={lang} onOpenAI={onOpenAI}/>:currentTab==='balance'?<GameBalance project={project} lang={lang}/>:currentTab==='overview'?<><div className="studio-stats"><Stat shade="stat-teal" icon={Layers3} num={project.elements?.length||0} label={t('creativeElements')}/><Stat shade="stat-gold" icon={Network} num={project.relations?.length||0} label={t('connectedIdeas')}/><Stat shade="stat-rose" icon={History} num={dateLabel(project.updatedAt,lang)} label={t('lastEdited')}/></div><SectionTitle title={t('recentElements')} description={t('workspaceSubtitle')} action={<Btn variant="soft" onClick={()=>onAdd(elementTypes(project)[0])} icon={Plus}>{t('add')}</Btn>}/>{elements.length?<div className="elements-grid">{elements.slice(0,6).map(el=><ElementTile key={el.id} element={el} t={t} onEdit={onEdit} onDelete={onDelete} onFocus={viewMap}/>)}</div>:<Empty icon={Sparkles} title={t('emptyElements')} description={t('emptyElementsBody')} action={<Btn icon={Plus} onClick={()=>onAdd(elementTypes(project)[0])}>{t('add')}</Btn>}/>}<div className="overview-link-card"><span className="overview-link-icon"><Network size={29}/></span><div><h3>{t('map')}</h3><p>{t('featureMapsBody')}</p></div><Btn variant="outline-dark" onClick={()=>onTab('map')}>{t('viewMap')}</Btn></div></>:currentTab==='map'?<><div className="map-section-bar sana-map-tools" role="toolbar" aria-label={lang==='ar'?'أدوات المخطط':'Diagram tools'}>
  {API_MODE&&<DiagramBar t={t} project={project} onDiagram={onDiagram} onNewDiagram={onNewDiagram}/>}
  <div className="sana-map-tools-actions">
   <Btn onClick={onRelation} icon={Link2}>{t('newRelation')}</Btn>
   <IconButton label={t('projectSettings')} onClick={()=>onEditProject(project)}><Settings2 size={18}/></IconButton>
   <Btn icon={Download} variant="outline-dark" onClick={onExport}>{lang==='ar'?'تصدير المخطط':'Export diagram'}</Btn>
  </div>
 </div>{SPECIAL_BOARD_TYPES.has(project.activeType)&&<div className="sana-view-switch" role="group" aria-label={lang==='ar'?'طريقة العرض':'View mode'}><button className={boardMode==='board'?'active':''} onClick={()=>setBoardMode('board')} type="button">{lang==='ar'?'عرض متخصص':'Specialized board'}</button><button className={boardMode==='graph'?'active':''} onClick={()=>setBoardMode('graph')} type="button">{lang==='ar'?'عرض العلاقات والكانفاس':'Connections & canvas'}</button></div>}{SPECIAL_BOARD_TYPES.has(project.activeType)&&boardMode==='board'?<SpecializedDiagramBoard project={project} t={t} lang={lang} onSelect={onEdit} onAdd={onAdd} onRelation={onRelation}/>:<MapView key={`${project.id}-${focus||'all'}`} initialFocus={focus} t={t} lang={lang} project={project} onSelect={onEdit} onAdd={onAdd} onMove={onMove} onAddRelation={onRelation} onDeleteRelation={onDeleteRelation}/>}</>:<><div className="studio-tool-row"><div className="search-field"><Search size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder={t('search')} /></div><Btn onClick={()=>onAdd(currentTab)} icon={Plus}>{t('add')} {t(currentTab)}</Btn></div>{elements.length?<div className="elements-grid">{elements.map(el=><ElementTile key={el.id} element={el} t={t} onEdit={onEdit} onDelete={onDelete} onFocus={viewMap}/>)}</div>:<Empty icon={ICONS[currentTab]||Sparkles} title={t('emptyElements')} description={t('emptyElementsBody')} action={<Btn icon={Plus} onClick={()=>onAdd(currentTab)}>{t('add')}</Btn>}/>}</>}</main><div ref={assistantDockRef} className="spark-assistant-dock" hidden={!assistantOpen} role={assistantCompact&&assistantOpen?'dialog':undefined} aria-modal={assistantCompact&&assistantOpen?true:undefined} aria-label={lang==='ar'?'مساعد سنا الذكي':'SANA AI assistant'}>{assistantDock}</div></div></div>
}
function Footer({t,lang,onNavigate}){return <footer className="footer"><div className="main-shell footer-inner"><div className="footer-brand"><img src="/sana-logo.webp" alt="SANA"/><span>{t('copyright')}</span></div><div className="footer-links"><button onClick={()=>onNavigate('home')}>{t('home')}</button><button onClick={()=>onNavigate('projects')}>{t('projects')}</button><button onClick={()=>onNavigate('tools')}>{t('tools')}</button><button onClick={()=>onNavigate('library')}>{t('library')}</button><button onClick={()=>onNavigate('pricing')}>{lang==='ar'?'الباقات':'Plans'}</button><button onClick={()=>onNavigate('membership')}>{lang==='ar'?'باقتي':'My plan'}</button></div><small>© 2026 SANA · MADE WITH ✦</small></div></footer>}
function SettingsPage({t,lang,onLanguage,onNew,onMembership}){return <div className="main-shell inner-page page-enter"><div className="page-intro"><span className="eyebrow"><span className="eyebrow-dot"/>{t('settings')}</span><h1>{t('settings')}</h1><p>{t('helpText')}</p></div><div className="settings-grid"><section className="settings-card"><Globe2 size={26}/><h2>{t('language')}</h2><p>{lang==='ar'?'اختر لغة عرض الواجهة واتجاهها.':'Choose the language and reading direction.'}</p><div className="language-choice"><button className={lang==='ar'?'active':''} onClick={()=>onLanguage('ar')}>العربية</button><button className={lang==='en'?'active':''} onClick={()=>onLanguage('en')}>English</button></div></section><section className="settings-card"><ShieldCheck size={26}/><h2>{t('help')}</h2><p>{t('helpText')}</p></section><section className="settings-card sana-settings-membership"><Sparkles size={26}/><h2>{lang==='ar'?'باقتي':'My membership'}</h2><p>{lang==='ar'?'تعرف على باقتك الحالية وعدد مشاريعك المتاح.':'View your membership and project allowance.'}</p><Btn variant="outline-dark" onClick={onMembership}>{lang==='ar'?'عرض باقتي':'View my plan'}</Btn></section></div></div>}

function App({onLogout,user,onLogin}){
 const guest=Boolean(API_MODE&&!user);
 const [moodboardFocus,setMoodboardFocus]=useState(null);
 const [branchRefreshKey,setBranchRefreshKey]=useState(0);
 const [lang,setLang]=useState(()=>localStorage.getItem('sana-lang')||'ar');const [route,setRoute]=useState(parseRoute);const [projects,setProjects]=useState([]);const [project,setProject]=useState(null);const [loading,setLoading]=useState(!guest);const [modal,setModal]=useState(null);const [busy,setBusy]=useState(false);const [toast,setToast]=useState(null);const [aiOpen,setAiOpen]=useState(false);const [mobileMenu,setMobileMenu]=useState(false);const importRef=useRef(null);
 const t=useCallback(key=>humanize(tr(lang,key)),[lang]);
 const notice=(message,error=false)=>setToast({message:error?publicError(message,lang):message,error});
 const navigate=useCallback((page,id,tab)=>{if(guest&&!['home','tools','library','pricing','settings'].includes(page)){setMobileMenu(false);onLogin?.();return;}window.location.hash=page==='studio'?`/studio/${id}/${tab||'map'}`:page==='library'&&id?`/library/${id}`:`/${page}`;setMobileMenu(false);window.scrollTo({top:0,behavior:'smooth'})},[guest,onLogin]);
 const refresh=useCallback(async()=>{setLoading(true);try{const all=await repository.list();setProjects(Array.isArray(all)?all:[])}catch(e){notice(e.message||t('apiError'),true)}finally{setLoading(false)}},[t]);
 const loadOne=useCallback(async id=>{try{const p=await repository.get(id);if(!p)throw new Error(t('notFound'));setProject(p);try{const key=`sana-recent-${user?.id||'local'}`,ids=JSON.parse(localStorage.getItem(key)||'[]');localStorage.setItem(key,JSON.stringify([String(p.id),...ids.filter(x=>x!==String(p.id))].slice(0,12)))}catch{}}catch(e){notice(e.message,true);setProject(null)}},[t]);
 useEffect(()=>{const change=()=>setRoute(parseRoute());window.addEventListener('hashchange',change);return()=>window.removeEventListener('hashchange',change)},[]);
 useEffect(()=>{document.documentElement.lang=lang;document.documentElement.dir=lang==='ar'?'rtl':'ltr';localStorage.setItem('sana-lang',lang)},[lang]);
 useEffect(()=>{if(!guest)refresh()},[guest,refresh]);
 useEffect(()=>{if(guest&&!['home','tools','library','pricing','settings'].includes(route.page)){window.location.hash='/home';return;}if(!guest&&route.page==='studio'&&route.id)loadOne(route.id)},[guest,route.page,route.id,loadOne]);
 useEffect(()=>{if(!toast)return;const timer=setTimeout(()=>setToast(null),4400);return()=>clearTimeout(timer)},[toast]);
 const perform=async(fn,onSuccess)=>{setBusy(true);try{const result=await fn();if(onSuccess)await onSuccess(result);setModal(null);return result}catch(e){notice(e?.message||t('error'),true);return null}finally{setBusy(false)}};
 const saveProject=payload=>perform(()=>modal?.initial?repository.updateProject(modal.initial.id,payload):repository.createProject(payload),async p=>{await refresh();notice(t('saved'));if(!modal?.initial&&p?.id){const seed=sessionStorage.getItem('sana-ai-pending');if(seed){sessionStorage.setItem(`sana-ai-seed-${p.id}`,seed);sessionStorage.removeItem('sana-ai-pending');setAiOpen(true)}navigate('studio',p.id,'map');}else if(route.page==='studio'&&route.id)loadOne(route.id)});
 const saveElement=(type,values,id)=>perform(()=>repository.saveElement(project.id,type,values,id),async p=>{
  if(type==='characters'&&values.characterColor){
   const newNode=id||p?.elements?.find(e=>e.type==='characters'&&!project.elements.some(old=>String(old.id)===String(e.id)))?.id;
   if(newNode!=null)saveCharacterColor(project.id,newNode,values.characterColor);
  }
  setProject(p);await refresh();notice(t('saved'));
  if(type==='characters'&&sessionStorage.getItem(`sana-branching-pending-${project.id}`)){navigate('studio',project.id,'branching');setAiOpen(true)}
 });
 const deleteThing=()=>perform(async()=>{if(modal.what==='project')return repository.removeProject(modal.initial.id);if(modal.what==='relation')return repository.removeRelation(project.id,modal.initial);return repository.removeElement(project.id,modal.initial)},async p=>{if(modal.what==='project'){await refresh();if(route.page==='studio')navigate('projects')}else{setProject(p);await refresh()}notice(t('deleted'))});
 const addRelation=data=>perform(()=>data.id?repository.updateRelation(project.id,data):repository.createRelation(project.id,data),async p=>{setProject(p);await refresh();notice(t('relationSaved'))});
 const move=async(id,x,y)=>{try{await repository.moveNode(project.id,id,x,y);setProject(await repository.get(project.id))}catch(e){notice(e.message,true);throw e}};
 const switchDiagram=outputId=>perform(()=>repository.setActiveDiagram(project.id,outputId),p=>setProject(p));
 const newDiagram=(title,diagramType)=>perform(()=>repository.addDiagram(project.id,title,diagramType),async p=>{setProject(p);await refresh();notice(t('saved'))});
 const aiApplied=async (outputId,{characterOnly=false}={})=>{try{
  const branchResult=route.page==='studio'&&route.tab==='branching';
  // A character proposal targets STORY_MAP. Never remember its ID as a BRANCHING_STORY output.
  if(branchResult&&!characterOnly)rememberBranchingOutput(project.id,outputId);
  const p=branchResult?await repository.get(project.id):(outputId?await repository.setActiveDiagram(project.id,outputId):await repository.get(project.id));
  setProject(p);
  if(branchResult&&!characterOnly)setBranchRefreshKey(k=>k+1);
  await refresh();
 }catch(e){notice(e.message,true)}};
 const addApprovedStoryCharacter=async idea=>{try{const story=project.diagrams?.find(d=>d.outputType==='STORY_MAP');if(!story){notice(lang==='ar'?'أنشئ خريطة قصة أولًا لإضافة الشخصية.':'Create a story map first to add a character.',true);return;}if(idea)sessionStorage.setItem(`sana-branching-pending-${project.id}`,idea);const p=await repository.setActiveDiagram(project.id,story.id);setProject(p);setAiOpen(false);navigate('studio',project.id,'map');setModal({kind:'element',initialType:'characters'});}catch(e){notice(e.message,true)}};
 const exportProject=()=>{if(!project)return;const blob=new Blob([JSON.stringify(project,null,2)],{type:'application/json'}),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`sana-${String(project.name).replace(/[\\/:*?"<>|]/g,'-')}.json`;a.click();URL.revokeObjectURL(url);notice(project.domain==='technology'?(lang==='ar'?'تم تصدير المخطط':'Diagram exported'):t('export'))};
 const importFile=async file=>{if(!file)return;try{const json=JSON.parse(await file.text());const p=await repository.importProject(json);await refresh();notice(t('imported'));navigate('studio',p.id)}catch(e){notice(`${t('importError')}: ${e.message}`,true)}};
 const previewDelete=async initial=>{if(!API_MODE){setModal({kind:'confirm',what:'element',initial});return;}try{const impact=await repository.deleteImpact(project.id,initial);setModal({kind:'confirm',what:'element',initial,impact})}catch(e){notice(e.message,true)}};
 // Frontend guidance only; the server MUST enforce project quotas on project creation.
 const planAllowsNewProject=async()=>{
  if(billingEnabled){
   try{
    const membership=await billingApi.getMembership();
    const limit=membershipLimit(membership);
    const count=Number.isInteger(Number(membership?.projectsUsed))?Number(membership.projectsUsed):projects.length;
    if(limit!==null&&count>=limit){
     notice(lang==='ar'?'وصلت إلى عدد المشاريع المتاح في باقتك. اختر باقة أكبر للمتابعة.':'Your plan has reached its project limit. Upgrade to create more projects.',true);
     navigate('pricing');
     return false;
    }
   }catch{
    notice(lang==='ar'?'تعذر التحقق من باقتك حاليًا. جرّب مرة أخرى.':'Unable to check your plan right now. Please try again.',true);
    return false;
   }
  }
  return true;
 };
 const openNew=async(seed)=>{
  if(guest){onLogin?.();return;}
  if(!await planAllowsNewProject())return;
  if(route.page!=='ai-start')sessionStorage.removeItem('sana-ai-pending');
  setModal({kind:'project',seed});
 };
 // Quick start: the Start page created the project and drew its diagram; show it in the Studio
 const quickStartReady=async projectId=>{await refresh();setAiOpen(false);navigate('studio',projectId,'map')};
 const main=()=>{
 if(guest&&!['home','tools','library','pricing','settings'].includes(route.page))return <Landing guest projects={[]} t={t} lang={lang} onNavigate={navigate} onNew={()=>openNew()} onLogin={onLogin} onOpen={()=>onLogin?.()} canCreateProject={planAllowsNewProject} onReady={quickStartReady}/>;
 if(route.page==='home')return <Landing guest={guest} projects={projects} t={t} lang={lang} onNavigate={navigate} onNew={()=>openNew()} onLogin={onLogin} onOpen={id=>navigate('studio',id)} canCreateProject={planAllowsNewProject} onReady={quickStartReady}/>;
 if(route.page==='projects')return <Projects user={user} projects={projects} t={t} lang={lang} onNew={()=>openNew()} onOpen={id=>navigate('studio',id)} onEdit={initial=>setModal({kind:'project',initial})} onDelete={initial=>setModal({kind:'confirm',what:'project',initial})} onImport={()=>importRef.current?.click()}/>;
 if(route.page==='tools')return <ToolsPage projects={projects} t={t} lang={lang} onNew={seed=>openNew(seed)} onOpen={id=>navigate('studio',id)} onNavigate={navigate}/>;
 if(route.page==='library')return <LibraryPage key={route.domain||'all'} initialDomain={route.domain||'all'} t={t} lang={lang} onNew={seed=>openNew(seed)}/>;
  if(route.page==='ai-start')return <AIStartPage t={t} lang={lang} projects={projects} canCreateProject={planAllowsNewProject} onReady={quickStartReady} onContinue={(id,idea)=>{sessionStorage.setItem(`sana-ai-seed-${id}`,idea);navigate('studio',id);setAiOpen(true)}} onTemplates={()=>navigate('library')} onNew={seed=>openNew(seed)}/>;
 if(route.page==='admin')return <AdminStudio user={user} lang={lang} onOpen={id=>navigate('studio',id)}/>;
 if(route.page==='pricing')return <BillingExperience guest={guest} view="plans" lang={lang} projects={projects} onNavigate={navigate}/>;
 if(route.page==='membership')return <BillingExperience view="membership" lang={lang} projects={projects} onNavigate={navigate}/>;
 if(route.page==='billing-result')return <BillingExperience view="result" resultStatus={route.resultStatus} lang={lang} projects={projects} onNavigate={navigate}/>;
 if(route.page==='settings')return <SettingsPage t={t} lang={lang} onLanguage={setLang} onNew={()=>openNew()} onMembership={()=>navigate('membership')}/>;
 if(route.page==='studio')return project&&String(project.id)===String(route.id)?<StudioWorkspace open={aiOpen} assistant={API_MODE?<AIPanel key={`${project.id}-${route.tab==='branching'?'branch':'graph'}`} branchingMode={route.tab==='branching'&&project.domain==='games'} onNewCharacter={addApprovedStoryCharacter} t={t} lang={lang} project={project} onClose={()=>setAiOpen(false)} onApplied={aiApplied} onMoodboardSaved={async boardId=>{setMoodboardFocus({projectId:project.id,boardId});navigate('studio',project.id,'moodboard')}}/>:<AISidebar t={t} lang={lang} onClose={()=>setAiOpen(false)}/>}><Studio t={t} lang={lang} project={project} moodboardFocus={moodboardFocus} tab={route.tab} onTab={tab=>navigate('studio',project.id,tab)} onBack={()=>navigate('projects')} onAdd={(initialType,position)=>setModal({kind:'element',initialType,position})} onEdit={initial=>setModal({kind:'element',initial})} onDelete={previewDelete} onRelation={initial=>setModal({kind:'relation',initial:initial?.source?initial:null})} onEditProject={initial=>setModal({kind:'project',initial})} onMove={move} onExport={exportProject} onOpenAI={()=>setAiOpen(true)} assistantOpen={aiOpen} onDeleteRelation={initial=>setModal({kind:'confirm',what:'relation',initial})} onDiagram={switchDiagram} onNewDiagram={newDiagram} branchRefreshKey={branchRefreshKey}/></StudioWorkspace>:<div className="main-shell inner-page"><Empty icon={FolderOpen} title={t('notFound')} description={t('apiError')} action={<Btn onClick={()=>navigate('projects')}>{t('back')}</Btn>}/></div>;
 return <div className="main-shell inner-page"><Empty title={t('notFound')} action={<Btn onClick={()=>navigate('home')}>{t('home')}</Btn>}/></div>;
 };
 return <div className={`app ${route.page==='studio'?'sana-studio-route':'sana-public-route'}`} dir={lang==='ar'?'rtl':'ltr'}><AmbientStars/><header className="site-header"><div className="main-shell nav-wrap"><Logo onClick={()=>navigate('home')}/><nav className={`nav-links ${mobileMenu?'nav-open':''}`} aria-label="Main navigation">{[['home','home'],['projects','projects'],['tools','tools'],['library','library'],['pricing','pricing']].map(([page,key])=><button key={page} className={route.page===page?'active':''} onClick={()=>navigate(page)}>{page==='pricing'?(lang==='ar'?'الباقات':'Plans'):t(key)}</button>)}</nav><div className="nav-side">{user?.role==='ADMIN'&&<button className={`language-toggle sana-admin-nav ${route.page==='admin'?'active':''}`} onClick={()=>navigate('admin')}><LayoutDashboard size={16}/>{t('admin')}</button>}<button className="language-toggle" onClick={()=>setLang(lang==='ar'?'en':'ar')} aria-label={t('language')}><Globe2 size={17}/><span>{lang==='ar'?'EN':'عربي'}</span></button>{API_MODE&&(guest?<button type="button" className="sana-signout sana-signin" onClick={onLogin}>{lang==='ar'?'تسجيل الدخول':'Sign in'}</button>:<button type="button" className="sana-signout" onClick={onLogout}>{lang==='ar'?'تسجيل الخروج':'Sign out'}</button>)}<IconButton className="nav-settings" label={t('settings')} onClick={()=>navigate('settings')}><Settings2 size={19}/></IconButton><button className="nav-cta" onClick={()=>openNew()}><Plus size={17}/><span>{guest?(lang==='ar'?'ابدأ الآن':'Get started'):t('newProject')}</span></button><IconButton className="mobile-toggle" label={t('menu')} onClick={()=>setMobileMenu(m=>!m)}>{mobileMenu?<X size={21}/>:<Menu size={21}/>}</IconButton></div></div></header><div className="app-body">{loading&&projects.length===0?<div className="loading-view"><img src="/sana-logo.webp" alt="SANA"/><div className="loading-line"/></div>:main()}</div>{route.page!=='studio'&&<Footer t={t} lang={lang} onNavigate={navigate}/>}<Toast toast={toast} onClose={()=>setToast(null)}/>{modal?.kind==='project'&&<ProjectModal t={t} lang={lang} busy={busy} initial={modal.initial||modal.seed} onClose={()=>setModal(null)} onSave={saveProject}/>}{modal?.kind==='element'&&<ElementModal t={t} lang={lang} busy={busy} initial={modal.initial} initialType={modal.initialType} position={modal.position} availableTypes={elementTypes(project)} project={project} onClose={()=>setModal(null)} onSave={saveElement} onDelete={previewDelete} onGameBalance={()=>{setModal(null);navigate('studio',project.id,'balance')}}/>}{modal?.kind==='relation'&&<RelationModal initial={modal.initial} t={t} lang={lang} busy={busy} project={project} elements={project?.elements||[]} onClose={()=>setModal(null)} onSave={addRelation}/>}{modal?.kind==='confirm'&&<ConfirmModal t={t} lang={lang} busy={busy} onCancel={()=>setModal(null)} onConfirm={deleteThing} impact={modal.impact} description={modal.what==='project'?`${t('confirmDelete')} — ${modal.initial.name}`:`${t('confirmDelete')} — ${modal.initial.name||modal.initial.label||modal.initial.type||''}`}/>}<input ref={importRef} type="file" hidden accept=".json,application/json" onChange={e=>{importFile(e.target.files?.[0]);e.target.value=''}} /></div>
}

// API-mode authentication is established before App mounts and makes project requests.
// Keep the existing demo mode completely unchanged.
function SanaAuthGate(){
 const [user,setUser]=useState(null);
 const [showAuth,setShowAuth]=useState(false);
 const [checking,setChecking]=useState(()=>API_MODE&&authApi.hasCredentials());
 const [registering,setRegistering]=useState(false);
 const [name,setName]=useState('');
 const [email,setEmail]=useState('');
 const [password,setPassword]=useState('');
 const [busy,setBusy]=useState(false);
 const [error,setError]=useState('');
 const [lang,setLang]=useState(()=>localStorage.getItem('sana-lang')||'ar');
 const ar=lang==='ar';
 useEffect(()=>{document.documentElement.lang=lang;document.documentElement.dir=ar?'rtl':'ltr';localStorage.setItem('sana-lang',lang)},[lang,ar]);
 useEffect(()=>{
  if(!API_MODE)return;
  let active=true;
  if(!authApi.hasCredentials()){setChecking(false);return;}
  authApi.me().then(me=>{if(active){if(String(me?.role||'').toUpperCase()==='ADMIN')window.location.hash='/admin';else if(window.location.hash==='#/admin')window.location.hash='/home';setUser(me)}}).catch(()=>{if(active)authApi.forget()})
    .finally(()=>{if(active)setChecking(false)});
  return ()=>{active=false};
 },[]);
 useEffect(()=>{
  if(!API_MODE)return;
  const unauthorized=()=>{authApi.forget();setUser(null);setLang(localStorage.getItem('sana-lang')||'ar');setPassword('');setError(ar?'انتهى تسجيل الدخول. ادخلي مرة ثانية.':'Please sign in again.');};
  window.addEventListener('sana-unauthorized',unauthorized);
  return ()=>window.removeEventListener('sana-unauthorized',unauthorized);
 },[ar]);
 const submit=async e=>{
  e.preventDefault();if(busy)return;
  setBusy(true);setError('');
  try{
   const me=registering?await authApi.register(name.trim(),email.trim(),password):await authApi.login(email.trim(),password);
   setPassword('');setShowAuth(false);if(String(me?.role||'').toUpperCase()==='ADMIN')window.location.hash='/admin';else if(window.location.hash==='#/admin')window.location.hash='/home';setUser(me);
  }catch(err){setError(err.message||'Authentication failed')}
  finally{setBusy(false)}
 };
 const logout=async()=>{
  await authApi.logout();setLang(localStorage.getItem('sana-lang')||'ar');
  setUser(null);setShowAuth(false);setPassword('');
  window.location.hash='/home';
 };
 if(!API_MODE)return <App/>;
 if(checking)return <main className="sana-auth-shell" dir={ar?'rtl':'ltr'}><div className="sana-auth-card"><img src="/sana-logo.webp" alt="SANA"/><p>{ar?'جاري التحقق من الحساب...':'Checking your account...'}</p></div></main>;
 if(user)return <App key={user.id} user={user} onLogout={logout}/>;
 if(showAuth)return <AccountExperience {...{lang,setLang,registering,setRegistering,name,setName,email,setEmail,password,busy,error,setError,setPassword,submit}} onBack={()=>{setShowAuth(false);setError('');setPassword('');window.location.hash='/home';}}/>;
 return <App user={null} onLogin={()=>{setRegistering(false);setError('');setShowAuth(true);}}/>;
}

createRoot(document.getElementById('root')).render(<SanaAuthGate/>);
