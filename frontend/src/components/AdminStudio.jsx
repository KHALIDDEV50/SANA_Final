import React,{useEffect,useState} from 'react';
import {Activity,BarChart3,FolderKanban,Users,Sparkles,RefreshCw,ClipboardList,AlertCircle} from 'lucide-react';
import {adminApi} from '../lib/admin';
import {tr} from '../lib/i18n';

const safeList=value=>Array.isArray(value)?value:[];
const readableDate=(value,lang)=>{if(!value)return '—';const date=new Date(value);return Number.isNaN(date.getTime())?'—':date.toLocaleString(lang==='ar'?'ar-SA':'en-GB',{dateStyle:'medium',timeStyle:'short'})};
const activityNames={interactions:['سجل المساعد','Assistant history'],runs:['عمليات الأدوات','Tool activity'],proposals:['المقترحات','Proposals']};
const statusClass=status=>String(status||'').toUpperCase().replace(/[^A-Z_]/g,'').toLowerCase();

export default function AdminStudio({user,lang,onOpen}){
 const t=k=>tr(lang,k),ar=lang==='ar';
 const [users,setUsers]=useState(null),[projects,setProjects]=useState(null);
 const [requests,setRequests]=useState(null),[proposals,setProposals]=useState(null);
 const [activity,setActivity]=useState([]),[activityKind,setActivityKind]=useState('interactions');
 const [tab,setTab]=useState('dashboard'),[query,setQuery]=useState(''),[editing,setEditing]=useState(null);
 const [error,setError]=useState(''),[historyError,setHistoryError]=useState(''),[busy,setBusy]=useState(false);
 const [loaded,setLoaded]=useState(false);

 async function refresh(){
  setBusy(true);setError('');setHistoryError('');
  try{
   const [u,p,r,pr]=await Promise.allSettled([adminApi.users(),adminApi.projects(),adminApi.activity('interactions'),adminApi.activity('proposals')]);
   if(u.status==='fulfilled')setUsers(safeList(u.value));
   if(p.status==='fulfilled')setProjects(safeList(p.value));
   if(r.status==='fulfilled')setRequests(safeList(r.value));else setHistoryError(ar?'تعذر عرض سجل المساعد حاليًا.':'Assistant history is unavailable right now.');
   if(pr.status==='fulfilled')setProposals(safeList(pr.value));
   if(u.status==='rejected'||p.status==='rejected'){
    const issue=u.status==='rejected'?u.reason:p.reason;
    setError(issue?.status===403?t('accessDenied'):(ar?'تعذر تحميل بعض بيانات الإدارة.':'Could not load some administration data.'));
   }
   setLoaded(true);
  }finally{setBusy(false)}
 }
 useEffect(()=>{if(user?.role==='ADMIN')refresh()},[user?.id,user?.role]);
 async function loadActivity(kind=activityKind){
  setBusy(true);setError('');
  try{const rows=kind==='interactions'&&requests!==null?requests:kind==='proposals'&&proposals!==null?proposals:await adminApi.activity(kind);setActivity(safeList(rows))}
  catch{setActivity([]);setError(ar?'تعذر تحميل السجل.':'Could not load activity history.')}
  finally{setBusy(false)}
 }
 async function action(fn){setBusy(true);setError('');try{await fn();setEditing(null);await refresh()}catch(e){setError(e?.status===403?t('accessDenied'):(e?.message||t('error')))}finally{setBusy(false)}}
 if(user?.role!=='ADMIN')return <main className="main-shell inner-page"><p role="alert">{t('accessDenied')}</p></main>;
 const rows=(tab==='users'?users:projects)||[];
 const filtered=rows.filter(x=>`${x.name||''} ${x.email||''} ${x.id||''}`.toLowerCase().includes(query.toLowerCase()));
 const recent=(requests||[]).slice().sort((a,b)=>new Date(b.createdAt||0)-new Date(a.createdAt||0)).slice(0,6);
 const legacyNote=ar?'السجل المتاح حاليًا لا يشمل طلبات سنا الحديثة. لذلك الأرقام هنا خاصة بالطلبات الظاهرة فقط.':'This history currently excludes newer SANA requests. Counts reflect only records available here.';
 const metrics=[
  {label:ar?'المستخدمون':'Users',value:users?.length,Icon:Users},
  {label:ar?'المشاريع':'Projects',value:projects?.length,Icon:FolderKanban},
  {label:ar?'سجلات المساعد المتاحة':'Available assistant records',value:requests?.length,Icon:Sparkles},
  {label:ar?'المقترحات المتاحة':'Available proposals',value:proposals?.length,Icon:ClipboardList}
 ];
 return <main className="main-shell inner-page spark-admin sana-admin-studio">
  <header className="sana-admin-intro"><span className="eyebrow">✦ SANA</span><h1>{ar?'لوحة تحكم سنا':'SANA Dashboard'}</h1><p>{ar?'مساحة واحدة لمتابعة المستخدمين والمشاريع والنشاط المتاح.':'Your space for reviewing people, projects, and available activity.'}</p></header>
  <nav className="sana-admin-tabs" aria-label={ar?'أقسام الإدارة':'Administration sections'}>
   {[
    ['dashboard',ar?'الرئيسية':'Dashboard',BarChart3],
    ['users',t('users'),Users],
    ['projects',t('projects'),FolderKanban],
    ['activity',ar?'سجل النشاط':'Activity',Activity]
   ].map(([key,label,Icon])=><button type="button" key={key} aria-current={tab===key?'page':undefined} className={tab===key?'active':''} onClick={()=>{setTab(key);setEditing(null);setQuery('');if(key==='activity')loadActivity()}}><Icon size={16}/>{label}</button>)}
   <button type="button" className="sana-admin-refresh" disabled={busy} onClick={()=>tab==='activity'?loadActivity():refresh()}><RefreshCw size={16}/>{t('refresh')}</button>
  </nav>
  {error&&<p className="ux-error" role="alert">{error}</p>}
  {tab==='dashboard'&&<>
   <section className="sana-admin-overview" aria-label={ar?'ملخص الإدارة':'Admin summary'}>{metrics.map(({label,value,Icon})=><article key={label}><div><small>{label}</small><strong>{value==null?'—':value}</strong></div><span className="sana-admin-metric-icon"><Icon size={22}/></span></article>)}</section>
   <section className="sana-admin-history"><header><div><span className="eyebrow">✦ {ar?'نشاط سنا':'SANA ACTIVITY'}</span><h2>{ar?'آخر سجلات المساعد':'Recent assistant history'}</h2></div><button type="button" onClick={()=>{setTab('activity');setActivityKind('interactions');loadActivity('interactions')}}>{ar?'عرض السجل':'View history'}</button></header>
    <p className="sana-admin-history-note"><AlertCircle size={17}/>{legacyNote}</p>
    {historyError&&<p role="status" className="sana-admin-history-note">{historyError}</p>}
    {recent.length?<div className="sana-admin-event-list">{recent.map((r,i)=><article key={r.id??i}><span className="sana-admin-event-icon"><Sparkles size={18}/></span><div><strong>{r.actionType||r.title||(ar?'طلب مساعدة':'Assistant request')}</strong><small>{readableDate(r.createdAt,lang)}</small></div><span className={`sana-admin-status status-${statusClass(r.status)}`}>{r.status||'—'}</span></article>)}</div>:<div className="sana-admin-empty"><Sparkles size={27}/><p>{loaded?(ar?'ما فيه طلبات ظاهرة في هذا السجل حاليًا.':'No requests are available in this history yet.'):(ar?'جارٍ تحميل النشاط...':'Loading activity...')}</p></div>}
   </section>
  </>}
  {(tab==='users'||tab==='projects')&&<>
   <div className="sana-admin-table-tools"><input className="search-field" value={query} onChange={e=>setQuery(e.target.value)} placeholder={t('search')}/>{tab==='users'&&<button className="btn btn-primary" onClick={()=>setEditing({kind:'user',name:'',email:'',passwordHash:''})}>{t('add')}</button>}</div>
   {editing&&<form className="spark-panel spark-admin-form" onSubmit={e=>{e.preventDefault();action(()=>editing.kind==='user'?adminApi.saveUser(editing.id,{name:editing.name,email:editing.email,passwordHash:editing.passwordHash}):adminApi.saveProject(editing,{name:editing.name,description:editing.description,status:editing.status}))}}><h2>{t('edit')}</h2><label>{t('name')}<input required maxLength={150} value={editing.name} onChange={e=>setEditing({...editing,name:e.target.value})}/></label>{editing.kind==='user'?<><label>{t('email')}<input type="email" required value={editing.email} onChange={e=>setEditing({...editing,email:e.target.value})}/></label><label>{t('password')}<input autoComplete="new-password" type="password" required minLength={8} value={editing.passwordHash} onChange={e=>setEditing({...editing,passwordHash:e.target.value})}/></label><p>{ar?'تغيير بيانات المستخدم يتطلب كلمة مرور جديدة.':'Updating user details requires a new password.'}</p></>:<><label>{t('description')}<textarea value={editing.description||''} onChange={e=>setEditing({...editing,description:e.target.value})}/></label><label>{t('status')}<select value={editing.status} onChange={e=>setEditing({...editing,status:e.target.value})}>{['DRAFT','ACTIVE','ARCHIVED'].map(value=><option key={value}>{value}</option>)}</select></label></>}<div className="modal-actions"><button type="button" className="btn btn-ghost" onClick={()=>setEditing(null)}>{t('cancel')}</button><button className="btn btn-primary" disabled={busy}>{t('save')}</button></div></form>}
   <div className="spark-table-scroll"><table><thead><tr><th>ID</th><th>{t('name')}</th><th>{tab==='users'?t('email'):t('domain')}</th><th>{tab==='users'?(ar?'الدور':'Role'):t('status')}</th><th>{ar?'الإجراءات':'Actions'}</th></tr></thead><tbody>{filtered.map(row=><tr key={row.id}><td>{row.id}</td><td>{row.name}</td><td>{tab==='users'?row.email:row.domain}</td><td>{tab==='users'?row.role||'USER':row.status}</td><td><button disabled={busy} onClick={()=>setEditing({...row,kind:tab==='users'?'user':'project',passwordHash:''})}>{t('edit')}</button>{tab==='projects'&&<button onClick={()=>onOpen(row.id)}>{t('openProject')}</button>}<button disabled={busy||(tab==='users'&&row.id===user.id)} onClick={()=>{if(window.confirm(`${t('confirmDelete')} — ${row.name}`))action(()=>tab==='users'?adminApi.deleteUser(row.id):adminApi.deleteProject(row.id))}}>{t('delete')}</button></td></tr>)}</tbody></table>{!filtered.length&&!busy&&<p className="sana-admin-no-results">{t('noResults')}</p>}</div>
  </>}
  {tab==='activity'&&<section className="sana-admin-history"><header><div><h2>{ar?'سجل النشاط المتاح':'Available activity history'}</h2><p>{legacyNote}</p></div><select value={activityKind} disabled={busy} onChange={e=>{setActivityKind(e.target.value);loadActivity(e.target.value)}}>{Object.entries(activityNames).map(([key,labels])=><option key={key} value={key}>{labels[ar?0:1]}</option>)}</select></header><div className="sana-admin-table-tools"><input className="search-field" value={query} onChange={e=>setQuery(e.target.value)} placeholder={t('search')}/></div><div className="spark-table-scroll"><table><thead><tr><th>ID</th><th>{t('type')}</th><th>{t('status')}</th><th>{ar?'وقت الإنشاء':'Created'}</th></tr></thead><tbody>{activity.filter(r=>[r.id,r.title,r.actionType,r.toolType,r.status].join(' ').toLowerCase().includes(query.toLowerCase())).map(r=><tr key={r.id}><td>{r.id}</td><td>{r.title||r.actionType||r.toolType||'—'}</td><td><span className={`sana-admin-status status-${statusClass(r.status)}`}>{r.status||'—'}</span></td><td>{readableDate(r.createdAt,lang)}</td></tr>)}</tbody></table>{!activity.length&&!busy&&<p className="sana-admin-no-results">{ar?'لا توجد سجلات متاحة حاليًا.':'No activity records are available.'}</p>}</div></section>}
 </main>;
}
