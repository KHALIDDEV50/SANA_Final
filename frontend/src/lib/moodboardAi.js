import {api} from './data';

const base = projectId => `/api/projects/${encodeURIComponent(projectId)}/creativity/moodboard-ai`;
const request = (method, body) => ({method, headers: {'X-Sana-Moodboard': '1'}, body: JSON.stringify(body)});

export const moodboardAi = {
  generate: (projectId, prompt, language) => api(`${base(projectId)}/drafts`, request('POST', {prompt, language})),
  get: (projectId, draftId) => api(`${base(projectId)}/drafts/${encodeURIComponent(draftId)}`, {headers: {'X-Sana-Moodboard': '1'}}),
  approve: (projectId, draftId) => api(`${base(projectId)}/drafts/${encodeURIComponent(draftId)}/approve`, request('POST', {version: 1})),
  reject: (projectId, draftId) => api(`${base(projectId)}/drafts/${encodeURIComponent(draftId)}/reject`, request('POST', {version: 1}))
};

// Only ordinary public HTTPS image/source references returned by the backend are rendered.
export function moodboardReferenceUrl(value) {
  try {
    const url = new URL(value);
    const host = url.hostname.toLowerCase();
    if (url.protocol !== 'https:' || url.username || url.password || !host.includes('.')
        || /^[\d.]+$/.test(host) || host.endsWith('.localhost') || host.endsWith('.local') || host.endsWith('.internal')) return null;
    return url.href;
  } catch { return null; }
}
