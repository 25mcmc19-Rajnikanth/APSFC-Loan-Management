const fs = require('fs');
const dir = __dirname.replace(/\\$/, '') + '/../../assets/js';
const appId = process.argv[2];
const token = process.argv[3];

const src = `
const storage={};const localStorage={getItem:k=>(k in storage?storage[k]:null),setItem:(k,v)=>{storage[k]=String(v)},removeItem:k=>{delete storage[k]}};
const window={location:{hostname:"localhost",hash:""}};const location={hash:"",href:""};
const document={querySelector:()=>null,getElementById:()=>({addEventListener:()=>{},value:"",appendChild:()=>{},innerHTML:""}),
  createElement:()=>({className:"",textContent:"",classList:({add:()=>{}}),appendChild:()=>{},remove:()=>{}}),body:{appendChild:()=>{}}};
function toast(){}function modal(){}function closeModal(){}
${fs.readFileSync(dir+'/api.js','utf8')}
${fs.readFileSync(dir+'/common.js','utf8')}
${fs.readFileSync(dir+'/views.js','utf8')}
(async()=>{
  ace.token="${token}";
  const html=await Views["app-detail-edit"](null,null,${appId});
  const bad=/undefined|NaN|\\[object Object\\]/.test(html.replace(/<script[\\s\\S]*?<\\/script>/g,""));
  console.log("edit view renders:", html.length, "chars; bad:" + bad);
  console.log("has product select:", /<select name="productId">/.test(html), "| prefilled amount:", /value="1300000"/.test(html));
})().catch(e=>console.error("ERR",e.message));
`;
(0, eval)(src);

(async () => {
  const ace2 = { token: token };
  const r = await fetch('http://localhost:8080/api/loan-applications/' + appId, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify({ productId: 5, loanAmount: 1355000, tenureMonths: 30, purpose: 'Home renovation (updated)' })
  });
  const j = await r.json();
  console.log('PUT draft update ->', r.status, '| amount:', j.loanAmount, '| tenure:', j.tenureMonths, '| purpose:', j.purpose, '| status:', j.status);
})().catch(e => console.error('PUT ERR', e.message));