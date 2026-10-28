import React, {useState} from 'react';
import {moodboardReferenceUrl} from '../../lib/creativeTools';
import './moodboard-chat-preview.css';

export default function MoodboardChatPreview({preview, lang = 'en', busy, error, renderActions}) {
  const [failedImages,setFailedImages]=useState({});
  if (!preview?.draftId || !Array.isArray(preview.images)) return null;
  const ar = lang === 'ar';
  return <section className="ai-message creativity-mood-preview" dir={ar ? 'rtl' : 'ltr'} aria-label={ar ? 'معاينة لوحة الإلهام' : 'Moodboard preview'} aria-busy={busy}>
    <strong className="creativity-mood-preview__status">✦ {ar ? 'معاينة — لم تُحفظ بعد' : 'Preview — not saved yet'}</strong>
    <h3>{preview.title}</h3>
    <p>{preview.theme}</p>
    <div className="creativity-mood-preview__images">{preview.images.map(({image,searchTerm}) => {
      const source=moodboardReferenceUrl(image.sourceUrl),license=moodboardReferenceUrl(image.licenseUrl);
      const src=moodboardReferenceUrl(image.thumbnail)||moodboardReferenceUrl(image.imageUrl);
      return <figure key={image.id}>
        {src&&!failedImages[image.id]?<img src={src} alt={image.title||''} loading="lazy" referrerPolicy="no-referrer" onError={()=>setFailedImages(old=>({...old,[image.id]:true}))}/>:<p>{ar?'تعذر تحميل الصورة؛ مرجعها محفوظ أدناه.':'Image unavailable; its source is linked below.'}</p>}
        <figcaption>{source?<a href={source} target="_blank" rel="noopener noreferrer">{image.title||(ar?'مرجع الصورة':'Image source')}</a>:image.title}
          <small>{image.creator||image.provider||'Image library'} · {license?<a href={license} target="_blank" rel="noopener noreferrer">{image.license} {image.licenseVersion}</a>:image.license}</small>
          {image.attribution&&<small>{image.attribution}</small>}{moodboardReferenceUrl(image.creatorUrl)&&<small><a href={moodboardReferenceUrl(image.creatorUrl)} target="_blank" rel="noopener noreferrer">{ar?'صفحة صاحب الصورة':'Photographer profile'}</a></small>}{searchTerm&&<small>{ar?'فكرة البحث: ':'Reference: '}{searchTerm}</small>}
        </figcaption>
      </figure>;
    })}</div>
    <h4>{ar ? 'ملاحظات إبداعية' : 'Creative notes'}</h4>
    <ul>{(preview.notes||[]).map((note,i) => <li key={i}>{note.text}</li>)}</ul>
    <h4>{ar ? 'ألوان مقترحة' : 'Suggested palette'}</h4>
    <div className="creativity-mood-preview__palette">{(preview.palette||[]).map(color => <div key={color.hex}><span style={{backgroundColor:/^#[0-9a-f]{6}$/i.test(color.hex)?color.hex:undefined}} aria-hidden="true"/><strong>{color.name}</strong><code dir="ltr">{color.hex}</code></div>)}</div>
    <p>{ar ? 'اختارت سنا الصور اعتمادًا على تفاصيل مشروعك وبيانات المكتبات الأربع، ولم تتحقق بصريًا من كل صورة؛ راجعي النتائج قبل الحفظ.' : 'SANA selected references from your project and four image libraries using metadata, not visual inspection. Please review before saving.'}</p>
    {preview.images.length<6&&<p>{ar?'بعض المراجع غير متاحة؛ راجعي الصور المتاحة قبل الحفظ.':'Some references were unavailable. Review the available images before saving.'}</p>}
    <small>{ar?'تنتهي صلاحية المسودة: ':'Draft expires: '}{new Date(preview.expiresAt).toLocaleTimeString(ar?'ar-SA':'en-GB')}</small>
    {(preview.warnings||[]).length>0&&<details><summary>{ar?'ملاحظات التوليد':'Generation notes'}</summary><ul>{preview.warnings.map((warning,i)=><li key={i}>{warning}</li>)}</ul></details>}
    {error&&<p role="alert">{error}</p>}
    <div className="modal-actions">{renderActions?.()}</div>
  </section>;
}
