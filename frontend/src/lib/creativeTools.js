/** Game narrative, playable-character balancing and moodboards.
 * In API mode every operation uses its actual Spring Boot controller.
 * In demo mode boards are stored in localStorage and never presented as server-saved.
 */
import {API_MODE,api,diagramIdFor} from './data';
// Use the real MoodboardController whenever Spring Boot API mode is active.
// Local uploads remain device-only; the new unified library persists verified provider images.
export const MOODBOARD_API=API_MODE;
const STORE='sana-special-tools-v3';
const LOCAL_IMAGES='sana-local-mood-images-v4';
const localPhotos=()=>{try{return JSON.parse(localStorage.getItem(LOCAL_IMAGES))||{}}catch{return {}}};
const localItems=boardId=>localPhotos()[boardId]||[];
const persistPhoto=(boardId,item)=>{const state=localPhotos();state[boardId]=[...(state[boardId]||[]),item];localStorage.setItem(LOCAL_IMAGES,JSON.stringify(state));return item};
const replacePhoto=(boardId,id,fn)=>{const state=localPhotos();state[boardId]=(state[boardId]||[]).map(i=>i.id===id?fn(i):i);localStorage.setItem(LOCAL_IMAGES,JSON.stringify(state))};
const dropPhoto=(boardId,id)=>{const state=localPhotos();state[boardId]=(state[boardId]||[]).filter(i=>i.id!==id);localStorage.setItem(LOCAL_IMAGES,JSON.stringify(state))};
const copy=v=>JSON.parse(JSON.stringify(v));
const initial={};
const load=()=>{try{return JSON.parse(localStorage.getItem(STORE))||copy(initial)}catch{return copy(initial)}};
const save=v=>{localStorage.setItem(STORE,JSON.stringify(v));return v};
const id=()=>Date.now()*100+Math.floor(Math.random()*99);
const body=(method,data)=>({method,body:JSON.stringify(data)});
const scope=(projectId)=>{const db=load();if(!db[projectId])db[projectId]={boards:[],scenes:[],choices:[],fighters:[],variables:[]};return [db,db[projectId]]};
export const creativeTools={
 // Character profiles remain independent story entities even after linking gameplay statistics.
 async storyProfiles(project){
  if(!API_MODE)return (project.elements||[]).filter(e=>e.type==='characters'&&e.name);
  const known=(project.diagrams||[]).filter(d=>['STORY_MAP','CREATIVITY'].includes(d.outputType));
  const diagramIds=[...new Set(known.map(d=>d.id).filter(Boolean))];
  if(!diagramIds.length){
   const id=await diagramIdFor(project,'STORY_MAP');
   diagramIds.push(id);
  }
  const results=await Promise.allSettled(diagramIds.map(id=>api(`/api/v1/creativity/search/${id}?type=CHARACTER`)));
  const successful=results.filter(r=>r.status==='fulfilled');
  if(!successful.length)throw results[0]?.reason||new Error('Could not load story characters');
  const unique=new Map();
  for(const result of successful)for(const r of (Array.isArray(result.value)?result.value:[])){
   if(String(r.type).toUpperCase()!=='CHARACTER'||r.id==null)continue;
   unique.set(String(r.id),{id:r.nodeId,entityId:r.id,name:r.title||'',description:r.description||'',type:'characters'});
  }
  return [...unique.values()];
 },

 async saveBranchLayout(project,nodes){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/update/${outputId}/layout/nodes/${nodes.map(n=>n.branchingNodeId).join(',')}`,body('PUT',nodes.map(n=>({positionX:n.positionX,positionY:n.positionY}))));const [db,s]=scope(project.id);s.scenes=s.scenes.map(n=>({...n,...nodes.find(m=>m.branchingNodeId===n.branchingNodeId)}));save(db)},
 async updateVariable(project,variableId,name,initialValue){const id=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/update/${id}/variables/${variableId}`,body('PUT',{name,initialValue}));const [db,s]=scope(project.id);const v=s.variables.find(v=>v.id===variableId);if(v)Object.assign(v,{name,initialValue});save(db)},
 async unlinkFighter(project,characterId){const id=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/${id}/characters/${characterId}/profile`,{method:'DELETE'});const [db,s]=scope(project.id);const f=s.fighters.find(f=>String(f.id??f.characterId)===String(characterId));if(f){f.characterProfileId=null;f.characterProfileName=null;save(db)}},
 async renameBoard(projectId,boardId,title){if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/update/${boardId}/title`,body('PUT',{title}));const [db,s]=scope(projectId);const board=s.boards.find(b=>String(b.id)===String(boardId));if(!board)throw Error('Moodboard not found');board.title=title;save(db);return board;},
 async editLabel(projectId,boardId,itemId,data){if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/update/${boardId}/labels/${itemId}`,body('PUT',data));const [db,s]=scope(projectId);const item=s.boards.find(b=>String(b.id)===String(boardId))?.items.find(i=>String(i.id)===String(itemId));if(!item?.label)throw Error('Note not found');item.label={...item.label,...data};save(db);return item;},
 async boards(projectId){if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/get/project/${projectId}`);return scope(projectId)[1].boards.map(({id,title})=>({id,title,projectId}));},
 async deleteBoard(projectId,boardId){
  if(MOODBOARD_API){
   await api(`/api/v1/project-output/delete/${encodeURIComponent(boardId)}`,{method:'DELETE'});
   const photos=localPhotos();delete photos[boardId];localStorage.setItem(LOCAL_IMAGES,JSON.stringify(photos));
   return true;
  }
  const [db,state]=scope(projectId);state.boards=state.boards.filter(b=>String(b.id)!==String(boardId));save(db);return true;
 },
 async addBoard(projectId,title){if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/add/${projectId}`,body('POST',{title}));const [db,s]=scope(projectId);const board={id:id(),title,projectId,items:[]};s.boards.push(board);save(db);return board},
 async board(projectId,boardId){if(MOODBOARD_API){const b=await api(`/api/v1/creativity/moodboard/get/${boardId}`);return {...b,items:[...(b.items||[]),...localItems(boardId)]}}return scope(projectId)[1].boards.find(b=>String(b.id)===String(boardId))||null},
 async searchImages(keyword){if(!MOODBOARD_API)return {images:[]};return api(`/api/v1/creativity/moodboard/images/search?keyword=${encodeURIComponent(keyword)}&page=1&pageSize=20`)},
 // Explicitly opt-in to a device-only reference for uploads and pasted URLs.
 async addLocalReference(projectId,boardId,image,pos){
  if(!image?.imageUrl || !/^https:\/\//i.test(image.imageUrl))throw new Error('No usable image URL');
  const item={id:-id(),type:'MOODBOARD_IMAGE',image:{...image,id:`local-${image.id||id()}`},positionX:pos.x,positionY:pos.y,localOnly:true};
  if(MOODBOARD_API)return persistPhoto(boardId,item);
  const [db,state]=scope(projectId);const board=state.boards.find(b=>String(b.id)===String(boardId));
  if(!board)throw new Error('Moodboard not found');board.items.push(item);save(db);return item;
 },
 async addImage(projectId,boardId,image,pos){
  if(MOODBOARD_API && ['PIXABAY','CLEVELAND','MET','WIKIMEDIA'].includes(image?.provider)){
   // Never silently store a search result as a local-only photo if server validation fails.
   return api(`/api/v1/creativity/moodboard/add/${boardId}/images`,body('POST',{
    provider:image.provider,imageId:image.id,positionX:pos.x,positionY:pos.y
   }));
  }
  if(MOODBOARD_API){const item={id:-id(),type:'MOODBOARD_IMAGE',image,positionX:pos.x,positionY:pos.y,localOnly:true};return persistPhoto(boardId,item)}
  const [db,s]=scope(projectId);const board=s.boards.find(b=>b.id===boardId);board.items.push({id:id(),type:'MOODBOARD_IMAGE',image,positionX:pos.x,positionY:pos.y});save(db);return board
 },
 async addLabel(projectId,boardId,text,pos,colors={}){const data={text,positionX:pos.x,positionY:pos.y,color:colors.color||'#39322c',backgroundColor:colors.backgroundColor||'#fff0ce',fontSize:17};if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/add/${boardId}/labels`,body('POST',data));const [db,s]=scope(projectId);const board=s.boards.find(b=>b.id===boardId);board.items.push({id:id(),type:'MOODBOARD_LABEL',label:{text,color:data.color,backgroundColor:data.backgroundColor,fontSize:17},positionX:pos.x,positionY:pos.y});save(db);return board},
 async moveBoardItem(projectId,boardId,itemId,x,y){if(MOODBOARD_API && Number(itemId)<0){replacePhoto(boardId,itemId,i=>({...i,positionX:x,positionY:y}));return;}if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/update/${boardId}/items/${itemId}/position`,body('PUT',{positionX:x,positionY:y}));const [db,s]=scope(projectId);const item=s.boards.find(b=>b.id===boardId)?.items.find(i=>i.id===itemId);if(item){item.positionX=x;item.positionY=y;save(db)}},
 async deleteBoardItem(projectId,boardId,itemId){if(MOODBOARD_API && Number(itemId)<0){dropPhoto(boardId,itemId);return;}if(MOODBOARD_API)return api(`/api/v1/creativity/moodboard/delete/${boardId}/items/${itemId}`,{method:'DELETE'});const [db,s]=scope(projectId);const board=s.boards.find(b=>b.id===boardId);board.items=board.items.filter(i=>i.id!==itemId);save(db)},
 // Existing Spring Boot APIs keep speaker links separate from scene text.
 // Reading the graph exposes the saved speakerCharacterId for each projectNodeId.
 async branchSpeakers(project){
  if(!API_MODE)return {};
  const outputId=await diagramIdFor(project,'BRANCHING_STORY');
  const response=await api(`/api/v1/diagram/graph/get/${outputId}`);
  return Object.fromEntries((response?.graph?.nodes||[])
   .filter(node=>node.id!=null&&node.properties?.speakerCharacterId!=null)
   .map(node=>[String(node.id),String(node.properties.speakerCharacterId)]));
 },
 async linkSpeaker(project,nodeId,characterId){
  const outputId=await diagramIdFor(project,'BRANCHING_STORY');
  if(!API_MODE)throw new Error('Speaker linking requires the backend');
  return api(`/api/v1/branching/link/${outputId}/nodes/${nodeId}/speaker/${characterId}`,{method:'PUT'});
 },
 async unlinkSpeaker(project,nodeId){
  const outputId=await diagramIdFor(project,'BRANCHING_STORY');
  if(!API_MODE)throw new Error('Speaker linking requires the backend');
  return api(`/api/v1/branching/unlink/${outputId}/nodes/${nodeId}/speaker`,{method:'DELETE'});
 },
 async branching(project){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/get/${outputId}/map`);const s=scope(project.id)[1];return {nodes:s.scenes.map(n=>({...n})),choices:s.choices.map(c=>({...c}))}},
 async addScene(project,{title,content,type,positionX,positionY}){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/add/${outputId}/nodes`,body('POST',{title,content,type,positionX,positionY}));const [db,s]=scope(project.id);s.scenes.push({branchingNodeId:id(),title,content,type,positionX,positionY});save(db)},
 async updateScene(project,node){const outputId=await diagramIdFor(project,'BRANCHING_STORY');const nodeId=node.branchingNodeId;const data={title:node.title,content:node.content,type:node.type,positionX:node.positionX,positionY:node.positionY};if(API_MODE)return api(`/api/v1/branching/update/${outputId}/nodes/${nodeId}`,body('PUT',data));const [db,s]=scope(project.id);s.scenes=s.scenes.map(n=>n.branchingNodeId===nodeId?{...n,...data}:n);save(db)},
 async moveScene(project,nodeId,x,y){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/update/${outputId}/nodes/${nodeId}/position`,body('PUT',{positionX:x,positionY:y}));const [db,s]=scope(project.id);const node=s.scenes.find(n=>n.branchingNodeId===nodeId);if(node){node.positionX=x;node.positionY=y;save(db)}},
 async deleteScene(project,nodeId){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/delete/${outputId}/nodes/${nodeId}`,{method:'DELETE'});const [db,s]=scope(project.id);s.scenes=s.scenes.filter(n=>n.branchingNodeId!==nodeId);s.choices=s.choices.filter(c=>c.sourceBranchingNodeId!==nodeId&&c.targetBranchingNodeId!==nodeId);save(db)},
 async addChoice(project,source,target,text,displayOrder=0){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/add/${outputId}/choices/${source}/${target}`,body('POST',{text,displayOrder}));const [db,s]=scope(project.id);s.choices.push({choiceId:id(),sourceBranchingNodeId:source,targetBranchingNodeId:target,text,displayOrder});save(db)},
 async deleteChoice(project,choiceId){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/delete/${outputId}/choices/${choiceId}`,{method:'DELETE'});const [db,s]=scope(project.id);s.choices=s.choices.filter(c=>c.choiceId!==choiceId);save(db)},
 async variables(project){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/get/${outputId}/variables`);return scope(project.id)[1].variables||[]},
 async addVariable(project,name,initialValue){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/add/${outputId}/variables`,body('POST',{name,initialValue}));const [db,s]=scope(project.id);s.variables=s.variables||[];if(s.variables.some(v=>v.name===name))throw Error('Variable name already exists');s.variables.push({id:id(),name,initialValue});save(db)},
 async deleteVariable(project,variableId){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(API_MODE)return api(`/api/v1/branching/delete/${outputId}/variables/${variableId}`,{method:'DELETE'});const [db,s]=scope(project.id);s.variables=(s.variables||[]).filter(v=>v.id!==variableId);save(db)},
 async exportYarn(project){const outputId=await diagramIdFor(project,'BRANCHING_STORY');if(!API_MODE)throw Error('Yarn export requires backend');return api(`/api/v1/branching/export/yarn/${outputId}`)},
 async fighters(project){const outputId=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/get/${outputId}/characters`);return scope(project.id)[1].fighters},
 async addFighter(project,data){const outputId=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/${outputId}/characters/add`,body('POST',data));const [db,s]=scope(project.id);s.fighters.push({id:id(),...data});save(db)},
 async updateFighter(project,characterId,data){const outputId=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/${outputId}/characters/update/${characterId}`,body('PUT',data));const [db,s]=scope(project.id);const fighter=s.fighters.find(f=>(f.id??f.characterId)===characterId);if(!fighter)throw Error('Character not found');Object.assign(fighter,data);save(db)},
 async deleteFighter(project,characterId){const outputId=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/${outputId}/characters/delete/${characterId}`,{method:'DELETE'});const [db,s]=scope(project.id);s.fighters=s.fighters.filter(f=>(f.id??f.characterId)!==characterId);save(db)},
 async linkFighter(project,characterId,profileId){const outputId=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/${outputId}/characters/${characterId}/profile/${profileId}`,{method:'PUT'});const [db,s]=scope(project.id);const fighter=s.fighters.find(f=>String(f.id||f.characterId)===String(characterId));if(fighter){const profile=project.elements?.find(e=>String(e.entityId||e.id)===String(profileId));fighter.characterProfileId=profileId;fighter.characterProfileName=profile?.name||null;save(db);}},
 async analyzeFighters(project){const outputId=await diagramIdFor(project,'STORY_MAP');if(API_MODE)return api(`/api/v1/character-balance/${outputId}/analyze`);const fighters=scope(project.id)[1].fighters||[];const ready=fighters.filter(f=>['attack','defense','speed','rangeStat','stamina','special'].every(k=>f[k]!=null&&f[k]!==''&&Number.isFinite(Number(f[k]))));const total=f=>['attack','defense','speed','rangeStat','stamina','special'].reduce((n,k)=>n+Number(f[k]),0);const avg=ready.length?ready.reduce((n,f)=>n+total(f),0)/ready.length:null;return {totalCharacters:fighters.length,analyzedCharacters:ready.length,averagePoints:avg==null?null:Math.round(avg*10)/10,notes:ready.length<2?['Only one or no characters have complete stats. Group comparison requires at least two.']:['Equal total points do not guarantee actual gameplay balance.'],characters:ready.map(f=>({characterId:f.id,name:f.name,totalPoints:total(f),differenceFromAverage:Math.round((total(f)-avg)*10)/10,warnings:ready.length>1&&Math.abs(total(f)-avg)>avg*.1?['Total points differ from the group average by more than 10%']:[]}))}}
};


// Creativity-only AI moodboard calls. Kept with the existing moodboard data API.
const moodboardAIPath = id => `/api/projects/${encodeURIComponent(id)}/creativity/moodboard-ai`;
const moodboardRequest = (method, body) => ({method, headers:{'X-Sana-Moodboard':'1'}, body:JSON.stringify(body)});
export const moodboardAi = {
 generate:(id,prompt,language)=>api(`${moodboardAIPath(id)}/drafts`,moodboardRequest('POST',{prompt,language})),
 get:(id,draft)=>api(`${moodboardAIPath(id)}/drafts/${encodeURIComponent(draft)}`,{headers:{'X-Sana-Moodboard':'1'}}),
 approve:(id,draft)=>api(`${moodboardAIPath(id)}/drafts/${encodeURIComponent(draft)}/approve`,moodboardRequest('POST',{version:1})),
 reject:(id,draft)=>api(`${moodboardAIPath(id)}/drafts/${encodeURIComponent(draft)}/reject`,moodboardRequest('POST',{version:1}))
};

export function moodboardReferenceUrl(value) {
 try {
  const url=new URL(value),host=url.hostname.toLowerCase();
  return url.protocol==='https:'&&!url.username&&!url.password&&host.includes('.')
   && !/^[\d.]+$/.test(host)&&!host.endsWith('.localhost')&&!host.endsWith('.local')&&!host.endsWith('.internal') ? url.href:null;
 } catch {return null}
}


// The SANA API uses Authorization headers; an ordinary <img> request cannot send them.
// Fetch saved Pixabay files as a blob using the same browser credentials, then revoke the URL.
export async function moodboardAuthorizedImageBlobUrl(assetPath, signal){
 if(!/^\/api\/v1\/creativity\/moodboard\/images\/pixabay-assets\/[0-9]+$/.test(assetPath))
  throw new Error('Unsupported protected moodboard asset');
 const base=(import.meta.env.VITE_SANA_API_URL||'').replace(/\/$/,'');
 const token=sessionStorage.getItem('sana-auth');
 const response=await fetch(`${base}${assetPath}`,{
  signal,credentials:'include',headers:token?{Authorization:`Basic ${token}`}:{},
 });
 if(!response.ok)throw new Error(`Could not load saved image (HTTP ${response.status})`);
 const blob=await response.blob();
 if(!blob.type.startsWith('image/')||blob.size>8*1024*1024)
  throw new Error('Invalid saved image');
 return URL.createObjectURL(blob);
}
