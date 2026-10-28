import {API_MODE,api} from './data';
import {PLANS,checkoutDestination} from './billingRules';
export {PLANS,findPlan,membershipLimit} from './billingRules';

// The frontend never creates invoices or changes subscription privileges.
// An authorized Spring Boot endpoint issues a checkout URL and verifies payment with Moyasar.
// Integration contract for the payment teammate:
// GET  {VITE_SANA_BILLING_API_BASE}/me       -> {planCode,status,projectsUsed,maxProjects,currentPeriodEnd}
// POST {VITE_SANA_BILLING_API_BASE}/checkout -> {checkoutUrl}  (body: {planCode})
// The checkout URL must come from backend; amount, currency and customer are set server-side.
const billingBase=String(import.meta.env.VITE_SANA_BILLING_API_BASE||'/api/billing').trim().replace(/\/+$/,'');
export const billingEnabled=Boolean(API_MODE&&billingBase&&billingBase.startsWith('/api/'));

export const billingApi={
 async getCatalog(){
  if(!billingEnabled)return null;
  const result=await api(`${billingBase}/plans`);
  if(!Array.isArray(result?.plans)||PLANS.some(p=>!result.plans.some(v=>v.code===p.code&&Number.isInteger(v.amountHalalas)&&v.amountHalalas>=0)))throw new Error('Invalid billing catalog');
  return result;
 },
 async getOrders(){
  if(!billingEnabled)return [];
  return api(`${billingBase}/orders`);
 },
 async reconcile(){
  if(!billingEnabled)throw new Error('Billing is not connected');
  return api(`${billingBase}/reconcile`,{method:'POST'});
 },
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
  return checkoutDestination(target);
 }
};

