import React from 'react';
import {BUSINESS_TOOLS} from '../lib/businessTools';

// React escapes every value; model output is never inserted as HTML.
const labels={
 executiveSummary:'الملخص التنفيذي',businessModel:'نموذج العمل',customerSegments:'شرائح العملاء',
 valuePropositions:'القيمة المقترحة',channels:'القنوات',customerRelationships:'علاقات العملاء',
 revenueStreams:'مصادر الإيرادات',keyResources:'الموارد الرئيسية',keyActivities:'الأنشطة الرئيسية',
 keyPartnerships:'الشراكات الرئيسية',costStructure:'هيكل التكاليف',businessModelCoherence:'ترابط نموذج العمل',
 audienceSegments:'شرائح الجمهور',customerPersonas:'شخصيات العملاء',segmentPrioritization:'أولوية الشرائح',
 strengths:'نقاط القوة',weaknesses:'نقاط الضعف',opportunities:'الفرص',threats:'التهديدات',strategies:'الاستراتيجيات',
 SO:'القوة والفرص',WO:'الضعف والفرص',ST:'القوة والتهديدات',WT:'الضعف والتهديدات',
 competitors:'المنافسون',comparisonCriteria:'معايير المقارنة',comparison:'المقارنة',competitiveAdvantages:'المزايا التنافسية',
 marketGaps:'فجوات السوق',informationGaps:'نقص المعلومات',risks:'المخاطر',riskContext:'سياق المخاطر',criticalRiskAreas:'مجالات المخاطر المهمة',
 criticalGaps:'الفجوات المهمة',gaps:'الفجوات',assumptions:'الافتراضات',recommendations:'التوصيات',followUpQuestions:'أسئلة المتابعة',researchQuestions:'أسئلة البحث',
 name:'الاسم',title:'العنوان',description:'الوصف',needs:'الاحتياجات',painPoints:'التحديات',motivations:'الدوافع',
 buyingBarriers:'عوائق الشراء',decisionCriteria:'معايير القرار',purchasingRole:'دور الشراء',customerType:'نوع العميل',
 evidenceBasis:'أساس المعلومة',confidence:'الثقة',priority:'الأولوية',businessImpact:'الأثر التجاري',potentialBenefit:'الفائدة المحتملة',
 potentialImpact:'الأثر المحتمل',conditions:'الشروط',warningIndicators:'مؤشرات التحذير',action:'الإجراء',rationale:'المبرر',firstStep:'الخطوة الأولى',
 assumption:'الافتراض',reason:'السبب',validationMethod:'طريقة التحقق',area:'المجال',whyItMatters:'الأهمية',
 question:'السؤال',purpose:'الهدف',customerProblem:'مشكلة العميل',customerBenefit:'فائدة العميل',differentiation:'التميز',
 mechanism:'آلية الإيراد',pricingConsiderations:'اعتبارات التسعير',contribution:'المساهمة',dependency:'الاعتماد',costType:'نوع التكلفة',
 finding:'النتيجة',profile:'الملف',segmentName:'الشريحة',goals:'الأهداف',objections:'الاعتراضات',preferredChannels:'القنوات المفضلة',
 buyingTriggers:'محفزات الشراء',keyUncertainty:'نقطة عدم اليقين',competitorName:'اسم المنافس',competitorType:'نوع المنافس',
 targetCustomers:'العملاء المستهدفون',knownProductsOrServices:'المنتجات أو الخدمات المعروفة',knownStrengths:'نقاط القوة المعروفة',knownWeaknesses:'نقاط الضعف المعروفة',
 criterion:'المعيار',assessment:'التقييم',missingInformation:'المعلومات الناقصة',researchMethod:'طريقة البحث',
 linkedStrengths:'نقاط القوة المرتبطة',linkedWeaknesses:'نقاط الضعف المرتبطة',linkedOpportunities:'الفرص المرتبطة',linkedThreats:'التهديدات المرتبطة',
 riskId:'معرف الخطر',category:'الفئة',causes:'الأسباب',potentialConsequences:'النتائج المحتملة',likelihood:'الاحتمال',impact:'الأثر',severity:'الشدة',
 earlyWarningIndicators:'مؤشرات الإنذار المبكر',preventiveMeasures:'الإجراءات الوقائية',mitigationMeasures:'إجراءات التخفيف',contingencyPlan:'خطة الطوارئ',responsibleRole:'الدور المسؤول',informationNeeded:'المعلومات المطلوبة'
};
const enumLabels={HIGH:'عالية',MEDIUM:'متوسطة',LOW:'منخفضة',FACT:'حقيقة مقدمة',ASSUMPTION:'افتراض',INFERENCE:'استنتاج',PROVIDED_DATA:'بيانات مقدمة',LIMITED:'محدودة',DIRECT:'مباشر',INDIRECT:'غير مباشر',FIXED:'ثابتة',VARIABLE:'متغيرة',BUYER:'المشتري',DECISION_MAKER:'صاحب القرار'};
const label=(key,lang)=>lang==='ar'?(labels[key]||key):key.replace(/([A-Z])/g,' $1').trim();
function Value({value,lang,depth=0}) {
 if(value==null)return <span>—</span>;
 if(Array.isArray(value))return value.length?<ul className="business-values">{value.map((item,i)=><li key={i}><Value value={item} lang={lang} depth={depth+1}/></li>)}</ul>:<small>{lang==='ar'?'لا توجد عناصر مدعومة بالمدخلات.':'No supported entries.'}</small>;
 if(typeof value==='object')return <dl className="business-fields">{Object.entries(value).map(([key,item])=><div key={key}><dt>{label(key,lang)}</dt><dd><Value value={item} lang={lang} depth={depth+1}/></dd></div>)}</dl>;
 return <span className={typeof value==='string'&&['HIGH','MEDIUM','LOW'].includes(value)?`business-priority ${value.toLowerCase()}`:undefined}>{lang==='ar'?(enumLabels[value]||String(value)):String(value)}</span>;
}
export default function BusinessReport({report,lang='ar'}) {
 const tool=BUSINESS_TOOLS.find(t=>t.toolType===report.toolType);
 return <article className="business-report"><h3>{tool?.[lang]||report.toolType}</h3><p>{report.executiveSummary}</p>
 {Object.entries(report).filter(([key])=>!['toolType','executiveSummary'].includes(key)).map(([key,value])=><details key={key} open={['strengths','weaknesses','opportunities','threats','recommendations'].includes(key)}><summary>{label(key,lang)}</summary><Value value={value} lang={lang}/></details>)}</article>;
}
