import React,{useCallback,useEffect,useRef,useState} from 'react';
import {Plus,Trash2,Image as ImageIcon,Search,StickyNote,Gamepad2,GitBranch,Play,RefreshCcw,Check,Info,Sparkles,Activity,Move,LayoutGrid,ZoomIn,ZoomOut,Scan,Maximize2,Link2,X,ChevronLeft,FolderPlus,UploadCloud,BarChart3,UserRound,UsersRound,ArrowRight, ArrowLeft, Pencil, BookOpen, CircleHelp, AlertTriangle} from 'lucide-react';
import {API_MODE,api,diagramIdFor,selectedBranchingDiagram,rememberBranchingOutput} from '../lib/data';
import {creativeTools as store,MOODBOARD_API,moodboardAuthorizedImageBlobUrl} from '../lib/creativeTools';
import {moodboardReferenceUrl} from '../lib/creativeTools';
import {tr} from '../lib/i18n';
import {edgeEndpoints} from './DiagramPresentation';
import {useDialogFocus} from './StudioWorkspace';
import CharacterRadarChart from './creativity/CharacterRadarChart';
import {DialoguePlayMode,NarrativeInspector} from './creativity/NarrativeExperience';

const word=(lang,ar,en)=>lang==='ar'?ar:en;
const clamp=(n,max)=>Math.max(6,Math.min(max,Math.round(n)));
const chosenId=n=>String(n?.id??n?.characterId??'');
const defaultStats=()=>({attack:null,defense:null,speed:null,rangeStat:null,stamina:null,special:null});
function Hint({children}){return <p className="ux-hint"><Info size={15}/>{children}</p>}
function ErrorBanner({error,onClose}){return error?<div className="ux-error" role="alert"><AlertTriangle size={18}/><span>{error}</span><button onClick={onClose} aria-label="Dismiss"><X size={16}/></button></div>:null}
function Action({children,icon:Icon=Plus,light=false,...props}){return <button type="button" className={`ux-action ${light?'ux-light':''}`} {...props}><Icon size={17}/>{children}</button>}
function Drawer({title,desc,children,onClose,lang,className=''}){const dialog=useRef(null);useDialogFocus(dialog,onClose);const close=useRef(onClose);close.current=onClose;useEffect(()=>{const f=e=>{if(e.key==='Escape'&&!e.defaultPrevented)close.current()};document.addEventListener('keydown',f);return()=>document.removeEventListener('keydown',f)},[]);return <><button type="button" className="ux-drawer-shade" onClick={onClose} aria-label={word(lang,'إغلاق اللوحة','Close panel')}/><aside ref={dialog} role="dialog" aria-modal="true" aria-label={title} className={`ux-drawer ${className}`} dir={lang==='ar'?'rtl':'ltr'}><div className="ux-drawer-top"><div><h2>{title}</h2>{desc&&<p>{desc}</p>}</div><button className="ux-icon" onClick={onClose} aria-label={word(lang,'إغلاق','Close')}><X size={21}/></button></div><div className="ux-drawer-body">{children}</div></aside></>}
function ToolHeader({icon:Icon,title,subtitle,actions,lang,kicker}){return <header className="ux-tool-head" dir={lang==='ar'?'rtl':'ltr'}><div className="ux-tool-heading"><span className="ux-tool-icon"><Icon size={23}/></span><div><small>{kicker}</small><h1>{title}</h1><p>{subtitle}</p></div></div><div className="ux-tool-actions">{actions}</div></header>}
function EmptySpace({icon:Icon,title,subtitle,action}){return <div className="ux-empty-state"><span><Icon size={31}/></span><strong>{title}</strong><p>{subtitle}</p>{action}</div>}
function Labeled({label,children}){return <label className="ux-field"><span>{label}</span>{children}</label>}
function SubmitRow({busy,lang,submitLabel}){return <button type="submit" disabled={busy} className="ux-submit"><Check size={17}/>{submitLabel||word(lang,'حفظ','Save')}</button>}

function BoardReferenceImage({image,lang}){
 const raw=image?.thumbnail||image?.imageUrl||'';
 const protectedAsset=raw.startsWith('/api/v1/creativity/moodboard/images/pixabay-assets/');
 const [resolved,setResolved]=useState(protectedAsset?'':raw),[failed,setFailed]=useState(false);
 useEffect(()=>{
  let active=true,objectUrl=null;
  const controller=new AbortController();
  setFailed(false);
  if(!protectedAsset){setResolved(raw);return()=>{active=false};}
  setResolved('');
  moodboardAuthorizedImageBlobUrl(raw,controller.signal).then(url=>{
   objectUrl=url;
   if(active)setResolved(url);
   else URL.revokeObjectURL(url);
  }).catch(()=>{if(active)setFailed(true)});
  return()=>{active=false;controller.abort();if(objectUrl)URL.revokeObjectURL(objectUrl)};
 },[raw,protectedAsset]);
 if(failed)return <small>{word(lang,'تعذر تحميل الصورة المحفوظة','Could not load saved image')}</small>;
 if(!resolved)return <small>{word(lang,'جارٍ تحميل الصورة...','Loading image...')}</small>;
 return <img src={resolved} alt={image?.title||''} loading="lazy" draggable="false" referrerPolicy="no-referrer" onError={()=>setFailed(true)}/>;
}

export function Moodboard({project,lang,initialBoardId,onOpenAI}){
 const [editingLabel,setEditingLabel]=useState(null),[replacingImage,setReplacingImage]=useState(null),[noteColor,setNoteColor]=useState('#39322c'),[noteBackground,setNoteBackground]=useState('#fff0ce');
 const [boards,setBoards]=useState([]),[board,setBoard]=useState(null),[drawer,setDrawer]=useState(null),[boardTitle,setBoardTitle]=useState(''),[note,setNote]=useState(''),[url,setUrl]=useState(''),[keyword,setKeyword]=useState(''),[images,setImages]=useState([]),[loading,setLoading]=useState(true),[busy,setBusy]=useState(false),[error,setError]=useState(''),[searchState,setSearchState]=useState('');
 const drag=useRef(null),photoInput=useRef(null),boardViewport=useRef(null),pan=useRef(null);
 const [zoom,setZoom]=useState(1),[expanded,setExpanded]=useState(false);
 const refresh=useCallback(async(which)=>{const list=await store.boards(project.id);setBoards(list);const selected=list.find(b=>String(b.id)===String(which??board?.id))?.id??list[0]?.id;setBoard(selected!=null?await store.board(project.id,selected):null)},[project.id,board?.id]);
 useEffect(()=>{let active=true;setLoading(true);store.boards(project.id).then(async list=>{if(!active)return;const selected=list.find(b=>String(b.id)===String(initialBoardId))||list[0];const loaded=selected?await store.board(project.id,selected.id):null;if(active){setBoards(list);setBoard(loaded)}}).catch(e=>active&&setError(e.message)).finally(()=>active&&setLoading(false));return()=>{active=false}},[project.id,initialBoardId]);
 async function commit(action,which,close=true){setBusy(true);setError('');try{const result=await action();await refresh(which??result?.id);if(close)setDrawer(null);return result}catch(e){setError(e.message);return null}finally{setBusy(false)}}
 const place=()=>({x:65+((board?.items?.length||0)%4)*300,y:75+Math.floor((board?.items?.length||0)/4)*340});
 const stageWidth=Math.max(1800,...(board?.items||[]).map(item=>(item.positionX||0)+300));
 const stageHeight=Math.max(1200,...(board?.items||[]).map(item=>(item.positionY||0)+(item.label?120+Math.ceil((item.label.text||'').length/22)*27:370)));
 function down(e,item){if(e.button!==0||e.target.closest('button,a'))return;drag.current={id:item.id,x:item.positionX||0,y:item.positionY||0,ox:e.clientX,oy:e.clientY};e.currentTarget.setPointerCapture(e.pointerId)}
 function moving(e){const d=drag.current;if(!d)return;const x=clamp(d.x+(e.clientX-d.ox)/zoom,Math.max(1700,stageWidth-236)),y=clamp(d.y+(e.clientY-d.oy)/zoom,Math.max(1100,stageHeight-80));setBoard(b=>({...b,items:b.items.map(i=>i.id===d.id?{...i,positionX:x,positionY:y}:i)}))}
 async function up(e){const d=drag.current;if(!d)return;drag.current=null;const x=clamp(d.x+(e.clientX-d.ox)/zoom,Math.max(1700,stageWidth-236)),y=clamp(d.y+(e.clientY-d.oy)/zoom,Math.max(1100,stageHeight-80));try{await store.moveBoardItem(project.id,board.id,d.id,x,y)}catch(err){setError(err.message);await refresh(board.id)}}
 function beginPan(e){
  if(e.button!==0 || e.target!==e.currentTarget)return;
  const viewport=boardViewport.current;
  if(!viewport)return;
  pan.current={x:e.clientX,y:e.clientY,left:viewport.scrollLeft,top:viewport.scrollTop};
  e.currentTarget.setPointerCapture(e.pointerId);
 }
 function movePan(e){
  if(!pan.current || !boardViewport.current)return;
  boardViewport.current.scrollLeft=pan.current.left-(e.clientX-pan.current.x);
  boardViewport.current.scrollTop=pan.current.top-(e.clientY-pan.current.y);
 }
 function changeZoom(next,clientX,clientY){
  const limited=Math.max(.5,Math.min(1.65,Math.round(next*10)/10));
  const viewport=boardViewport.current;
  if(!viewport){setZoom(limited);return;}
  const rect=viewport.getBoundingClientRect();
  const px=clientX==null?rect.width/2:clientX-rect.left;
  const py=clientY==null?rect.height/2:clientY-rect.top;
  const originalX=(viewport.scrollLeft+px)/zoom,originalY=(viewport.scrollTop+py)/zoom;
  setZoom(limited);
  requestAnimationFrame(()=>{viewport.scrollLeft=originalX*limited-px;viewport.scrollTop=originalY*limited-py});
 }
 function zoomWheel(e){if(!(e.ctrlKey||e.metaKey||e.altKey))return;e.preventDefault();changeZoom(zoom*(e.deltaY>0?.9:1.1),e.clientX,e.clientY)}
 async function autoArrange(){
  if(!board?.items?.length||busy)return;
  const placements=board.items.map((item,i)=>({...item,positionX:60+(i%4)*310,positionY:60+Math.floor(i/4)*350}));
  setBoard(previous=>({...previous,items:placements}));
  setBusy(true);setError('');
  try{
   // Each item is saved through the existing position endpoint (no new backend table).
   for(const item of placements)await store.moveBoardItem(project.id,board.id,item.id,item.positionX,item.positionY);
   await refresh(board.id);
  }catch(e){setError(e.message);await refresh(board.id)}finally{setBusy(false)}
 }
 async function findImages(e){e.preventDefault();if(!keyword.trim())return;setSearchState('loading');setError('');try{const data=await store.searchImages(keyword.trim());setImages(data.images||[]);setSearchState((data.images||[]).length?'success':'empty')}catch(e){setSearchState('error');setError(word(lang,'تعذّر البحث في مكتبات الصور، ويمكن إضافة صورة من الجهاز بدلًا من ذلك.','Image libraries are unavailable. You can upload an image instead.'))}}
 async function saveImage(image){
  if(!board)return;
  const current=replacingImage;
  const pos=current?{x:current.positionX??0,y:current.positionY??0}:place();
  const result=await commit(async()=>{
   const added=await store.addImage(project.id,board.id,image,pos);
   // Never delete the original until the replacement is actually saved.
   if(current)await store.deleteBoardItem(project.id,board.id,current.id);
   return added;
  },board.id);
  if(result){setReplacingImage(null);setUrl('');}
 }
 async function removeBoard(){
  if(!board||busy)return;
  const message=word(lang,`حذف لوحة «${board.title}» وكل صورها وملاحظاتها؟ لا يمكن التراجع عن الحذف.`,`Delete “${board.title}” and all its contents? This cannot be undone.`);
  if(!window.confirm(message))return;
  setBusy(true);setError('');
  try{await store.deleteBoard(project.id,board.id);setReplacingImage(null);setBoard(null);await refresh('first');}
  catch(e){setError(e.message)}finally{setBusy(false)}
 }
 function exportBoardPdf(){
  if(!board)return;
  // Native browser print-to-PDF: no server changes or heavy PDF dependency.
  const printWindow=window.open('','_blank');
  if(!printWindow){setError(word(lang,'اسمح بفتح نافذة التصدير من المتصفح.','Allow popups to export as PDF.'));return;}
  printWindow.document.write(`<!doctype html><html lang="${lang==='ar'?'ar':'en'}" dir="${lang==='ar'?'rtl':'ltr'}"><head><meta charset="utf-8"><title>SANA Moodboard</title><style>
  @page{size:A4 landscape;margin:13mm}*{box-sizing:border-box}body{font-family:Tahoma,Arial,sans-serif;background:#fffdfa;color:#163e44;margin:0;padding:25px}header{border-bottom:2px solid #c9a35b;padding-bottom:16px;margin-bottom:20px}h1{margin:0;font-size:25px}header p{font-size:11px;color:#568081}.gallery{display:grid;grid-template-columns:repeat(3,1fr);gap:17px;align-items:start}.card{border:1px solid #d6e3df;border-radius:13px;overflow:hidden;padding:12px;break-inside:avoid;background:#fff}img{max-width:100%;max-height:240px;object-fit:contain;display:block;margin:auto}p{white-space:pre-wrap;overflow-wrap:anywhere;font-size:13px;line-height:1.8;margin:8px 0}small{font-size:9px;word-break:break-word;color:#47716d}footer{margin-top:18px;color:#77938e;font-size:10px}.no-print{display:none}@media print{body{padding:0}a{color:inherit;text-decoration:none}.card{box-shadow:none}}</style></head><body></body></html>`);
  printWindow.document.close();
  const doc=printWindow.document;
  const header=doc.createElement('header');const title=doc.createElement('h1');title.textContent=board.title||'SANA Moodboard';header.append(title);
  const sub=doc.createElement('p');sub.textContent=word(lang,'لوحة الإلهام · سنا','Inspiration board · SANA');header.append(sub);doc.body.append(header);
  const gallery=doc.createElement('div');gallery.className='gallery';doc.body.append(gallery);
  const entries=[...(board.items||[])].sort((a,b)=>(a.positionY??0)-(b.positionY??0)||(a.positionX??0)-(b.positionX??0));
  for(const item of entries){
   const card=doc.createElement('article');card.className='card';
   if(item.label){card.style.backgroundColor=item.label.backgroundColor||'#fff0ce';card.style.color=item.label.color||'#39322c';
    const p=doc.createElement('p');p.textContent=item.label.text||'';card.append(p);}
   else if(item.image){
    const existing=document.querySelector(`[data-sana-board-item="${item.id}"] img`);
    const img=doc.createElement('img');img.src=existing?.currentSrc||item.image.thumbnail||item.image.imageUrl||'';
    img.alt=item.image.title||'';card.append(img);
    const p=doc.createElement('p');p.textContent=item.image.title||word(lang,'صورة إلهام','Image reference');card.append(p);
    const src=doc.createElement('small');src.textContent=[item.image.provider,item.image.attribution,item.image.sourceUrl].filter(Boolean).join(' · ');card.append(src);
   }
   gallery.append(card);
  }
  const foot=doc.createElement('footer');foot.textContent='SANA · '+new Date().toLocaleDateString(lang==='ar'?'ar-SA':'en');doc.body.append(foot);
  const imgs=[...doc.images];
  Promise.all(imgs.map(img=>img.complete?Promise.resolve():new Promise(resolve=>{img.onload=resolve;img.onerror=resolve;setTimeout(resolve,4500)})))
   .then(()=>{printWindow.focus();printWindow.print()});
 }
 async function addImageFromFile(file){if(!file)return;if(!file.type.startsWith('image/')){setError(word(lang,'اختر ملف صورة صالحًا','Please choose an image'));return}if(file.size>10000000){setError(word(lang,'الصورة أكبر من 10MB؛ اختر ملفًا أصغر.','Image exceeds 10 MB; choose a smaller file.'));return}try{let dataUrl=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reject;reader.readAsDataURL(file)});if(dataUrl.length>1150000){const original=await new Promise((resolve,reject)=>{const image=new Image();image.onload=()=>resolve(image);image.onerror=reject;image.src=dataUrl});const scale=Math.min(1,1200/original.width,900/original.height);const output=document.createElement('canvas');output.width=Math.max(1,Math.round(original.width*scale));output.height=Math.max(1,Math.round(original.height*scale));const ctx=output.getContext('2d');ctx.fillStyle='#fffcf6';ctx.fillRect(0,0,output.width,output.height);ctx.drawImage(original,0,0,output.width,output.height);dataUrl=output.toDataURL('image/jpeg',0.74);if(dataUrl.length>1150000)dataUrl=output.toDataURL('image/jpeg',0.55);if(dataUrl.length>1400000)throw Error('Image too large after compression')}await saveImage({id:`local-${Date.now()}`,title:file.name,imageUrl:dataUrl,thumbnail:dataUrl})}catch(e){setError(word(lang,'تعذّر حفظ الصورة محليًا؛ جرّب صورة أصغر أو احذف ملفات تجريبية قديمة.','Could not save the image locally. Try a smaller image or clear old demo files.'))}}
 const isLocal=(item)=>Boolean(item.localOnly)||Number(item.id)<0;
 return <section className="ux-tool-page">
  <ToolHeader lang={lang} icon={ImageIcon} kicker="SANA · MOODBOARD" title={word(lang,'لوحة الإلهام','Moodboard')} subtitle={word(lang,'رتّب الصور والملاحظات في لوحة واحدة واضحة.','Arrange references and notes on one clear, spacious board.')} actions={<><Action icon={Sparkles} onClick={()=>onOpenAI?.()}>{word(lang,'سنا، تصوّري قصتي','Visualize with SANA')}</Action><Action icon={FolderPlus} light onClick={()=>setDrawer('new-board')}>{word(lang,'لوحة جديدة','New board')}</Action><Action icon={Pencil} light disabled={!board} onClick={()=>{setBoardTitle(board.title);setDrawer('rename-board')}}>{word(lang,'اسم اللوحة','Rename board')}</Action><Action icon={Trash2} light disabled={!board||busy} onClick={removeBoard}>{word(lang,'حذف اللوحة','Delete board')}</Action><Action icon={BookOpen} light disabled={!board||busy} onClick={exportBoardPdf}>{word(lang,'تصدير PDF','Export PDF')}</Action><Action icon={StickyNote} light onClick={()=>{setEditingLabel(null);setNote('');setNoteColor('#39322c');setNoteBackground('#fff0ce');setDrawer('note')}} disabled={!board}>{word(lang,'ملاحظة','Note')}</Action><Action icon={ImageIcon} onClick={()=>{setReplacingImage(null);setDrawer('image')}} disabled={!board}>{word(lang,'إضافة صورة','Add image')}</Action></>}/>
  <ErrorBanner error={error} onClose={()=>setError('')}/>
  <div className="ux-board-switch" role="tablist" aria-label={word(lang,'لوحات المشروع','Project boards')}>{boards.map(b=><button role="tab" aria-selected={String(b.id)===String(board?.id)} className={String(b.id)===String(board?.id)?'active':''} key={b.id} onClick={()=>commit(()=>Promise.resolve(null),b.id,false)}>{b.title}</button>)}</div>
  {loading?<div className="ux-loading">{word(lang,'جارٍ تحميل اللوحة...','Loading board...')}</div>:!board?<EmptySpace icon={ImageIcon} title={word(lang,'ابدأ بلوحة إلهام','Start your moodboard')} subtitle={word(lang,'أنشئ لوحة أولًا، ثم أضف الصور والملاحظات بسهولة.','Create a board, then add your visual references and notes.')} action={<Action icon={FolderPlus} onClick={()=>setDrawer('new-board')}>{word(lang,'إنشاء أول لوحة','Create first board')}</Action>}/>:<div className={`ux-stage-shell sana-mood-workspace ${expanded?'sana-mood-expanded':''}`}><div className="ux-stage-guide sana-mood-toolbar"><Move size={15}/><span>{word(lang,'حرّك الصور والملاحظات لتغيير ترتيبها، أو حرّك المساحة الفارغة للتنقل في اللوحة','Drag references to arrange; drag empty space to pan')}</span><div className="sana-mood-controls" role="toolbar" aria-label={word(lang,'أدوات اللوحة','Board controls')}><button type="button" disabled={busy} title={word(lang,'ترتيب البطاقات تلقائيًا','Arrange cards')} onClick={autoArrange}><LayoutGrid size={16}/>{word(lang,'ترتيب','Arrange')}</button><button type="button" title={word(lang,'تصغير','Zoom out')} onClick={()=>changeZoom(zoom-.1)}><ZoomOut size={16}/></button><span className="sana-mood-zoom">{Math.round(zoom*100)}%</span><button type="button" title={word(lang,'تكبير','Zoom in')} onClick={()=>changeZoom(zoom+.1)}><ZoomIn size={16}/></button><button type="button" title={word(lang,'ملاءمة اللوحة','Fit board')} onClick={()=>{const viewport=boardViewport.current;changeZoom(Math.min(1,(viewport?.clientWidth||1000)/stageWidth));if(viewport){viewport.scrollLeft=0;viewport.scrollTop=0}}}><Scan size={16}/>{word(lang,'ملاءمة','Fit')}</button><button type="button" onClick={()=>setExpanded(x=>!x)}><Maximize2 size={16}/>{expanded?word(lang,'إغلاق','Close'):word(lang,'توسيع','Expand')}</button></div></div><div ref={boardViewport} className="ux-stage-scroll sana-mood-scroll" onWheel={zoomWheel}><div className="sana-mood-zoom-space" style={{width:stageWidth*zoom,height:stageHeight*zoom}}><div className="ux-mood-stage sana-creative-mood-stage" style={{width:stageWidth,height:stageHeight,transform:`scale(${zoom})`,transformOrigin:'top left'}} onPointerDown={beginPan} onPointerMove={movePan} onPointerUp={()=>pan.current=null} onPointerCancel={()=>pan.current=null}>{!board.items?.length&&<EmptySpace icon={Sparkles} title={word(lang,'مساحة لإلهامك','Room for inspiration')} subtitle={word(lang,'أضف أول ملاحظة أو صورة من الأعلى.','Add your first note or image from above.')}/>} {(board.items||[]).map(item=><div key={item.id} className={`ux-mood-card sana-gallery-card ${item.image?'image':'note'}`} data-sana-board-item={item.id} style={{left:item.positionX??0,top:item.positionY??0,...(item.label?{'--sana-note-bg':item.label.backgroundColor||'#fff0ce','--sana-note-text':item.label.color||'#39322c'}:{})}} onPointerDown={e=>down(e,item)} onPointerMove={moving} onPointerUp={up} onPointerCancel={()=>drag.current=null} tabIndex={0} role="group" aria-label={item.image?.title||item.label?.text||'Board item'} onKeyDown={e=>{const d=e.shiftKey?24:10;const x=e.key==='ArrowRight'?d:e.key==='ArrowLeft'?-d:0,y=e.key==='ArrowDown'?d:e.key==='ArrowUp'?-d:0;if(!x&&!y)return;e.preventDefault();const nx=clamp((item.positionX??0)+x,Math.max(1700,stageWidth-236)),ny=clamp((item.positionY??0)+y,Math.max(1100,stageHeight-80));commit(()=>store.moveBoardItem(project.id,board.id,item.id,nx,ny),board.id,false)}}><div className="ux-mood-card-top"><span className="ux-mood-drag"><Move size={14}/>{word(lang,'تحريك','Move')}</span>{item.image&&<button type="button" onPointerDown={e=>e.stopPropagation()} onClick={()=>{setReplacingImage(item);setKeyword('');setImages([]);setSearchState('');setDrawer('image')}} title={word(lang,'استبدال الصورة','Replace image')} aria-label={word(lang,'استبدال الصورة','Replace image')}><RefreshCcw size={15}/></button>}{item.label&&<button onPointerDown={e=>e.stopPropagation()} onClick={()=>{setEditingLabel(item);setNote(item.label.text);setNoteColor(item.label.color||'#39322c');setNoteBackground(item.label.backgroundColor||'#fff0ce');setDrawer('note')}} aria-label={word(lang,'تعديل الملاحظة','Edit note')}><Pencil size={15}/></button>}<button onPointerDown={e=>e.stopPropagation()} onClick={()=>{if(window.confirm(word(lang,'حذف العنصر من اللوحة؟','Delete this item?')))commit(()=>store.deleteBoardItem(project.id,board.id,item.id),board.id,false)}} aria-label={word(lang,'حذف','Delete')}><X size={16}/></button></div>{item.image?<><BoardReferenceImage image={item.image} lang={lang}/><small className="ux-image-title">{item.image.title||word(lang,'صورة','Image')} {isLocal(item)?word(lang,'· محفوظة على هذا الجهاز فقط','· On this device only'):''}</small>{item.image.sourceUrl&&/^https:\/\//i.test(item.image.sourceUrl)&&<a href={item.image.sourceUrl} target="_blank" rel="noopener noreferrer" onPointerDown={e=>e.stopPropagation()} style={{fontSize:11}}>{item.image.attribution||item.image.provider||'Openverse'} · {word(lang,'المصدر','Source')}</a>}{item.image.creatorUrl&&/^https:\/\//i.test(item.image.creatorUrl)&&<a href={item.image.creatorUrl} target="_blank" rel="noopener noreferrer" onPointerDown={e=>e.stopPropagation()} style={{fontSize:11}}>{item.image.creator||word(lang,'الفنان','Artist')}</a>}</>:<p>{item.label?.text}</p>}</div>)}</div></div></div></div>}
  {drawer&&<Drawer title={drawer==='rename-board'?word(lang,'اسم اللوحة','Rename board'):drawer==='new-board'?word(lang,'إنشاء لوحة جديدة','New moodboard'):drawer==='note'?word(lang,'إضافة ملاحظة','Add a note'):replacingImage?word(lang,'استبدال الصورة','Replace image'):word(lang,'إضافة صورة','Add image')} desc={drawer==='image'?word(lang,'أضف صورة من جهازك، أو ابحث عنها، أو الصق رابطها.','Upload an image, search for one, or paste its link.'):drawer==='note'?word(lang,'يمكن تحريك الملاحظة داخل اللوحة بعد حفظها.','After saving, you can drag the note anywhere on the board.'):null} lang={lang} onClose={()=>{setDrawer(null);setReplacingImage(null)}}>
   {(drawer==='new-board'||drawer==='rename-board')?<form className="ux-form" onSubmit={e=>{e.preventDefault();if(!boardTitle.trim())return;commit(()=>drawer==='rename-board'?store.renameBoard(project.id,board.id,boardTitle.trim()):store.addBoard(project.id,boardTitle.trim())).then(r=>{if(r)setBoardTitle('')})}}><Labeled label={word(lang,'اسم اللوحة','Board name')}><input autoFocus required maxLength={80} value={boardTitle} onChange={e=>setBoardTitle(e.target.value)} placeholder={word(lang,'مثل: أجواء المدينة','e.g. City atmosphere')}/></Labeled><SubmitRow busy={busy} lang={lang}/></form>:drawer==='note'?<form className="ux-form" onSubmit={e=>{e.preventDefault();if(!note.trim())return;commit(()=>editingLabel?store.editLabel(project.id,board.id,editingLabel.id,{text:note.trim(),color:noteColor,backgroundColor:noteBackground,fontSize:editingLabel.label.fontSize||17}):store.addLabel(project.id,board.id,note.trim(),place(),{color:noteColor,backgroundColor:noteBackground}),board.id).then(r=>{if(r!==null)setNote('')})}}><Labeled label={word(lang,'نص الملاحظة','Note text')}><textarea autoFocus required rows={5} maxLength={1000} value={note} onChange={e=>setNote(e.target.value)} placeholder={word(lang,'اكتب الفكرة هنا...','Write your idea here...')}/></Labeled><div className="sana-note-palette"><Labeled label={word(lang,'لون النص','Text color')}><input type="color" value={noteColor} onChange={e=>setNoteColor(e.target.value)}/></Labeled><Labeled label={word(lang,'لون الخلفية','Background color')}><input type="color" value={noteBackground} onChange={e=>setNoteBackground(e.target.value)}/></Labeled></div><div className="sana-note-preview" style={{backgroundColor:noteBackground,color:noteColor}}>{note||word(lang,'معاينة الملاحظة','Note preview')}</div><Hint>{word(lang,'يمكن تحريك البطاقة إلى أي مكان في اللوحة بعد الحفظ.','After saving, drag the card anywhere on the board.')}</Hint><SubmitRow busy={busy} lang={lang}/></form>:<div className="ux-form"><button className="ux-upload-zone" type="button" onClick={()=>photoInput.current?.click()}><UploadCloud size={27}/><strong>{word(lang,'اختيار صورة من الجهاز','Upload from your computer')}</strong><small>{word(lang,'الصورة من الجهاز تُحفظ محليًا فقط؛ صور المكتبة تُحفظ في المشروع.','Uploads stay on this device; library images are saved to the project.')}</small></button><input ref={photoInput} type="file" hidden accept="image/*" onChange={e=>{const f=e.target.files?.[0];e.target.value='';addImageFromFile(f)}}/><div className="ux-separator">{word(lang,'أو رابط مباشر','or direct link')}</div><form onSubmit={e=>{e.preventDefault();if(!/^https:\/\//i.test(url)){setError(word(lang,'الرابط لازم يبدأ بـ https://','URL must start with https://'));return}saveImage({id:`local-${Date.now()}`,title:'Reference',imageUrl:url,thumbnail:url})}} className="ux-inline-form"><input type="url" value={url} onChange={e=>setUrl(e.target.value)} placeholder="https://example.com/image.jpg"/><button disabled={!url.trim()||busy} className="ux-submit" type="submit" title={replacingImage?word(lang,'اعتماد الصورة البديلة','Use replacement'):word(lang,'إضافة صورة','Add image')}><Plus size={17}/></button></form>{MOODBOARD_API&&<><div className="ux-separator">{word(lang,'مكتبة الصور','Image library')}</div><form className="ux-inline-form" onSubmit={findImages}><input value={keyword} onChange={e=>setKeyword(e.target.value)} placeholder={word(lang,'ابحث عن صورة مناسبة...','Search for an image...')}/><button className="ux-submit" disabled={!keyword.trim()||searchState==='loading'} type="submit"><Search size={17}/></button></form>{searchState==='loading'&&<Hint>{word(lang,'نبحث عن صور مناسبة في المكتبات...','Finding references across the libraries...')}</Hint>}{searchState==='empty'&&<Hint>{word(lang,'لا توجد نتائج، جرّب كلمة ثانية أو أضف صورة من الجهاز.','No results. Try another term or upload.')}</Hint>}{searchState==='error'&&<Hint>{word(lang,'البحث غير متاح الآن، ويمكن إضافة صورة مباشرة من الجهاز.','Search is unavailable right now, but you can upload an image from your device.')}</Hint>}<div className="ux-image-grid">{images.map((image,i)=><div className="sana-openverse-choice" key={image.id||i}>
  <img loading="lazy" src={image.thumbnail||image.imageUrl} alt={image.title||'Image result'}/>
  <button type="button" disabled={busy} onClick={()=>saveImage(image)}><Plus size={13}/>{replacingImage?word(lang,'اعتماد الصورة البديلة','Use replacement'):word(lang,'إضافة إلى اللوحة','Add to board')}</button>
  <small>{image.provider||'Art'} · {image.creator||''}</small>{image.sourceUrl&&<a href={image.sourceUrl} target="_blank" rel="noopener noreferrer" style={{fontSize:11}}>{image.attribution||word(lang,'المصدر والترخيص','Source and license')}</a>}{image.creatorUrl&&<a href={image.creatorUrl} target="_blank" rel="noopener noreferrer" style={{fontSize:11}}>{image.creator||'Photographer'}</a>}
 </div>)}</div></>}</div>}
  </Drawer>}
 </section>;
}

const BRANCH_CARD_W=242;
const BRANCH_CARD_H=230;
const BRANCH_COL_GAP=112;
const BRANCH_ROW_GAP=90;

// A layered diagram layout: stable order, no collisions, handles disconnected scenes and cycles.
// The preview is local until the user explicitly saves scene locations.
export function layoutDialogueTree(nodes=[],choices=[]){
 if(!nodes.length)return [];
 const keys=new Set(nodes.map(n=>String(n.branchingNodeId)));
 const children=new Map(nodes.map(n=>[String(n.branchingNodeId),[]]));
 const indegree=new Map(nodes.map(n=>[String(n.branchingNodeId),0]));
 const seenEdge=new Set();
 for(const c of choices){
  const from=String(c.sourceBranchingNodeId),to=String(c.targetBranchingNodeId);
  if(!keys.has(from)||!keys.has(to)||from===to||seenEdge.has(`${from}/${to}`))continue;
  seenEdge.add(`${from}/${to}`);children.get(from).push(to);indegree.set(to,indegree.get(to)+1);
 }
 const ordered=[...nodes].sort((a,b)=>Number(a.positionY||0)-Number(b.positionY||0)||Number(a.positionX||0)-Number(b.positionX||0)||String(a.branchingNodeId).localeCompare(String(b.branchingNodeId)));
 const roots=ordered.filter(n=>indegree.get(String(n.branchingNodeId))===0).map(n=>String(n.branchingNodeId));
 const queue=roots.map(id=>[id,0]);
 const depths=new Map(),rows=new Map();
 // Ensure loop-only and disconnected components are still placed exactly once.
 const visitQueue=()=>{for(let i=0;i<queue.length;i++){
  const [id,depth]=queue[i];if(depths.has(id))continue;
  depths.set(id,depth);const group=rows.get(depth)||[];group.push(id);rows.set(depth,group);
  for(const target of children.get(id)||[])if(!depths.has(target))queue.push([target,depth+1]);
 }};
 visitQueue();
 for(const n of ordered){const id=String(n.branchingNodeId);if(!depths.has(id)){queue.push([id,0]);visitQueue()}}
 const count=Math.max(1,...[...rows.values()].map(group=>group.length));
 const positions=new Map();
 for(const [depth,ids] of rows){
  const offset=(count-ids.length)*(BRANCH_CARD_H+BRANCH_ROW_GAP)/2;
  ids.forEach((id,i)=>positions.set(id,{
   positionX:70+depth*(BRANCH_CARD_W+BRANCH_COL_GAP),
   positionY:65+offset+i*(BRANCH_CARD_H+BRANCH_ROW_GAP)
  }));
 }
 return nodes.map(n=>({...n,...positions.get(String(n.branchingNodeId))}));
}
function dialogueTreeHasOverlaps(nodes=[]){
 if(!nodes.length)return false;
 if(nodes.some(n=>!Number.isFinite(Number(n.positionX))||!Number.isFinite(Number(n.positionY))||Number(n.positionX)<0||Number(n.positionY)<0))return true;
 for(let i=0;i<nodes.length;i++)for(let j=i+1;j<nodes.length;j++){
  const a=nodes[i],b=nodes[j];
  if(Math.abs(Number(a.positionX)-Number(b.positionX))<BRANCH_CARD_W+24&&
     Math.abs(Number(a.positionY)-Number(b.positionY))<BRANCH_CARD_H+30)return true;
 }
 return false;
}
const NODE_TYPES=['DIALOGUE','DECISION','EVENT','ENDING'];
const NAMES={DIALOGUE:['حوار','Dialogue'],DECISION:['قرار','Decision'],EVENT:['حدث','Event'],ENDING:['نهاية','Ending']};
export function BranchingNarrative({project,lang,onOpenAI}){
 const branchOptions=(project.diagrams||[]).filter(d=>d.outputType==='BRANCHING_STORY').sort((a,b)=>Number(b.id)-Number(a.id));
 const [selectedOutputId,setSelectedOutputId]=useState(()=>selectedBranchingDiagram(project)?.id||null);
 const [fetching,setFetching]=useState(false),refreshVersion=useRef(0);
 const [toolReady,setToolReady]=useState(!API_MODE||project.diagrams?.some(d=>d.outputType==='BRANCHING_STORY'));const [inspecting,setInspecting]=useState(null),[previewStart,setPreviewStart]=useState(null),[playing,setPlaying]=useState(false),[layoutDraft,setLayoutDraft]=useState(false),[branchZoom,setBranchZoom]=useState(1);const branchScroll=useRef(null),playSession=useRef(0);
 const [map,setMap]=useState({nodes:[],choices:[]}),[drawer,setDrawer]=useState(null),[editing,setEditing]=useState(null),[type,setType]=useState('DIALOGUE'),[title,setTitle]=useState(''),[content,setContent]=useState(''),[source,setSource]=useState(''),[target,setTarget]=useState(''),[choiceText,setChoiceText]=useState(''),[preview,setPreview]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const [storyCharacters,setStoryCharacters]=useState([]),[sceneSpeakers,setSceneSpeakers]=useState({}),[sceneSpeaker,setSceneSpeaker]=useState('');
 const drag=useRef(null),branchPan=useRef(null),[linkDraft,setLinkDraft]=useState(null),linkRef=useRef(null),node=id=>map.nodes.find(n=>String(n.branchingNodeId)===String(id));
 function linkCoords(e){const stage=branchScroll.current?.querySelector('.ux-branch-stage');if(!stage)return null;const rect=stage.getBoundingClientRect();return {x:(e.clientX-rect.left)/branchZoom,y:(e.clientY-rect.top)/branchZoom}}
 function startLink(e,n){if(e.button!==0||busy||layoutDraft)return;e.preventDefault();e.stopPropagation();const rect=e.currentTarget.getBoundingClientRect();const stage=branchScroll.current?.querySelector('.ux-branch-stage')?.getBoundingClientRect();const start={source:String(n.branchingNodeId),x1:stage?(rect.left+rect.width/2-stage.left)/branchZoom:(n.positionX||0)+BRANCH_CARD_W,y1:stage?(rect.top+rect.height/2-stage.top)/branchZoom:(n.positionY||0)+BRANCH_CARD_H/2};linkRef.current=start;const point=linkCoords(e)||{x:start.x1,y:start.y1};setLinkDraft({...start,x2:point.x,y2:point.y});e.currentTarget.setPointerCapture(e.pointerId)}
 function moveLink(e){if(!linkRef.current)return;e.stopPropagation();const point=linkCoords(e);if(point)setLinkDraft(old=>old?{...old,x2:point.x,y2:point.y}:null)}
 function endLink(e){if(!linkRef.current)return;e.preventDefault();e.stopPropagation();const start=linkRef.current;linkRef.current=null;setLinkDraft(null);const target=e.type==='pointercancel'?null:document.elementFromPoint(e.clientX,e.clientY)?.closest('[data-branch-id]');const targetId=target?.dataset.branchId;if(!targetId||targetId===start.source)return;setSource(start.source);setTarget(targetId);setChoiceText('');setDrawer('choice')}

 const refresh=useCallback(async(showSavedPositions=false)=>{
  const version=++refreshVersion.current;
  setFetching(true);
  try{
   const [m,characters,speakers]=await Promise.all([store.branching(project),store.storyProfiles(project).catch(()=>[]),store.branchSpeakers(project).catch(()=>({}))]);
   if(version!==refreshVersion.current)return;
   const diagram=m||{nodes:[],choices:[]};const overlap=!showSavedPositions&&dialogueTreeHasOverlaps(diagram.nodes||[]);setMap(overlap?{...diagram,nodes:layoutDialogueTree(diagram.nodes,diagram.choices||[])}:diagram);setLayoutDraft(overlap);setStoryCharacters(characters||[]);setSceneSpeakers(speakers||{});setError('');
  }catch(err){
   if(version===refreshVersion.current)setError(err.message);
  }finally{
   if(version===refreshVersion.current)setFetching(false);
  }
 },[project,selectedOutputId]);
 useEffect(()=>{if(toolReady)refresh()},[refresh,toolReady]);
 function chooseTree(id){
  rememberBranchingOutput(project.id,id);
  setSelectedOutputId(Number(id));
  setMap({nodes:[],choices:[]});setLayoutDraft(false);
  setPreview(null);setInspecting(null);setDrawer(null);setError('');
 }
 async function commit(action,close=true){setBusy(true);setError('');try{await action();await refresh();if(close){setDrawer(null);setEditing(null)}return true}catch(e){setError(e.message);return false}finally{setBusy(false)}}
 function prepareScene(n){setEditing(n||null);setTitle(n?.title||'');setContent(n?.content||'');setType(n?.type||'DIALOGUE');setSceneSpeaker(n?String(sceneSpeakers[String(n.projectNodeId)]||''):'');setDrawer('scene')}
 function prepareChoice(n){setSource(n?.branchingNodeId?String(n.branchingNodeId):String(map.nodes[0]?.branchingNodeId||''));setTarget(String(map.nodes.find(x=>x.branchingNodeId!==n?.branchingNodeId)?.branchingNodeId||''));setChoiceText('');setDrawer('choice')}
 function down(e,n){if(e.button!==0||e.target.closest('button'))return;drag.current={id:n.branchingNodeId,x:n.positionX||0,y:n.positionY||0,sx:e.clientX,sy:e.clientY,moved:false};e.currentTarget.setPointerCapture(e.pointerId)}
 function moving(e){const d=drag.current;if(!d)return;const dx=(e.clientX-d.sx)/branchZoom,dy=(e.clientY-d.sy)/branchZoom;if(Math.abs(dx)+Math.abs(dy)>5)d.moved=true;if(d.moved)setMap(old=>({...old,nodes:old.nodes.map(n=>n.branchingNodeId===d.id?{...n,positionX:Math.max(6,Math.round(d.x+dx)),positionY:Math.max(6,Math.round(d.y+dy))}:n)}))}
 async function up(e){const d=drag.current;drag.current=null;if(!d?.moved)return;if(layoutDraft){setMap(old=>({...old,nodes:old.nodes.map(n=>n.branchingNodeId===d.id?{...n,positionX:Math.max(6,Math.round(d.x+(e.clientX-d.sx)/branchZoom)),positionY:Math.max(6,Math.round(d.y+(e.clientY-d.sy)/branchZoom))}:n)}));return;}try{await store.moveScene(project,d.id,Math.max(6,Math.round(d.x+(e.clientX-d.sx)/branchZoom)),Math.max(6,Math.round(d.y+(e.clientY-d.sy)/branchZoom)))}catch(err){setError(err.message);await refresh()}}
 async function beginPreview(n){if(playing||!n)return;const session=++playSession.current;setPlaying(true);setPreviewStart(n);setError('');try{let result;if(API_MODE){const outputId=await diagramIdFor(project,'BRANCHING_STORY');result=await api(`/api/v1/branching/play/${outputId}/start/${n.branchingNodeId}`)}else result=localPreview(n,{});if(session===playSession.current)setPreview(result)}catch(e){if(session===playSession.current)setError(e.message)}finally{if(session===playSession.current)setPlaying(false)}}
 function localPreview(n,state){return {scene:{...n,choices:map.choices.filter(c=>String(c.sourceBranchingNodeId)===String(n.branchingNodeId)).map(c=>({choiceId:c.choiceId,text:c.text,targetBranchingNodeId:c.targetBranchingNodeId})),ending:n.type==='ENDING'},variables:state}}
 async function follow(choice){if(playing||!preview)return;const session=playSession.current;setPlaying(true);setError('');try{let result;if(API_MODE){const outputId=await diagramIdFor(project,'BRANCHING_STORY');result=await api(`/api/v1/branching/play/${outputId}/${preview.scene.branchingNodeId}/choose/${choice.choiceId}`,{method:'POST',body:JSON.stringify({variables:preview.variables||{}})})}else{const next=node(choice.targetBranchingNodeId);result=next?localPreview(next,preview.variables||{}):null}if(session===playSession.current)setPreview(result)}catch(e){if(session===playSession.current)setError(e.message)}finally{if(session===playSession.current)setPlaying(false)}}
 const connected=(map.nodes||[]).length>0;
 const branchW=Math.max(1100,...map.nodes.map(n=>(n.positionX||0)+BRANCH_CARD_W+140)),branchH=Math.max(740,...map.nodes.map(n=>(n.positionY||0)+BRANCH_CARD_H+125));
 function zoomBranch(next,clientX,clientY){const box=branchScroll.current;const z=Math.max(.3,Math.min(1.7,Math.round(next*100)/100));if(!box){setBranchZoom(z);return}const r=box.getBoundingClientRect(),px=(clientX??r.left+r.width/2)-r.left,py=(clientY??r.top+r.height/2)-r.top;const oldX=(box.scrollLeft+px)/branchZoom,oldY=(box.scrollTop+py)/branchZoom;setBranchZoom(z);requestAnimationFrame(()=>{box.scrollLeft=oldX*z-px;box.scrollTop=oldY*z-py})}
 function wheelBranch(e){if(!(e.ctrlKey||e.metaKey||e.altKey))return;e.preventDefault();zoomBranch(branchZoom*(e.deltaY>0?.91:1.09),e.clientX,e.clientY)}
 function branchPanDown(e){if(e.button!==0&&e.button!==1)return;if(e.target.closest('button,input,.ux-scene-card'))return;const box=branchScroll.current;if(!box)return;e.preventDefault();branchPan.current={startX:e.clientX,startY:e.clientY,left:box.scrollLeft,top:box.scrollTop};e.currentTarget.setPointerCapture(e.pointerId)}
 function branchPanMove(e){const p=branchPan.current,box=branchScroll.current;if(!p||!box)return;box.scrollLeft=p.left-(e.clientX-p.startX);box.scrollTop=p.top-(e.clientY-p.startY)}
 function arrange(){if(!map.nodes.length)return;setMap(old=>({...old,nodes:layoutDialogueTree(old.nodes,old.choices)}));setLayoutDraft(true);const box=branchScroll.current;if(box)requestAnimationFrame(()=>box.scrollTo({left:0,top:0,behavior:'smooth'}))}


 if(!toolReady)return <ToolDiagramGate lang={lang} onCreate={async()=>{await diagramIdFor(project,'BRANCHING_STORY',true);setToolReady(true)}}/>;
 return <section className="ux-tool-page"><ToolHeader lang={lang} icon={GitBranch} kicker="SANA · GAME NARRATIVE" title={word(lang,'شجرة الحوارات','Dialogue tree')} subtitle={word(lang,'أنشئ المشاهد، واربطها باختيارات اللاعب، ثم جرّب المسارات.','Create scenes, connect player choices, then play through the story.')} actions={<><Action icon={Sparkles} light onClick={onOpenAI}>{word(lang,'مساعدة سنا في الحوارات','Create with SANA')}</Action><Action icon={BookOpen} light onClick={async()=>{try{const text=await store.exportYarn(project);const url=URL.createObjectURL(new Blob([text],{type:'text/plain;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download='sana-dialogue.yarn';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000)}catch(e){setError(e.message)}}}>{tr(lang,'exportYarn')}</Action><Action icon={Plus} light onClick={()=>prepareScene(null)}>{word(lang,'مشهد جديد','New scene')}</Action><Action icon={Link2} onClick={()=>prepareChoice(null)} disabled={map.nodes.length<2}>{word(lang,'ربط اختيار','Connect choice')}</Action></>}/>
 {API_MODE&&<div className="sana-branch-tree-picker" style={{display:'flex',alignItems:'center',gap:12,flexWrap:'wrap',padding:'12px 16px',borderRadius:14,background:'#f4faf8',border:'1px solid #cbded8',marginBottom:12}}>
 {branchOptions.length>1&&<label style={{display:'flex',alignItems:'center',gap:8,color:'#155861',fontWeight:700}}>{word(lang,'شجرة الحوارات','Dialogue tree')}
 <select aria-label={word(lang,'اختر شجرة الحوارات','Select dialogue tree')} value={selectedOutputId||''} onChange={e=>chooseTree(e.target.value)} style={{borderRadius:8,border:'1px solid #9abcb5',padding:'7px 12px',maxWidth:'min(50vw,330px)',background:'#fff',color:'#15414c'}}>
 {branchOptions.map((d,i)=><option value={d.id} key={d.id}>{d.title||word(lang,'شجرة حوارات','Dialogue tree')} {i===0?'✦':''}</option>)}
 </select></label>}
 <button type="button" disabled={fetching} onClick={refresh} style={{display:'inline-flex',alignItems:'center',gap:6,background:'#e0f2ef',color:'#155861',border:'1px solid #a4cdc3',borderRadius:9,padding:'8px 13px',cursor:'pointer'}}><RefreshCcw size={15}/>{word(lang,'تحديث الشجرة','Refresh tree')}</button>
 {fetching&&<span role="status" style={{color:'#46746b'}}>{word(lang,'جارٍ تحميل المشاهد...','Loading scenes...')}</span>}
 </div>}
 <ErrorBanner error={error} onClose={()=>setError('')}/><div className="ux-steps" aria-label={word(lang,'خطوات الاستخدام','How to use')}><span><b>1</b>{word(lang,'أنشئ مشهدًا','Create scene')}</span><span><b>2</b>{word(lang,'اسحب رمز الربط من مشهد إلى آخر، ثم سمّ الاختيار','Drag the link handle to another scene, then name the choice')}</span><span><b>3</b>{word(lang,'اضغط تشغيل لتجربة القصة','Press play to test')}</span></div>
 <div className="ux-stage-shell"><div className="ux-stage-guide"><button type="button" disabled={busy||!map.nodes.length} onClick={arrange}><><LayoutGrid size={15}/>{word(lang,'ترتيب تلقائي','Auto arrange')}</></button>{layoutDraft&&<><button disabled={busy} onClick={()=>commit(()=>store.saveBranchLayout(project,map.nodes),false).then(ok=>{if(ok)setLayoutDraft(false)})}>{word(lang,'حفظ أماكن المشاهد','Save positions')}</button><button disabled={busy} onClick={()=>{refresh(true).catch(e=>setError(e.message))}}>{word(lang,'التراجع','Undo')}</button></>}<button onClick={()=>zoomBranch(branchZoom-.1)} aria-label={lang==='ar'?'تصغير':'Zoom out'}>−</button><span>{Math.round(branchZoom*100)}%</span><button onClick={()=>zoomBranch(branchZoom+.1)} aria-label={lang==='ar'?'تكبير':'Zoom in'}>+</button><button onClick={()=>{const box=branchScroll.current;if(box){setBranchZoom(Math.max(.5,Math.min(1,box.clientWidth/branchW,box.clientHeight/branchH)));box.scrollTo({left:0,top:0})}}}>{word(lang,'إظهار الشجرة','Fit tree')}</button><Move size={15}/>{word(lang,'اسحب المشاهد لترتيبها، وافتح تعديل أو تشغيل من البطاقة.','Drag scenes to arrange them. Edit or play from each card.')}</div><div ref={branchScroll} className="ux-stage-scroll sana-branch-scroll" onWheel={wheelBranch} onPointerDown={branchPanDown} onPointerMove={branchPanMove} onPointerUp={()=>branchPan.current=null} onPointerCancel={()=>branchPan.current=null}><div style={{width:branchW*branchZoom,height:branchH*branchZoom,position:'relative'}}><div className="ux-branch-stage" style={{width:branchW,height:branchH,transform:`scale(${branchZoom})`,transformOrigin:'top left'}}>{!connected&&!fetching&&<EmptySpace icon={GitBranch} title={word(lang,'ابدأ بأول مشهد','Start with your first scene')} subtitle={word(lang,'مثلاً: البطل يقابل حارس البوابة. بعده أضف مشهدًا ثانيًا وخيارًا يربطهما.','For example, the hero meets a gatekeeper. Add another scene and connect them.')} action={<Action onClick={()=>prepareScene(null)}>{word(lang,'إنشاء مشهد','Create scene')}</Action>}/>}<svg width={branchW} height={branchH} className="ux-branch-svg" aria-hidden="true"><defs><marker id="uxArrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto"><path d="M0 0 L10 5 L0 10" fill="#B48945"/></marker></defs>{map.choices.map(c=>{const a=node(c.sourceBranchingNodeId),b=node(c.targetBranchingNodeId);if(!a||!b)return null;const {x1,y1,x2,y2}=edgeEndpoints({x:a.positionX||0,y:a.positionY||0},{x:b.positionX||0,y:b.positionY||0},BRANCH_CARD_W,BRANCH_CARD_H);const bend=Math.max(45,Math.min(140,Math.abs(x2-x1)*.42));const dir=x2>=x1?1:-1;return <g key={c.choiceId}><title>{c.text}</title><path d={`M ${x1} ${y1} C ${x1+dir*bend} ${y1}, ${x2-dir*bend} ${y2}, ${x2} ${y2}`} fill="none" stroke="#b48945" strokeWidth="2.5" markerEnd="url(#uxArrow)"/>{branchZoom>=.8&&<text x={(x1+x2)/2} y={(y1+y2)/2-11} className="ux-edge-text" textAnchor="middle">{String(c.text||'').length>24?String(c.text).slice(0,23)+'…':c.text}</text>}</g>})}{linkDraft&&<path className="sana-branch-link-preview" d={`M ${linkDraft.x1} ${linkDraft.y1} Q ${(linkDraft.x1+linkDraft.x2)/2} ${(linkDraft.y1+linkDraft.y2)/2-32} ${linkDraft.x2} ${linkDraft.y2}`} fill="none" stroke="#bd9b56" strokeWidth="3" strokeDasharray="8 5" markerEnd="url(#uxArrow)"/>}</svg>{map.nodes.map(n=><article key={n.branchingNodeId} data-branch-id={n.branchingNodeId} className={`ux-scene-card ux-${String(n.type).toLowerCase()}`} style={{left:n.positionX||0,top:n.positionY||0}} onPointerDown={e=>down(e,n)} onPointerMove={moving} onPointerUp={up} onPointerCancel={()=>drag.current=null}><button type="button" className="sana-dialogue-connect" title={word(lang,'اسحب لربط هذا الحوار بحوار آخر','Drag to connect another scene')} aria-label={word(lang,'ربط المشهد بالسحب','Drag to connect scene')} onPointerDown={e=>startLink(e,n)} onPointerMove={moveLink} onPointerUp={endLink} onPointerCancel={endLink} onClick={e=>e.stopPropagation()}><Link2 size={16}/></button><div className="sana-narrative-scene-heading"><span className="sana-narrative-scene-symbol" aria-hidden="true">{{DIALOGUE:'❞',DECISION:'◇',EVENT:'✦',ENDING:'✧'}[n.type]||'✦'}</span><small>{word(lang,...(NAMES[n.type]||NAMES.EVENT))}</small></div><strong>{n.title}</strong>{storyCharacters.find(c=>String(c.entityId)===String(sceneSpeakers[String(n.projectNodeId)]))&&<small style={{color:'#36746b',fontWeight:700}}>{word(lang,'المتحدث: ','Speaker: ')}{storyCharacters.find(c=>String(c.entityId)===String(sceneSpeakers[String(n.projectNodeId)]))?.name}</small>}<p>{n.content}</p><span className="sana-scene-branch-count">{map.choices.filter(c=>String(c.sourceBranchingNodeId)===String(n.branchingNodeId)).length} {word(lang,'اختيارات','choices')}</span><div className="ux-scene-card-actions"><button onClick={()=>prepareScene(n)}><Pencil size={14}/>{word(lang,'تعديل','Edit')}</button><button onClick={()=>setInspecting({node:n})}><Link2 size={14}/>{word(lang,'تفاصيل إضافية','More details')}</button><button disabled={playing} onClick={()=>beginPreview(n)}><Play size={14}/>{word(lang,'تجربة','Play')}</button><button onClick={()=>{if(window.confirm(word(lang,'حذف المشهد؟ احذف الروابط المتصلة به أولًا.','Delete scene? Remove connected choices first.')))commit(()=>store.deleteScene(project,n.branchingNodeId),false)}} aria-label={word(lang,'حذف المشهد','Delete scene')}><Trash2 size={14}/></button></div></article>)}</div></div></div></div>
 {map.choices.length>0&&<div className="ux-connections"><strong>{word(lang,'الاختيارات المتصلة','Connected choices')} · {map.choices.length}</strong><div>{map.choices.map(c=><div className="ux-connected" key={c.choiceId}><span>{node(c.sourceBranchingNodeId)?.title||'—'} <ArrowRight size={13}/> <button type="button" onClick={()=>setInspecting({choice:c})}><b>{c.text}</b></button> <ArrowRight size={13}/> {node(c.targetBranchingNodeId)?.title||'—'}</span><button onClick={()=>{if(window.confirm(word(lang,'حذف هذا الاختيار؟','Delete choice?')))commit(()=>store.deleteChoice(project,c.choiceId),false)}} aria-label={word(lang,'حذف الاختيار','Delete choice')}><X size={15}/></button></div>)}</div></div>}
 {inspecting&&<Drawer lang={lang} title={inspecting.choice?word(lang,'تفاصيل الاختيار','Choice details'):word(lang,'تفاصيل إضافية','More details')} onClose={()=>setInspecting(null)}><NarrativeInspector project={project} {...inspecting} nodes={map.nodes} variables={[]} hideVariables lang={lang} onSaved={refresh}/></Drawer>}
 {drawer&&<Drawer lang={lang} onClose={()=>{setDrawer(null);setEditing(null)}} title={drawer==='scene'?(editing?word(lang,'تعديل المشهد','Edit scene'):word(lang,'إضافة حوار','Add dialogue')):drawer==='choice'?word(lang,'ربط مشهدين','Connect scenes'):word(lang,'خيارات إضافية','Extra options')} desc={drawer==='choice'?word(lang,'حرّك علامة الربط من مشهد إلى آخر، أو اختر المشهدين من القائمة هنا.','Drag the connector from one scene to another, or choose both scenes here.'):drawer==='scene'?word(lang,'أضف عنوانًا واضحًا ثم اكتب ما يحدث داخل هذا الجزء من القصة.','Add a clear title, then write what happens in this part of the story.'):null}>
  {drawer==='scene'?<form className="ux-form" onSubmit={e=>{e.preventDefault();if(!title.trim()||!content.trim())return;commit(async()=>{
   if(editing){
    await store.updateScene(project,{...editing,title:title.trim(),content:content.trim(),type});
    const oldSpeaker=String(sceneSpeakers[String(editing.projectNodeId)]||'');
    if(sceneSpeaker!==oldSpeaker){
     if(sceneSpeaker)await store.linkSpeaker(project,editing.branchingNodeId,Number(sceneSpeaker));
     else await store.unlinkSpeaker(project,editing.branchingNodeId);
    }
   }else await store.addScene(project,{title:title.trim(),content:content.trim(),type,positionX:70+(map.nodes.length%4)*245,positionY:75+Math.floor(map.nodes.length/4)*180});
  })}}><Labeled label={word(lang,'نوع الحوار','Scene type')}><select value={type} onChange={e=>setType(e.target.value)}>{NODE_TYPES.map(k=><option key={k} value={k}>{word(lang,...NAMES[k])}</option>)}</select></Labeled><Labeled label={word(lang,'العنوان','Title')}><input autoFocus required maxLength={150} value={title} onChange={e=>setTitle(e.target.value)} placeholder={word(lang,'مثل: لقاء الملك','e.g. Meeting the king')}/></Labeled><Labeled label={word(lang,'ماذا يحدث هنا؟','What happens here?')}><textarea required rows={5} maxLength={2500} value={content} onChange={e=>setContent(e.target.value)} placeholder={word(lang,'اكتب الحوارات أو وصف الحدث...','Write dialogue or describe the event...')}/></Labeled>
 {editing?<Labeled label={word(lang,'المتحدث من شخصيات المشروع','Speaker from project characters')}><select value={sceneSpeaker} onChange={e=>setSceneSpeaker(e.target.value)}><option value="">{word(lang,'بدون متحدث محدد','No linked speaker')}</option>{storyCharacters.map(c=><option key={c.entityId} value={c.entityId}>{c.name}</option>)}</select></Labeled>:<Hint>{word(lang,'بعد حفظ المشهد، يمكن فتح التعديل وربط المتحدث بشخصية محفوظة.','After saving, edit the scene to link a saved character as speaker.')}</Hint>}
 <SubmitRow busy={busy} lang={lang}/></form>:drawer==='choice'?<form className="ux-form" onSubmit={e=>{e.preventDefault();if(!source||!target||source===target||!choiceText.trim()){setError(word(lang,'اختر مشهدين مختلفين واكتب نص الاختيار','Choose two different scenes and enter the choice text'));return}commit(()=>store.addChoice(project,Number(source),Number(target),choiceText.trim(),map.choices.length))}}><Labeled label={word(lang,'من المشهد','From scene')}><select required value={source} onChange={e=>setSource(e.target.value)}><option value="">{word(lang,'اختر المصدر','Choose source')}</option>{map.nodes.map(n=><option key={n.branchingNodeId} value={n.branchingNodeId}>{n.title}</option>)}</select></Labeled><Labeled label={word(lang,'نص الاختيار الذي يراه اللاعب','Choice shown to player')}><input value={choiceText} onChange={e=>setChoiceText(e.target.value)} required maxLength={220} placeholder={word(lang,'مثل: أوافق على المهمة','e.g. Accept the quest')}/></Labeled><Labeled label={word(lang,'ينتقل إلى المشهد','Leads to scene')}><select required value={target} onChange={e=>setTarget(e.target.value)}><option value="">{word(lang,'اختر الوجهة','Choose destination')}</option>{map.nodes.map(n=><option key={n.branchingNodeId} value={n.branchingNodeId}>{n.title}</option>)}</select></Labeled><Hint>{word(lang,'إذا لم يظهر المشهد المطلوب، أنشئه أولًا ثم اربط الاختيار.','Create the destination scene first if it is not listed.')}</Hint><SubmitRow busy={busy} lang={lang} submitLabel={word(lang,'حفظ الربط','Save connection')}/></form>:null}
 </Drawer>}
 {preview&&<DialoguePlayMode preview={preview} onChoose={follow} onClose={()=>{playSession.current++;setPlaying(false);setPreview(null)}} onRestart={()=>beginPreview(previewStart)} busy={playing} lang={lang} project={project} error={error}/>}
 </section>
}

const STAT_KEYS=['attack','defense','speed','rangeStat','stamina','special'];
const STAT_NAMES={attack:['الهجوم','Attack'],defense:['الدفاع','Defense'],speed:['السرعة','Speed'],rangeStat:['المدى','Range'],stamina:['التحمل','Stamina'],special:['مهارة خاصة','Special']};
export function GameBalance({project,lang}){
 const [toolReady,setToolReady]=useState(!API_MODE||project.diagrams?.some(d=>d.outputType==='STORY_MAP'));
 const [details,setDetails]=useState({}),[storyProfiles,setStoryProfiles]=useState([]),[profileError,setProfileError]=useState('');
 const [aiDraft,setAiDraft]=useState(null),[aiBusy,setAiBusy]=useState(false),[aiFocus,setAiFocus]=useState(''),[compare,setCompare]=useState(null);
 const [fighters,setFighters]=useState([]),[drawer,setDrawer]=useState(null),[edit,setEdit]=useState(null),[profile,setProfile]=useState(''),[characterMode,setCharacterMode]=useState('new'),[name,setName]=useState(''),[role,setRole]=useState(''),[stats,setStats]=useState(defaultStats),[report,setReport]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const profiles=API_MODE?storyProfiles:(project.elements||[]).filter(e=>e.type==='characters'&&e.name);
 const usedProfileIds=new Set(fighters.map(f=>f.characterProfileId).filter(id=>id!==null&&id!==undefined&&String(id)!=='').map(String));
 const availableProfiles=profiles.filter(p=>!usedProfileIds.has(String(p.entityId??p.id))||(edit&&String(edit.characterProfileId??'')===String(p.entityId??p.id)));
 useEffect(()=>{if(!toolReady)return;let active=true;store.storyProfiles(project).then(rows=>{if(active){setStoryProfiles(rows);setProfileError('')}}).catch(e=>{if(active)setProfileError(e.message)});return()=>{active=false}},[toolReady,project.id,project.activeOutputId]);
 const refresh=useCallback(async()=>{
  const [gameplay,originals]=await Promise.all([store.fighters(project),store.storyProfiles(project)]);
  setFighters(gameplay);setStoryProfiles(originals);
 },[project]);
 useEffect(()=>{if(toolReady)refresh().catch(e=>setError(e.message))},[refresh,toolReady]);
 async function commit(action){setBusy(true);setError('');try{await action();await refresh();setDrawer(null);setEdit(null);return true}catch(e){setError(e.message);return false}finally{setBusy(false)}}
 function formOpen(f){setEdit(f||null);setName(f?.name||'');setRole(f?.role||'');setProfile(f?.characterProfileId?String(f.characterProfileId):'');setCharacterMode(f?.characterProfileId?'story':'new');setStats(Object.fromEntries(STAT_KEYS.map(k=>[k,f?.[k]??null])));setDetails(Object.fromEntries(['weaponName','weaponType','strengths','weaknesses'].map(k=>[k,f?.[k]??''])));setDrawer('character')}
 function selectStoryProfile(id){
  setProfile(id);
  const selected=availableProfiles.find(p=>String(p.entityId??p.id)===String(id));
  if(selected&&!edit){setName(selected.name);setRole(selected.role||'')}
 }
 async function saveCharacter(e){
  e.preventDefault();
  const chosenProfile=characterMode==='story'?profile:'';
  if(characterMode==='story'&&!chosenProfile){setError(word(lang,'اختر شخصية قصصية قبل الحفظ.','Choose a story character before saving.'));return}
  if(chosenProfile&&fighters.some(f=>String(f.characterProfileId??'')===String(chosenProfile)&&chosenId(f)!==chosenId(edit))){setError(word(lang,'هذه الشخصية مرتبطة مسبقًا بشخصية لعبة.','This story character is already linked to a playable character.'));return}
  const data={name:name.trim(),role:role.trim(),...details,...stats};
  if(!data.name)return;
  await commit(async()=>{
   if(edit){
    await store.updateFighter(project,edit.id??edit.characterId,data);
    if(chosenProfile&&String(chosenProfile)!==String(edit.characterProfileId||''))await store.linkFighter(project,edit.id??edit.characterId,Number(chosenProfile));
    if(!chosenProfile&&edit.characterProfileId)await store.unlinkFighter(project,edit.id??edit.characterId);
    return;
   }
   const before=await store.fighters(project);
   await store.addFighter(project,data);
   if(chosenProfile){
    const after=await store.fighters(project);
    const old=new Set(before.map(chosenId));
    const created=after.find(c=>!old.has(chosenId(c)));
    if(!created)throw Error(word(lang,'تم إنشاء الشخصية، لكن تعذر ربطها تلقائيًا. يمكن فتحها للتعديل وإكمال الربط.','Character created, but automatic linking failed. Edit it to complete the link.'));
    await store.linkFighter(project,created.id??created.characterId,Number(chosenProfile));
   }
  });
 }
 async function analyze(){setBusy(true);setError('');try{setReport(await store.analyzeFighters(project))}catch(e){setError(e.message)}finally{setBusy(false)}}
 async function suggestBalance(){
  setAiBusy(true);setError('');setAiDraft(null);setCompare(null);
  try{
   const base=`/api/projects/${project.id}/creativity/assist`;
   const outputId=await diagramIdFor(project,'STORY_MAP');
   let draft=await api(`${base}/balance`,{method:'POST',body:JSON.stringify({focus:aiFocus.trim(),outputId:String(outputId)})});setAiDraft(draft);
   for(let i=0;i<45&&!['PENDING','FAILED','APPLIED'].includes(draft.status);i++){
    await new Promise(done=>setTimeout(done,2000));
    draft=await api(`${base}/drafts/${encodeURIComponent(draft.draftId)}`);setAiDraft(draft);
   }
   if(draft.status==='FAILED')setError(draft.error||'AI request failed');
   else if(draft.status!=='PENDING')setError(word(lang,'لم تنتهِ سنا من تحليل الشخصيات. يُرجى المحاولة لاحقًا.','SANA did not finish; try later.'));
  }catch(e){setError(e.message)}finally{setAiBusy(false)}
 }
 async function acceptBalance(){if(!aiDraft?.draftId||!window.confirm(word(lang,'تطبيق الإحصائيات المقترحة على الشخصيات؟','Apply suggested stats?')))return;
  setAiBusy(true);setError('');try{
   const updated=await api(`/api/projects/${project.id}/creativity/assist/drafts/${encodeURIComponent(aiDraft.draftId)}/approve-balance`,{method:'POST'});
   setAiDraft(updated);await refresh();setReport(null);
  }catch(e){setError(e.message)}finally{setAiBusy(false)}
 }
 const active=report?.analyzedCharacters??0;
 const readyFighters=fighters.filter(f=>STAT_KEYS.every(k=>f[k]!=null&&Number.isFinite(Number(f[k]))));
 if(!toolReady)return <ToolDiagramGate lang={lang} onCreate={async()=>{await diagramIdFor(project,'STORY_MAP',true);setToolReady(true)}}/>;
 return <section className="ux-tool-page sana-character-lab" dir={lang==='ar'?'rtl':'ltr'}>
  <header className="sana-lab-hero">
   <div className="sana-lab-hero-orb" aria-hidden="true"><Activity size={32}/></div>
   <div className="sana-lab-hero-copy"><small>SANA · CHARACTER LAB</small><h1>{word(lang,'استوديو الشخصيات','Character Studio')}</h1><p>{word(lang,'امنح كل شخصية قدراتها الخاصة، وقارن نقاط قوتها وتحدياتها في مساحة واحدة.','Shape your characters and compare their strengths and challenges in one place.')}</p></div>
   <div className="sana-lab-hero-actions"><Action icon={UserRound} onClick={()=>formOpen(null)}>{word(lang,'إضافة شخصية','Add character')}</Action><Action icon={BarChart3} light onClick={analyze} disabled={busy||fighters.length===0}>{busy?word(lang,'جارٍ التحليل...','Analyzing...'):word(lang,'تحليل الأرقام','Analyze stats')}</Action></div>
  </header>
  <ErrorBanner error={error} onClose={()=>setError('')}/>
  <section className="sana-lab-roster" aria-label={word(lang,'شخصيات اللعبة','Playable characters')}>
   <div className="sana-lab-section-heading"><div><small>{word(lang,'الشخصيات الحالية','CURRENT CHARACTERS')}</small><h2><UsersRound size={21}/>{word(lang,'شخصيات اللعبة','Game characters')}<span>{fighters.length}</span></h2><p>{word(lang,'اختر شخصية من البطاقات لتعديل قدراتها، أو أضف شخصية جديدة.','Edit an existing playable character or add another.')}</p></div><button type="button" className="sana-lab-text-action" onClick={()=>formOpen(null)}><Plus size={17}/>{word(lang,'شخصية جديدة','New character')}</button></div>
   {fighters.length===0?<EmptySpace icon={Gamepad2} title={word(lang,'لم تُضف شخصيات قابلة للعب بعد','No playable characters yet')} subtitle={word(lang,'أضف شخصية جديدة أو اربط شخصية محفوظة من القصة.','Add a character or link one from your story.')} action={<Action onClick={()=>formOpen(null)}>{word(lang,'إضافة شخصية','Add character')}</Action>}/>:<div className="sana-lab-roster-list">{fighters.map((f,i)=><button type="button" key={chosenId(f)} className="sana-lab-roster-chip" style={{'--fighter-accent':['#0F4C5C','#409C91','#C29B5C','#8E7AA9','#B87D70'][i%5]}} onClick={()=>formOpen(f)} title={word(lang,'تعديل الشخصية','Edit character')}><span className="sana-lab-roster-dot"/><span className="sana-lab-roster-name">{f.name}</span><small>{f.role||f.characterProfileName||word(lang,'شخصية قابلة للعب','Playable')}</small><Pencil size={14}/></button>)}</div>}
  </section>
  <div className="sana-lab-radar-wrap"><CharacterRadarChart fighters={fighters} lang={lang}/><aside className="sana-lab-radar-guide"><span className="sana-lab-guide-symbol"><BarChart3 size={22}/></span><h3>{word(lang,'قراءة بصمة الشخصية','Reading the character profile')}</h3><p>{word(lang,'كلما امتد الشكل في اتجاه قدرة معيّنة كانت قيمة هذه القدرة أعلى. يمكن اختيار حتى ثلاث شخصيات للمقارنة.','A longer axis means a higher stat. Compare up to three characters at once.')}</p><div><strong>{readyFighters.length}</strong><span>{word(lang,'شخصيات جاهزة للمقارنة','characters ready to compare')}</span></div><div><strong>6</strong><span>{word(lang,'قدرات لكل شخصية','stats per character')}</span></div></aside></div>
  <section className="sana-balance-ai sana-lab-ai" aria-label={word(lang,'موازنة الشخصيات مع سنا','Balance characters with SANA')}>
   <div className="sana-lab-ai-intro"><span className="sana-lab-ai-icon"><Sparkles size={26}/></span><div className="sana-balance-ai-head"><span>SANA AI</span><h2>{word(lang,'خلّ سنا تساعدك في الموازنة','Let SANA help balance your characters')}</h2><p>{word(lang,'تحلل سنا أدوار الشخصيات وقدراتها، ثم تعرض التعديلات المقترحة للمراجعة قبل تطبيقها.','SANA reviews characters and suggests stat changes for your approval before applying them.')}</p></div></div>
   <div className="sana-lab-ai-form"><label>{word(lang,'توجيه إضافي (اختياري)','Optional direction')}<input maxLength={300} value={aiFocus} onChange={e=>setAiFocus(e.target.value)} placeholder={word(lang,'مثال: الحفاظ على سرعة الشخصية السريعة','e.g. Preserve the fastest character’s speed')}/></label><button type="button" className="sana-lab-ai-submit" disabled={aiBusy||!readyFighters.length} onClick={suggestBalance}><Sparkles size={17}/>{aiBusy?word(lang,'سنا تراجع الشخصيات...','SANA is reviewing...'):word(lang,'اقتراح موازنة الشخصيات','Suggest character balance')}</button></div>
 {aiDraft&&<p role="status">{aiDraft.status==='PENDING'?aiDraft.summary:aiDraft.status==='APPLIED'?word(lang,'تم تطبيق المقترحات بنجاح.','Changes applied successfully.'):aiDraft.status==='FAILED'?aiDraft.error:word(lang,'جارٍ تجهيز المقترحات...','Preparing suggestions...')}</p>}
 {aiDraft?.status==='PENDING'&&<div className="sana-balance-suggestions">{(aiDraft.changes||[]).map(s=><article key={s.characterId}><strong>{s.name}</strong><p>{s.reason}</p><button type="button" onClick={()=>setCompare(compare===s.characterId?null:s.characterId)}>{word(lang,'مقارنة قبل وبعد','Compare before / after')}</button>{compare===s.characterId&&<CharacterRadarChart fighters={[{id:'original',name:word(lang,'قبل','Before'),...s.before},{id:'suggested',name:word(lang,'اقتراح سنا','SANA suggestion'),...s.after}]} lang={lang}/>}<div className="sana-stat-diff">{STAT_KEYS.map(k=><div key={k}><small>{word(lang,...STAT_NAMES[k])}</small><span>{s.before[k]} → <b>{s.after[k]}</b></span></div>)}</div></article>)}
 {!aiDraft.changes?.length?<p>{word(lang,'سنا ما اقترحت تغييرًا ضروريًا للشخصيات الحالية.','No changes recommended for these characters.')}</p>:<div className="sana-balance-decision"><button type="button" className="btn btn-primary" onClick={acceptBalance} disabled={aiBusy}>{word(lang,'موافقة وتطبيق التعديلات','Approve & apply')}</button><button type="button" className="btn" disabled={aiBusy} onClick={()=>{setAiDraft(null);setCompare(null)}}>{word(lang,'إلغاء','Discard')}</button></div>}</div>}
  </section>
 {report&&<section className="ux-report" aria-label={word(lang,'نتيجة التحليل','Analysis results')}><div className="ux-report-top"><div><small>SANA · BALANCE INSIGHT</small><h2>{word(lang,'ملخص توازن الشخصيات','Character balance summary')}</h2></div><button onClick={()=>setReport(null)} aria-label={word(lang,'إغلاق التقرير','Close report')}><X size={17}/></button></div><div className="ux-report-metrics"><div><small>{word(lang,'الشخصيات','Characters')}</small><strong>{report.totalCharacters??0}</strong></div><div><small>{word(lang,'تم تحليلها','Analyzed')}</small><strong>{active}</strong></div><div><small>{word(lang,'متوسط النقاط','Average points')}</small><strong>{report.averagePoints==null?'—':Number(report.averagePoints).toFixed(1)}</strong></div></div>{(report.characters||[]).map(r=><div className="ux-report-character" key={r.characterId}><div><strong>{r.name}</strong><span>{r.totalPoints} {word(lang,'نقطة','points')}</span></div>{r.warnings?.length?<ul>{r.warnings.map((w,i)=><li key={i}>{word(lang,'هذه الشخصية تحتاج مراجعة بسيطة مقارنة ببقية الشخصيات.','This character may need a small balance review compared with the others.')}</li>)}</ul>:<p><Check size={15}/>{word(lang,'توزيع النقاط يبدو متقاربًا ومريحًا لهذه الشخصية.','This character looks well-balanced overall.')}</p>}</div>)}{(report.notes||[]).length>0&&<div className="ux-report-notes"><Info size={17}/><div>{report.notes.map((n,i)=><p key={i}>{lang==='ar'?(n.includes('Only one')?'نحتاج شخصيتين على الأقل بإحصائيات كاملة عشان تكون المقارنة أوضح.':n.includes('without gameplay')?'بعض الشخصيات ما دخلت في المقارنة لأن بياناتها ناقصة.':n.includes('Equal total')?'حتى لو تساوت النقاط، الأفضل تجربة الشخصيات داخل اللعبة للتأكد من التوازن.':n.includes('No characters')?'لسه ما فيه شخصيات جاهزة للتحليل.':n):n}</p>)}</div></div>}<Hint>{word(lang,'هذا الملخص يساعد على مراجعة الأرقام بسرعة، ويبقى اختبار اللعب هو الأهم.','Use this summary for a quick review, then rely on playtesting for the final call.')}</Hint></section>}
  {fighters.length>0&&<section className="sana-lab-details" aria-label={word(lang,'تفاصيل شخصيات اللعبة','Playable character details')}><div className="sana-lab-section-heading"><div><small>{word(lang,'بطاقات الشخصيات','CHARACTER DETAILS')}</small><h2><UserRound size={21}/>{word(lang,'قدرات الشخصيات بالتفصيل','Character stats in detail')}</h2></div></div><div className="ux-balance-grid">{fighters.map(f=><article className="ux-balance-card" key={chosenId(f)}><header><span className="ux-avatar"><UserRound size={24}/></span><div><strong>{f.name}</strong><small>{f.characterProfileName?`${word(lang,'من القصة:','From story:')} ${f.characterProfileName}`:(f.role||word(lang,'شخصية قابلة للعب','Playable character'))}</small></div><button type="button" className="ux-icon" onClick={()=>formOpen(f)} aria-label={word(lang,'تعديل','Edit')}><Pencil size={16}/></button></header><div className="ux-stats-rows">{STAT_KEYS.map(k=><div className="ux-stat-row" key={k}><span>{word(lang,...STAT_NAMES[k])}</span><div className="ux-stat-meter"><i style={{width:`${Math.max(0,Math.min(10,Number(f[k]??0)))*10}%`}}/></div><b>{f[k]??'—'}</b></div>)}</div><footer><span>{STAT_KEYS.every(k=>f[k]!=null)?STAT_KEYS.reduce((sum,k)=>sum+Number(f[k]),0):'—'} / 60 {word(lang,'نقطة','pts')}</span><button type="button" onClick={()=>{if(window.confirm(word(lang,'حذف شخصية اللعبة؟ ستبقى الشخصية الأصلية في القصة محفوظة.','Delete this playable character? The story profile will remain.')))commit(()=>store.deleteFighter(project,f.id??f.characterId))}} aria-label={word(lang,'حذف','Delete')}><Trash2 size={16}/></button></footer></article>)}</div></section>}
  <details className="sana-lab-profiles"><summary><span><UsersRound size={19}/>{word(lang,'الشخصيات الأصلية في القصة','Original story characters')} <b>{profiles.length}</b></span><span>{word(lang,'عرض الشخصيات','View characters')} <ChevronLeft size={17}/></span></summary><p>{word(lang,'تبقى شخصيات القصة محفوظة في خريطة العلاقات حتى عند استخدامها كشخصيات لعب.','Your original story characters remain in the story map when linked to playable characters.')}</p>{profileError&&<p role="alert" className="sana-profile-error">{profileError}</p>}{profiles.length?<div className="sana-story-profile-list">{profiles.map(p=>{const linked=usedProfileIds.has(String(p.entityId??p.id));return <div key={String(p.entityId??p.id)}><strong>{p.name}</strong><span>{linked?word(lang,'مستخدمة في التوازن','Already linked'):word(lang,'متاحة للإضافة','Available to add')}</span></div>})}</div>:<p>{word(lang,'لا توجد شخصيات قصصية محفوظة حاليًا.','No saved story characters found.')}</p>}</details>
  {drawer&&<Drawer lang={lang} className="sana-character-drawer" onClose={()=>{if(!busy)setDrawer(null)}} title={edit?word(lang,'تعديل شخصية اللعبة','Edit character'):word(lang,'إضافة شخصية قابلة للعب','Add playable character')} desc={word(lang,'أنشئ شخصية جديدة أو استخدم شخصية من القصة، ثم حدّد القدرات المناسبة.','Create a new playable character or link one from your story, then set their abilities.')}>
   <form className="ux-form sana-character-form" onSubmit={saveCharacter}>
    {error&&<div className="sana-character-form-error" role="alert"><AlertTriangle size={17}/><span>{error}</span></div>}
    <section className="sana-character-form-section"><div className="sana-character-form-heading"><h3>{word(lang,'طريقة إضافة الشخصية','Choose character source')}</h3><p>{word(lang,'اختر مصدر الشخصية قبل تعبئة بياناتها.','Choose where the character comes from.')}</p></div><div className="sana-character-mode-grid"><button type="button" className={`sana-character-mode ${characterMode==='new'?'selected':''}`} aria-pressed={characterMode==='new'} onClick={()=>{setCharacterMode('new');setProfile('')}}><UserRound size={22}/><span><strong>{word(lang,'شخصية جديدة','New character')}</strong><small>{word(lang,'إنشاء شخصية بدون ارتباط بالقصة','Create independently of the story')}</small></span><Check size={16}/></button><button type="button" className={`sana-character-mode ${characterMode==='story'?'selected':''}`} aria-pressed={characterMode==='story'} onClick={()=>setCharacterMode('story')}><UsersRound size={22}/><span><strong>{word(lang,'من شخصيات القصة','From story characters')}</strong><small>{word(lang,'ربط شخصية محفوظة باللعبة','Link an existing story character')}</small></span><Check size={16}/></button></div>
     {characterMode==='story'&&<><Labeled label={word(lang,'الشخصيات المتاحة','Available story characters')}><select required value={profile} onChange={e=>selectStoryProfile(e.target.value)}><option value="">{word(lang,'اختر شخصية من القصة','Choose a story character')}</option>{availableProfiles.map(p=><option key={String(p.entityId??p.id)} value={String(p.entityId??p.id)}>{p.name}</option>)}</select></Labeled>{availableProfiles.length===0&&<Hint>{word(lang,'تم استخدام جميع شخصيات القصة المتاحة. يمكن إنشاء شخصية جديدة بدلًا من ذلك.','All saved story characters are already linked. Create a new character instead.')}</Hint>}</>}
    </section>
    <section className="sana-character-form-section"><div className="sana-character-form-heading"><h3>{word(lang,'المعلومات الأساسية','Basic information')}</h3><p>{word(lang,'الاسم والدور الذي تؤديه الشخصية في اللعبة.','Character name and role in the game.')}</p></div><div className="sana-character-two-cols"><Labeled label={word(lang,'اسم الشخصية *','Character name *')}><input required maxLength={100} value={name} onChange={e=>setName(e.target.value)} placeholder={word(lang,'مثال: فارس','e.g. Fares')}/></Labeled><Labeled label={word(lang,'الدور (اختياري)','Role (optional)')}><input maxLength={100} value={role} onChange={e=>setRole(e.target.value)} placeholder={word(lang,'مثال: مدافع','e.g. Defender')}/></Labeled></div></section>
    <details className="sana-character-extra"><summary><span><Info size={18}/>{word(lang,'تفاصيل إضافية للشخصية','Additional character details')}</span><ChevronLeft size={18}/></summary><div className="sana-character-two-cols"><Labeled label={word(lang,'اسم السلاح','Weapon name')}><textarea rows={2} value={details.weaponName||''} onChange={e=>setDetails(d=>({...d,weaponName:e.target.value}))}/></Labeled><Labeled label={word(lang,'نوع السلاح','Weapon type')}><textarea rows={2} value={details.weaponType||''} onChange={e=>setDetails(d=>({...d,weaponType:e.target.value}))}/></Labeled><Labeled label={word(lang,'نقاط القوة','Strengths')}><textarea rows={2} value={details.strengths||''} onChange={e=>setDetails(d=>({...d,strengths:e.target.value}))}/></Labeled><Labeled label={word(lang,'نقاط الضعف','Weaknesses')}><textarea rows={2} value={details.weaknesses||''} onChange={e=>setDetails(d=>({...d,weaknesses:e.target.value}))}/></Labeled></div></details>
    <section className="sana-character-form-section"><div className="sana-character-form-heading"><h3>{word(lang,'قدرات الشخصية','Character abilities')}</h3><p>{word(lang,'اضبط كل قدرة من 0 إلى 10، باستخدام الشريط أو إدخال الرقم مباشرة.','Set each ability from 0 to 10 using the slider or number field.')}</p></div><div className="sana-character-stats-grid">{STAT_KEYS.map(k=><div className="sana-character-stat" key={k}><div className="sana-character-stat-head"><label htmlFor={`sana-stat-${k}`}>{word(lang,...STAT_NAMES[k])}</label><output>{stats[k]??word(lang,'غير محدد','Not set')}</output></div><div className="sana-character-stat-controls"><input id={`sana-stat-${k}`} type="range" min="0" max="10" step="1" value={stats[k]??0} onChange={e=>setStats(prev=>({...prev,[k]:Number(e.target.value)}))} style={{'--stat-progress':`${(stats[k]??0)*10}%`}}/><input type="number" aria-label={`${word(lang,...STAT_NAMES[k])} (${word(lang,'من 0 إلى 10','0 to 10')})`} min="0" max="10" step="1" value={stats[k]??''} onChange={e=>{const v=e.target.value;setStats(prev=>({...prev,[k]:v===''?null:Math.max(0,Math.min(10,Math.round(Number(v))))}))}} placeholder="—"/></div></div>)}</div></section>
    <div className="sana-character-form-footer"><button type="button" className="sana-character-cancel" onClick={()=>setDrawer(null)} disabled={busy}>{word(lang,'إلغاء','Cancel')}</button><SubmitRow busy={busy} lang={lang} submitLabel={edit?word(lang,'حفظ التعديلات','Save changes'):word(lang,'حفظ الشخصية','Save character')}/></div>
   </form>
  </Drawer>}
 </section>;
}

function ToolDiagramGate({lang,onCreate}){const [busy,setBusy]=useState(false),[error,setError]=useState('');return <section className="spark-panel"><h2>{tr(lang,'toolDiagramMissing')}</h2>{error&&<p role="alert">{error}</p>}<button className="btn btn-primary" disabled={busy} onClick={async()=>{setBusy(true);try{await onCreate()}catch(e){setError(e.message)}finally{setBusy(false)}}}>{tr(lang,'createToolDiagram')}</button></section>}
