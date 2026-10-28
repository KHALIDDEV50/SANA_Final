import {API_MODE,api} from './data';

// The frontend never creates invoices or changes subscription privileges.
// An authorized Spring Boot endpoint must issue a checkout URL and confirm payment with Moyasar.
export const PLANS=[
 {code:'SPARK',monthlySar:0,maxProjects:1,nameAr:'شرارة',nameEn:'Spark',taglineAr:'كل فكرة عظيمة تبدأ بشرارة.',taglineEn:'Every great idea begins with a spark.',descriptionAr:'لبداية الرحلة وتجربة فكرتك الأولى.',descriptionEn:'A space for your very first idea.',theme:'spark'},
 {code:'GLOW',monthlySar:29,maxProjects:10,nameAr:'وهج',nameEn:'Glow',taglineAr:'لما تبدأ أفكارك تتوهّج.',taglineEn:'Let your ideas shine brighter.',descriptionAr:'مساحة أكبر لتطوير أفكار ومشاريع متعددة.',descriptionEn:'Room to grow several ideas and projects.',theme:'glow'},
 {code:'HORIZON',monthlySar:79,maxProjects:null,nameAr:'أفق',nameEn:'Horizon',taglineAr:'أطلق أفكارك إلى آفاق بلا حدود.',taglineEn:'Create beyond boundaries.',descriptionAr:'مساحة غير محدودة لمشاريعك القادمة.',descriptionEn:'Unlimited space for all your next projects.',theme:'horizon'}
];

// Integration contract for the payment teammate:
// GET  {VITE_SANA_BILLING_API_BASE}/me       -> {planCode,status,projectsUsed,maxProjects,currentPeriodEnd}
// POST {VITE_SANA_BILLING_API_BASE}/checkout -> {checkoutUrl}  (body: {planCode})
// The checkout URL must come from backend; amount, currency and customer are set server-side.
const billingBase=String(import.meta.env.VITE_SANA_BILLING_API_BASE||'').trim().replace(/\/+$/,'');
export const billingEnabled=Boolean(API_MODE&&billingBase&&billingBase.startsWith('/api/'));

export const billingApi={
 async getMembership(){
  if(!billingEnabled)return null;
  const result=await api(`${billingBase}/me`);
  if(!result||typeof result!=='object')throw new Error('Invalid membership response');
  return result;
 },
 async createCheckout(planCode){
  if(!billingEnabled)throw new Error('Billing is not connected');
  if(!['GLOW','HORIZON'].includes(planCode))throw new Error('Invalid plan');
  const response=await api(`${billingBase}/checkout`,{method:'POST',body:JSON.stringify({planCode})});
  const target=response?.checkoutUrl;
  // Trust only an HTTPS checkout URL returned by our authenticated backend.
  let url;
  try{url=new URL(target)}catch{throw new Error('Checkout is unavailable')}
  if(url.protocol!=='https:'||!url.hostname||url.username||url.password)throw new Error('Invalid checkout destination');
  return url.href;
 }
};

export const findPlan=code=>PLANS.find(p=>p.code===String(code||'').toUpperCase())||null;
export const membershipLimit=membership=>{
 if(!membership||!findPlan(membership.planCode))return null;
 // The backend is the authority for an active plan and its effective limits.
 const plan=findPlan(membership.planCode);
 if(plan.code!=='SPARK'&&String(membership.status||'').toUpperCase()!=='ACTIVE')return 1;
 const value=membership.maxProjects;
 return value===null?plan.maxProjects:Number.isInteger(Number(value))&&Number(value)>=0?Number(value):plan.maxProjects;
};
