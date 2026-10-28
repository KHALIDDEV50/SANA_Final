import {api} from './data';
const request=(method,data)=>({method,body:JSON.stringify(data)});
export const adminApi={
 activity:kind=>api(`/api/v1/${({interactions:'ai-interaction',runs:'tool-run',proposals:'change-proposal'})[kind]}/get`),
 users:()=>api('/api/v1/user/get'),projects:()=>api('/api/v1/project/get'),
 saveUser:(id,data)=>api(`/api/v1/user/${id?`update/${id}`:'add'}`,request(id?'PUT':'POST',data)),
 deleteUser:id=>api(`/api/v1/user/delete/${id}`,{method:'DELETE'}),
 deleteProject:id=>api(`/api/v1/project/delete/${id}`,{method:'DELETE'}),
 saveProject:(project,patch)=>api(`/api/v1/project/update/${project.id}`,request('PUT',{name:project.name,description:project.description,domain:project.domain,status:project.status,version:project.version,...patch}))
};
