import React,{useCallback,useEffect,useState} from 'react';
import {Sparkles,Sun,Orbit,Check,ArrowUpLeft,ArrowUpRight,ArrowLeft,ArrowRight,ShieldCheck,LockKeyhole,FolderKanban,CalendarDays,RefreshCw,AlertCircle} from 'lucide-react';
import {PLANS,billingApi,billingEnabled,findPlan,membershipLimit} from '../lib/billing';

const copy=(lang,ar,en)=>lang==='ar'?ar:en;
const ICONS={SPARK:Sparkles,GLOW:Sun,HORIZON:Orbit};
const sar=(amount,lang)=>new Intl.NumberFormat(lang==='ar'?'ar-SA':'en-SA',{maximumFractionDigits:0}).format(amount);
const planName=(plan,lang)=>lang==='ar'?plan.nameAr:plan.nameEn;
const dateLabel=(date,lang)=>{if(!date)return null;const d=new Date(date);return Number.isFinite(d.getTime())?d.toLocaleDateString(lang==='ar'?'ar-SA':'en-SA',{year:'numeric',month:'long',day:'numeric'}):null};

export default function BillingExperience({view='plans',resultStatus='',lang='ar',projects=[],onNavigate,guest=false}){
 const ar=lang==='ar',Back=ar?ArrowRight:ArrowLeft,Forward=ar?ArrowUpLeft:ArrowUpRight;
 const [membership,setMembership]=useState(null),[loading,setLoading]=useState(billingEnabled&&!guest),[error,setError]=useState(''),[checkout,setCheckout]=useState(''),[confirmPlan,setConfirmPlan]=useState(null);
 const reload=useCallback(async()=>{
  if(!billingEnabled||guest)return;
  setLoading(true);setError('');
  // After returning from Moyasar, ask the backend to verify the payment with Moyasar (never trust the return URL)
  try{setMembership(view==='result'?await billingApi.reconcile().catch(()=>billingApi.getMembership()):await billingApi.getMembership())}
  catch{setMembership(null);setError(copy(lang,'تعذر تحديث تفاصيل الاشتراك حاليًا. جرّب مرة أخرى.','Could not load your subscription. Please try again.'))}
  finally{setLoading(false)}
 },[lang,view,guest]);
 useEffect(()=>{reload()},[reload]);
 const activePlan=findPlan(membership?.planCode);
 const status=String(membership?.status||'').toUpperCase();
 const validPaid=Boolean(activePlan&&activePlan.code!=='SPARK'&&status==='ACTIVE');
 const effectivePlan=membership?(validPaid?activePlan:PLANS[0]):null;
 const used=Number.isInteger(Number(membership?.projectsUsed))?Number(membership.projectsUsed):projects.length;
 const limit=membership?membershipLimit(membership):null;
 const atLimit=membership&&limit!==null&&used>=limit;
 async function choose(plan){
  if(guest){onNavigate('projects');return;}
  if(plan.code==='SPARK'){onNavigate('projects');return}
  if(!billingEnabled){setError(copy(lang,'تجهيز الاشتراكات المدفوعة جارٍ. ستتوفر الترقية بمجرد تفعيل الدفع.','Paid subscriptions will be available once checkout is enabled.'));return}
  setConfirmPlan(plan);
 }
 async function confirmCheckout(){
  const plan=confirmPlan;
  if(!plan)return;
  setCheckout(plan.code);setError('');
  try{
   const url=await billingApi.createCheckout(plan.code);
   window.location.assign(url);
  }catch{
   setError(copy(lang,'تعذر بدء الدفع الآن. لم يتم تفعيل أي اشتراك أو خصم مبلغ. حاول مرة أخرى لاحقًا.','Checkout could not start. No subscription was activated. Please try later.'));
   setCheckout('');setConfirmPlan(null);
  }
 }
 const label=plan=>plan.maxProjects===null?copy(lang,'مشاريع بلا حدود','Unlimited projects'):plan.maxProjects===1?copy(lang,'مشروع واحد','1 project'):copy(lang,'10 مشاريع','10 projects');
 const features=plan=>[label(plan),copy(lang,'جميع المجالات الإبداعية والتقنية والتجارية والتعليمية','All four creative, technical, business, and learning domains'),copy(lang,'مخططات وأدوات تنظيم الأفكار','Diagrams and visual planning tools'),copy(lang,'لوحات الإلهام والقوالب','Moodboards and templates')];
 if(view==='result'){
  const failed=['failed','cancelled'].includes(resultStatus);
  return <main className="sana-billing-result main-shell page-enter"><div className="sana-billing-result-card">
   <div className="sana-billing-result-mark"><Sparkles size={29}/></div>
   <span className="sana-pricing-eyebrow">SANA · {copy(lang,'رحلتك الإبداعية','YOUR CREATIVE JOURNEY')}</span>
   <h1>{failed?copy(lang,resultStatus==='cancelled'?'تم إلغاء عملية الدفع':'لم تكتمل عملية الدفع',resultStatus==='cancelled'?'Checkout cancelled':'Payment was not completed'):validPaid?copy(lang,'باقة سنا جاهزة!','Your SANA plan is ready!'):copy(lang,'جارٍ تأكيد الاشتراك','Confirming your membership')}</h1>
   <p>{failed?copy(lang,'ما تغيّرت باقتك. تقدر ترجع للباقات وتجرب مرة ثانية.','Your plan was not changed. You can choose a plan again.'):validPaid?copy(lang,'تم تأكيد الاشتراك من حسابك. مساحتك الإبداعية جاهزة.','Your subscription has been confirmed by your account. Your space is ready.'):copy(lang,'إذا أكملت الدفع، قد يحتاج التفعيل لحظات حتى يصل التأكيد من نظام الدفع.','If you completed checkout, activation may take a moment while the payment is verified.')}</p>
   {!failed&&loading&&<span className="sana-pricing-loading">{copy(lang,'نتحقق من حالة الاشتراك...','Checking your subscription...')}</span>}
   {!failed&&error&&<div className="sana-billing-callout"><AlertCircle size={18}/>{error}</div>}
   <div className="sana-billing-result-actions"><button type="button" onClick={()=>onNavigate('membership')}>{copy(lang,'عرض باقتي','View my plan')}<Forward size={16}/></button><button type="button" onClick={()=>onNavigate('pricing')}>{copy(lang,'العودة إلى الباقات','Back to plans')}</button>{!failed&&billingEnabled&&!validPaid&&<button type="button" disabled={loading} onClick={reload}><RefreshCw size={16}/>{copy(lang,'تحديث الحالة','Refresh status')}</button>}</div>
  </div></main>;
 }
 return <main className="sana-pricing-shell main-shell page-enter">
  <section className="sana-pricing-hero">
   <div className="sana-pricing-hero-orbit" aria-hidden="true"><span>✦</span><span>✧</span><span>✦</span></div>
   <span className="sana-pricing-eyebrow"><Sparkles size={16}/>{copy(lang,'من شرارة إلى أفق','FROM SPARK TO HORIZON')}</span>
   <h1>{view==='membership'?copy(lang,'مساحتك مع سنا','Your SANA space'):copy(lang,'لكل شرارة مساحة لتنمو','Every spark deserves room to grow')}</h1>
   <p>{view==='membership'?copy(lang,'تعرف على باقتك ومساحة مشاريعك في مكان واحد.','Your plan and project space, beautifully organized.'):copy(lang,'ابدأ بمشروعك الأول، وارتقِ وقتما تحتاج مساحة أكبر لأفكارك.','Start with your first project. Grow whenever your ideas need more room.')}</p>
   <div className="sana-pricing-hero-actions"><button className="sana-pricing-quiet-link" onClick={()=>onNavigate('projects')}><Back size={16}/>{copy(lang,'مشاريعي','My projects')}</button><button className="sana-pricing-quiet-link" onClick={()=>onNavigate(view==='plans'?'membership':'pricing')}>{view==='plans'?copy(lang,'باقتي الحالية','My plan'):copy(lang,'استعراض الباقات','View plans')}<Forward size={16}/></button></div>
  </section>
  {view==='membership'&&<section className="sana-membership-summary" aria-live="polite">
   <div className="sana-membership-title"><span className="sana-pricing-mark"><FolderKanban size={19}/></span><div><h2>{copy(lang,'تفاصيل باقتك','Your membership')}</h2><p>{copy(lang,'مساحة مشاريعك الحالية','Your available project space')}</p></div></div>
   {!billingEnabled?<div className="sana-billing-callout"><AlertCircle size={19}/><span>{copy(lang,'ميزة متابعة الاشتراك قيد التجهيز. يمكنك الاطلاع على الباقات الآن، وتظهر تفاصيل باقتك بعد تفعيل الدفع.','Membership details will appear once billing is connected. You can browse the plans now.')}</span></div>:loading?<p className="sana-pricing-loading">{copy(lang,'جارٍ تحميل الباقة...','Loading membership...')}</p>:membership?<>
    <div className="sana-membership-metrics"><article><small>{copy(lang,'الباقة الحالية','Current plan')}</small><strong>{effectivePlan?planName(effectivePlan,lang):'—'}</strong></article><article><small>{copy(lang,'المشاريع المستخدمة','Projects used')}</small><strong>{sar(used,lang)}{limit!==null?` / ${sar(limit,lang)}`:''}{limit===null?' ∞':''}</strong></article><article><small>{copy(lang,'حالة الاشتراك','Subscription')}</small><strong>{status==='ACTIVE'?copy(lang,'نشط','Active'):status==='PENDING'?copy(lang,'بانتظار التأكيد','Pending'):status==='CANCELED'?copy(lang,'منتهي','Expired'):copy(lang,'متاح','Available')}</strong></article></div>
    {limit!==null&&<div className="sana-membership-meter"><div><span>{copy(lang,'المساحة المستخدمة','Used space')}</span><span>{Math.min(100,Math.round(used/Math.max(1,limit)*100))}%</span></div><progress value={Math.min(used,limit)} max={Math.max(1,limit)} aria-label={copy(lang,'عدد المشاريع المستخدمة','Projects used')}/></div>}
    {membership.currentPeriodEnd&&<p className="sana-membership-renew"><CalendarDays size={16}/>{copy(lang,'تاريخ نهاية الفترة الحالية: ','Current period ends: ')}{dateLabel(membership.currentPeriodEnd,lang)}</p>}
    {atLimit&&<div className="sana-billing-callout"><Sparkles size={19}/>{copy(lang,'وصلت إلى عدد المشاريع المتاح في باقتك. اختر باقة أكبر لمواصلة إنشاء المشاريع.','You have used all your project slots. Upgrade to create more projects.')}</div>}
   </>:<div className="sana-billing-callout"><AlertCircle size={19}/>{error||copy(lang,'لا تتوفر معلومات الباقة الآن.','Your membership is currently unavailable.')}<button type="button" onClick={reload}><RefreshCw size={15}/>{copy(lang,'إعادة المحاولة','Retry')}</button></div>}
  </section>}
  {view==='membership'&&<div className="sana-pricing-section-heading"><h2>{copy(lang,'اكتشف مساحة أكبر','Explore more possibilities')}</h2><p>{copy(lang,'اختر المساحة المناسبة لمشاريعك.','Choose the right space for your projects.')}</p></div>}
  <section className="sana-pricing-grid" aria-label={copy(lang,'باقات سنا','SANA plans')}>
   {PLANS.map(plan=>{const Icon=ICONS[plan.code],highlight=plan.code==='GLOW',current=Boolean(effectivePlan&&effectivePlan.code===plan.code);return <article className={`sana-pricing-card sana-plan-${plan.theme} ${highlight?'sana-plan-featured':''}`} key={plan.code}>
    {highlight&&<div className="sana-pricing-popular"><Sparkles size={13}/>{copy(lang,'الأكثر اختيارًا','MOST POPULAR')}</div>}
    <div className="sana-pricing-card-head"><span className="sana-pricing-card-icon"><Icon size={26}/></span><span className="sana-pricing-card-number">{plan.code==='SPARK'?'01':plan.code==='GLOW'?'02':'03'} / SANA</span></div>
    <h2>{planName(plan,lang)}</h2><p className="sana-pricing-tagline">{ar?plan.taglineAr:plan.taglineEn}</p>
    <div className="sana-pricing-price"><strong>{sar(plan.monthlySar,lang)}</strong><div><span>{copy(lang,'ر.س','SAR')}</span><small>{plan.monthlySar===0?copy(lang,'مجاني دائمًا','Free forever'):copy(lang,'شهريًا','per month')}</small></div></div>
    <div className="sana-pricing-limit"><FolderKanban size={18}/><strong>{label(plan)}</strong></div>
    <p className="sana-pricing-description">{ar?plan.descriptionAr:plan.descriptionEn}</p>
    <div className="sana-pricing-features">{features(plan).map(text=><div key={text}><Check size={16}/><span>{text}</span></div>)}</div>
    <button type="button" className="sana-pricing-cta" disabled={Boolean(checkout)||current} onClick={()=>choose(plan)}>{current?copy(lang,'باقتك الحالية','Current plan'):checkout===plan.code?copy(lang,'جارٍ تجهيز الدفع...','Starting checkout...'):plan.code==='SPARK'?copy(lang,'ابدأ مجانًا','Start for free'):plan.code==='GLOW'?copy(lang,'اختر وهج','Choose Glow'):copy(lang,'انطلق مع أفق','Choose Horizon')}<Forward size={17}/></button>
   </article>})}
  </section>
  {confirmPlan&&<div className="sana-billing-modal-backdrop" role="presentation" onMouseDown={e=>{if(e.target===e.currentTarget&&!checkout)setConfirmPlan(null)}}><section role="dialog" aria-modal="true" aria-labelledby="sana-payment-confirm" className="sana-billing-modal">
   <span className="sana-pricing-eyebrow"><Sparkles size={14}/>{copy(lang,'خطوة جديدة في رحلتك','YOUR NEXT STEP')}</span>
   <h2 id="sana-payment-confirm">{copy(lang,'تأكيد اختيار الباقة','Confirm your plan')}</h2>
   <p>{copy(lang,'راجع تفاصيل باقتك قبل الانتقال للدفع الآمن.','Review your plan before proceeding to secure checkout.')}</p>
   <div className="sana-billing-order"><div><span>{copy(lang,'الباقة','Plan')}</span><strong>{planName(confirmPlan,lang)}</strong></div><div><span>{copy(lang,'المشاريع','Projects')}</span><strong>{label(confirmPlan)}</strong></div><div><span>{copy(lang,'القيمة الشهرية','Monthly price')}</span><strong>{sar(confirmPlan.monthlySar,lang)} {copy(lang,'ر.س','SAR')}</strong></div></div>
   <small>{copy(lang,'يُحدد تاريخ التجديد وشروطه عند تفعيل نظام الاشتراكات. لا يُخصم أي مبلغ قبل تأكيد الدفع.','Renewal terms are shown when subscriptions become available. No charge is made until you confirm payment.')}</small>
   <div className="sana-billing-modal-actions"><button type="button" disabled={Boolean(checkout)} onClick={()=>setConfirmPlan(null)}>{copy(lang,'رجوع','Back')}</button><button type="button" disabled={Boolean(checkout)} onClick={confirmCheckout}>{checkout?copy(lang,'جارٍ تجهيز الدفع...','Starting checkout...'):copy(lang,'المتابعة للدفع','Continue to payment')}<Forward size={16}/></button></div>
  </section></div>}
  {error&&view==='plans'&&<div className="sana-billing-feedback" role="status"><AlertCircle size={18}/><span>{error}</span><button type="button" aria-label={copy(lang,'إغلاق','Dismiss')} onClick={()=>setError('')}>×</button></div>}
  <section className="sana-pricing-footer"><div><ShieldCheck size={22}/><h3>{copy(lang,'مشاريعك محفوظة، حتى لو تغيّرت باقتك','Your projects stay yours')}</h3><p>{copy(lang,'تغيير الباقة لا يحذف مشاريعك. وقد يتوقف إنشاء مشاريع جديدة عند بلوغ الحد المتاح.','Changing plans will not delete existing projects. New projects may be limited by your plan.')}</p></div><div><LockKeyhole size={22}/><h3>{copy(lang,'دفع آمن عند تفعيل الاشتراكات','Secure checkout')}</h3><p>{copy(lang,'عند توفر الدفع، سيتم التأكيد عبر خدمة دفع آمنة، ولا تُفعّل الباقة إلا بعد تأكيد العملية.','Once enabled, payment is confirmed securely before your plan is activated.')}</p></div></section>
  <p className="sana-pricing-disclaimer">{copy(lang,'الأسعار مقترحة للاعتماد قبل الإطلاق. الاشتراكات المدفوعة غير متاحة حتى يكتمل ربط الدفع والتحقق من الاشتراك. المزايا والأهلية النهائية يحددها النظام عند التفعيل.','Pricing is proposed for launch. Paid subscriptions are unavailable until checkout and server verification are connected.')}</p>
 </main>
}
