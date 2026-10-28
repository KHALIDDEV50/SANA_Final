// Billing addition: pure rules shared by the UI and local tests.
export const PLANS=[
 {code:'SPARK',monthlySar:0,maxProjects:1,nameAr:'شرارة',nameEn:'Spark',taglineAr:'كل فكرة عظيمة تبدأ بشرارة.',taglineEn:'Every great idea begins with a spark.',descriptionAr:'لبداية الرحلة وتجربة فكرتك الأولى.',descriptionEn:'A space for your very first idea.',theme:'spark'},
 {code:'GLOW',monthlySar:29,maxProjects:10,nameAr:'وهج',nameEn:'Glow',taglineAr:'لما تبدأ أفكارك تتوهّج.',taglineEn:'Let your ideas shine brighter.',descriptionAr:'مساحة أكبر لتطوير أفكار ومشاريع متعددة.',descriptionEn:'Room to grow several ideas and projects.',theme:'glow'},
 {code:'HORIZON',monthlySar:79,maxProjects:null,nameAr:'أفق',nameEn:'Horizon',taglineAr:'أطلق أفكارك إلى آفاق بلا حدود.',taglineEn:'Create beyond boundaries.',descriptionAr:'مساحة غير محدودة لمشاريعك القادمة.',descriptionEn:'Unlimited space for all your next projects.',theme:'horizon'}
];

export const findPlan=code=>PLANS.find(p=>p.code===String(code||'').toUpperCase())||null;
export const membershipLimit=membership=>{
 if(!membership||membership.paymentsEnabled===false||!findPlan(membership.planCode))return null;
 // The backend is the authority for an active plan and its effective limits.
 const plan=findPlan(membership.planCode);
 if(plan.code!=='SPARK'&&String(membership.status||'').toUpperCase()!=='ACTIVE')return 1;
 const value=membership.maxProjects;
 return value===null?plan.maxProjects:Number.isInteger(Number(value))&&Number(value)>=0?Number(value):plan.maxProjects;
};

export function checkoutDestination(target) {
 let url;
 try{url=new URL(target)}catch{throw new Error('Checkout is unavailable')}
 if(url.protocol!=='https:'||url.hostname!=='checkout.moyasar.com'||(url.port&&url.port!=='443')||url.username||url.password)throw new Error('Invalid checkout destination');
 return url.href;
}
