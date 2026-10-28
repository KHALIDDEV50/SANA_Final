import React,{useEffect,useRef,useState} from 'react';

export function useDialogFocus(ref,onClose,enabled=true){
 const close=useRef(onClose);close.current=onClose;
 useEffect(()=>{
  if(!enabled||!ref.current)return;
  const previous=document.activeElement,root=ref.current;
  const focusable=()=>[...root.querySelectorAll('button:not(:disabled),a[href],input:not(:disabled),textarea:not(:disabled),select:not(:disabled),[tabindex="0"]')].filter(e=>e.getClientRects().length);
  focusable()[0]?.focus();
  const key=e=>{
   if(e.key==='Escape'){e.preventDefault();e.stopPropagation();close.current?.()}
   if(e.key==='Tab'){
    const all=focusable(),first=all[0],last=all.at(-1);
    if(!first){e.preventDefault();return}
    if(e.shiftKey&&(document.activeElement===first||!root.contains(document.activeElement))){e.preventDefault();last.focus()}
    else if(!e.shiftKey&&(document.activeElement===last||!root.contains(document.activeElement))){e.preventDefault();first.focus()}
   }
  };
  root.addEventListener('keydown',key);
  return()=>{root.removeEventListener('keydown',key);previous?.isConnected&&previous.focus?.()};
 },[enabled,ref]);
}

// SANA and the canvas share their parent's studio-layout grid. No fixed pixel offsets,
// ResizeObserver, or synthetic spacer; the panel starts next to the actual work area.
export default function StudioWorkspace({children,assistant,open}){
 const dockRef=useRef(null);
 const [compact,setCompact]=useState(()=>typeof window!=='undefined'&&window.matchMedia('(max-width:1100px)').matches);
 useEffect(()=>{
  const media=window.matchMedia('(max-width:1100px)');
  const change=()=>setCompact(media.matches);
  media.addEventListener('change',change);
  return()=>media.removeEventListener('change',change);
 },[]);
 useDialogFocus(dockRef,assistant?.props?.onClose,open&&compact);
 return <div className={`spark-workspace ${open?'assistant-open':''}`}>
  {React.isValidElement(children)?React.cloneElement(children,{
   assistantOpen:open,assistantDock:assistant,assistantDockRef:dockRef,assistantCompact:compact
  }):children}
 </div>;
}
