// All SANA domains share the same project/canvas contract.
// Templates describe editable starter nodes; they are not AI generations.
// Non-creative node types are the backend node types in lower case (SERVICE -> service).
export const CREATIVE_TYPES = ['characters','worlds','locations','rules','events'];
export const TECH_TYPES = ['client','application','service','database','queue','component','external_system'];
export const BUSINESS_TYPES = ['customer_segment','value_proposition','channels','customer_relationships','revenue_streams','key_resources','key_activities','key_partnerships','cost_structure'];
export const EDUCATION_TYPES = ['central_topic','topic','subtopic','note'];
export const TYPES = [...CREATIVE_TYPES,...TECH_TYPES,...BUSINESS_TYPES,...EDUCATION_TYPES];
export const typesForDomain = domain => domain === 'technology' ? TECH_TYPES : domain === 'business' ? BUSINESS_TYPES : domain === 'education' ? EDUCATION_TYPES : CREATIVE_TYPES;
export const domainGroup = domain => ['writing','games','creativity'].includes(domain) ? 'creativity' : domain;
export const TEMPLATE_CATALOG = [
  {id:'blank-creative',domain:'writing',group:'creativity',ar:'لوحة إبداعية فارغة',en:'Blank creative canvas',arDesc:'مساحة حرة لبناء الشخصيات والعوالم والأحداث.',enDesc:'A free canvas for characters, worlds and plot.'},
  {id:'novel',domain:'writing',group:'creativity',ar:'مخطط رواية',en:'Novel planner',arDesc:'ابدئي بشخصية رئيسية وعالم وحدث أول.',enDesc:'Start with a protagonist, a world and an opening event.',nodes:[{type:'characters',ar:'الشخصية الرئيسية',en:'Protagonist'},{type:'worlds',ar:'عالم القصة',en:'Story world'},{type:'events',ar:'الحدث الأول',en:'Opening event'}]},
  {id:'game',domain:'games',group:'creativity',ar:'قصة لعبة',en:'Game narrative',arDesc:'أساس لبناء شخصيات اللعبة وبيئتها والأحداث.',enDesc:'Outline your game characters, setting and events.',nodes:[{type:'characters',ar:'بطل اللعبة',en:'Game protagonist'},{type:'worlds',ar:'عالم اللعبة',en:'Game world'},{type:'events',ar:'المهمة الأولى',en:'First quest'}]},
  {id:'blank-tech',domain:'technology',group:'technology',ar:'لوحة تقنية فارغة',en:'Blank technical canvas',arDesc:'خططي للمكونات التقنية بطريقتك.',enDesc:'Design your architecture from scratch.'},
  {id:'architecture',domain:'technology',group:'technology',ar:'معمارية نظام',en:'System architecture',arDesc:'واجهة مستخدم وخدمة وقاعدة بيانات مترابطة.',enDesc:'Plan a client app, a service and a database.',nodes:[{type:'client',ar:'تطبيق الويب',en:'Web app'},{type:'service',ar:'الخدمة الأساسية',en:'Core service'},{type:'database',ar:'قاعدة البيانات',en:'Database'}]},
  {id:'event-driven',domain:'technology',group:'technology',ar:'نظام قائم على الأحداث',en:'Event-driven system',arDesc:'خدمات تتواصل عبر طابور رسائل.',enDesc:'Services that talk through a message queue.',nodes:[{type:'service',ar:'خدمة الطلبات',en:'Order service'},{type:'queue',ar:'طابور الرسائل',en:'Message queue'},{type:'service',ar:'خدمة الإشعارات',en:'Notification service'}]},
  {id:'blank-business',domain:'business',group:'business',ar:'لوحة تجارية فارغة',en:'Blank business canvas',arDesc:'ابدئي بخريطة لفكرتك التجارية.',enDesc:'Explore a business idea on a free canvas.'},
  {id:'business-model',domain:'business',group:'business',ar:'نموذج فكرة تجارية',en:'Business idea canvas',arDesc:'العملاء، القيمة، الإيرادات والتكاليف في مكان واحد.',enDesc:'Map customers, value, revenue and costs.',nodes:[{type:'customer_segment',ar:'العملاء المستهدفون',en:'Target customers'},{type:'value_proposition',ar:'القيمة المقترحة',en:'Value proposition'},{type:'revenue_streams',ar:'مصادر الدخل',en:'Revenue streams'},{type:'cost_structure',ar:'التكاليف',en:'Costs'}]},
  {id:'marketing',domain:'business',group:'business',ar:'خطة وصول للعملاء',en:'Customer growth plan',arDesc:'حددي العملاء والقنوات والقيمة المقدمة.',enDesc:'Clarify audience, channels and value.',nodes:[{type:'customer_segment',ar:'شريحة العملاء',en:'Audience'},{type:'channels',ar:'قنوات الوصول',en:'Acquisition channels'},{type:'value_proposition',ar:'ما الذي يميزنا؟',en:'Our advantage'}]},
  {id:'blank-education',domain:'education',group:'education',ar:'خريطة ذهنية فارغة',en:'Blank mind map',arDesc:'ابدئي بموضوع رئيسي وفرّعي الأفكار.',enDesc:'Start from a central topic and branch out.',nodes:[{type:'central_topic',ar:'الموضوع الرئيسي',en:'Main topic'}]},
  {id:'study-map',domain:'education',group:'education',ar:'خريطة مذاكرة',en:'Study map',arDesc:'موضوع رئيسي مع محاور للمراجعة.',enDesc:'A main topic with sections to revise.',nodes:[{type:'central_topic',ar:'الموضوع الرئيسي',en:'Main topic'},{type:'topic',ar:'المحور الأول',en:'First section'},{type:'topic',ar:'المحور الثاني',en:'Second section'}]},
];
export const findTemplate = id => TEMPLATE_CATALOG.find(t => t.id === id);
export const starterNodes = (templateId, lang = 'ar') => (findTemplate(templateId)?.nodes || []).map((n,i)=>({type:n.type,name:lang==='en'?n.en:n.ar,description:'',x:105+(i%3)*268,y:118+Math.floor(i/3)*175}));

// Edges refer to indices in the template's starter nodes (not to persistent IDs).
export const starterConnections = (templateId,lang='ar') => templateId === 'study-map'
  ? [{source:0,target:1,type:'BRANCH',label:lang==='ar'?'يتفرع إلى':'Branch'},{source:0,target:2,type:'BRANCH',label:lang==='ar'?'يتفرع إلى':'Branch'}]
  : [];
