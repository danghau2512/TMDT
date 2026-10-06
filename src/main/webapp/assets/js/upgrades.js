(() => {
  'use strict';
  const script=document.querySelector('script[src*="upgrades.js"]');
  const base=new URL(script.src).pathname.split('/assets/')[0];
  const fallback=base+'/assets/images/product-placeholder.svg';
  const dialog=document.querySelector('.lightbox');
  document.querySelectorAll('[data-local-time]').forEach(el=>{
    const raw=el.textContent.trim(),date=new Date(raw.replace(' ','T')+'Z');
    if(!raw || !Number.isFinite(date.getTime()))return;
    el.textContent=new Intl.DateTimeFormat('vi-VN',{timeZone:'Asia/Bangkok',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}).format(date);
    el.setAttribute('datetime',date.toISOString());
  });
  document.querySelectorAll('.review-gallery a,.defect-gallery a').forEach(a=>a.addEventListener('click',e=>{if(!dialog?.showModal)return;e.preventDefault();const img=a.querySelector('img');dialog.querySelector('img').src=img.src;dialog.querySelector('img').alt=img.alt;dialog.showModal();}));
  document.querySelectorAll('[data-photo-picker]').forEach(input=>{
    const preview=document.createElement('div');preview.className='photo-picker';input.closest('label').after(preview);
    const message=document.createElement('p');message.className='photo-picker-error';message.setAttribute('role','alert');preview.after(message);
    let files=[],urls=[];const max=Number(input.dataset.photoPicker);
    function render(){urls.forEach(URL.revokeObjectURL);urls=[];preview.replaceChildren();
      const transfer=new DataTransfer();files.forEach(f=>transfer.items.add(f));input.files=transfer.files;
      files.forEach((file,index)=>{const card=document.createElement('div');card.className='photo-choice';const img=document.createElement('img');img.src=URL.createObjectURL(file);urls.push(img.src);img.alt='Xem trước ảnh '+(index+1);card.append(img);
        if(input.dataset.defectPicker!==undefined){const label=document.createElement('label'),check=document.createElement('input');check.type='checkbox';check.name='defectIndex';check.value=String(index);check.checked=file.defect===true;check.addEventListener('change',()=>file.defect=check.checked);label.append(check,document.createTextNode('Ảnh khuyết điểm'));card.append(label);}
        const remove=document.createElement('button');remove.type='button';remove.className='secondary';remove.textContent='Bỏ ảnh '+(index+1);remove.addEventListener('click',()=>{files.splice(index,1);render();});card.append(remove);preview.append(card);
      });
      // When replacing the image set, old markings no longer apply.
      document.querySelectorAll('input[name="defectAsset"]').forEach(c=>c.disabled=files.length>0);
    }
    input.addEventListener('change',()=>{const chosen=[...input.files];if(chosen.length>max){message.textContent='Chỉ được chọn tối đa '+max+' ảnh.';files=[];render();return;}
      if(chosen.some(f=>!['image/jpeg','image/png'].includes(f.type)||f.size>5*1024*1024)){message.textContent='Chọn JPEG/PNG, tối đa 5 MB mỗi ảnh.';files=[];render();return;}
      message.textContent='';files=chosen;render();});
  });
  const action=document.querySelector('select[name="action"]');
  const formError=document.querySelector('.error[role="alert"]');
  if(formError){const fields={appearance:'Ngoại hình',operation:'Hoạt động',repair:'Lịch sử sửa chữa',defects:'Lỗi đã biết',repairDetails:'Chi tiết sửa chữa',accessories:'Phụ kiện',comment:'Nhận xét',rating:'Số sao',response:'Phản hồi',summary:'Kết quả xử lý'};
    for(const [name,label] of Object.entries(fields)){const input=document.querySelector('[name="'+name+'"]');if(input&&formError.textContent.includes(label)){input.setAttribute('aria-invalid','true');const error=document.createElement('small');error.className='field-error';error.textContent=formError.textContent;input.closest('label,fieldset').append(error);break;}}}
  if(action){const update=()=>{const resolve=action.value==='RESOLVE';document.querySelectorAll('[data-resolution-fields]').forEach(el=>{el.hidden=!resolve;el.querySelectorAll('select,textarea').forEach(f=>f.required=resolve);});};action.addEventListener('change',update);update();}
  document.querySelector('[data-only-different]')?.addEventListener('change',e=>document.querySelectorAll('.compare-table tr[data-different="false"]').forEach(row=>row.hidden=e.target.checked));
  const KEY='c2c.compare.ids';let ids=[];try{const saved=JSON.parse(localStorage.getItem(KEY)||'[]');if(Array.isArray(saved))ids=[...new Set(saved.filter(id=>Number.isSafeInteger(id)&&id>0))].slice(0,3);}catch{ids=[];}
  const showTray=['','/','/home','/products','/products/detail','/categories'].includes(location.pathname.slice(base.length));
  if(!showTray && location.pathname!==base+'/compare')return;
  const tray=document.createElement('aside');tray.className='compare-tray';tray.hidden=true;tray.setAttribute('aria-label','Sản phẩm đang chọn so sánh');
  const list=document.createElement('div');list.className='compare-tray-items';const actions=document.createElement('div');actions.className='compare-tray-actions';const link=document.createElement('a');link.className='button';link.textContent='So sánh ngay';const status=document.createElement('p');status.className='compare-tray-status';status.setAttribute('role','status');actions.append(link,status);tray.append(list,actions);document.body.append(tray);
  let products=[],unavailable=[],same=true,request=0;
  function toast(text){document.querySelector('.compare-toast')?.remove();const box=document.createElement('div');box.className='compare-toast';box.setAttribute('role','status');box.textContent=text;document.body.append(box);setTimeout(()=>box.remove(),4500);}
  function persist(){try{localStorage.setItem(KEY,JSON.stringify(ids));}catch{toast('Trình duyệt không lưu được lựa chọn; danh sách chỉ giữ trong trang này.');}}
  function buttons(){document.querySelectorAll('[data-compare-id]').forEach(b=>{const chosen=ids.includes(Number(b.dataset.compareId));b.setAttribute('aria-pressed',String(chosen));b.textContent=chosen?'✓ Đã chọn · bỏ chọn':'⇄ So sánh';});}
  function draw(){tray.hidden=ids.length===0||!showTray;document.body.classList.toggle('has-compare-tray',ids.length>0&&showTray);list.replaceChildren();buttons();
    ids.forEach(id=>{const p=products.find(p=>Number(p.id)===id);const item=document.createElement('div');item.className='compare-selected';const img=document.createElement('img');img.src=p?.image_id?base+'/media/product?asset='+p.image_id:fallback;img.alt='';const title=document.createElement('span');title.textContent=p?.title||(unavailable.includes(id)?'Không còn hiển thị':'Đang tải…');const remove=document.createElement('button');remove.type='button';remove.textContent='×';remove.setAttribute('aria-label','Bỏ '+(p?.title||'sản phẩm'));remove.addEventListener('click',()=>removeId(id));item.append(img,title,remove);list.append(item);});
    const valid=ids.length>=2&&products.length===ids.length&&same;link.setAttribute('aria-disabled',String(!valid));link.href=base+'/compare?ids='+ids.join(',');status.textContent=!same?'Chọn sản phẩm cùng danh mục.':unavailable.length?'Bỏ sản phẩm không còn hiển thị.':ids.length+' / 3 sản phẩm · '+(ids.length<2?'chọn ít nhất 2':'cùng danh mục');
  }
  async function hydrate(){const current=++request;products=[];unavailable=[];draw();if(!ids.length)return;status.textContent='Đang tải thông tin mới nhất…';try{const response=await fetch(base+'/compare/items?ids='+ids.join(','),{headers:{Accept:'application/json'},cache:'no-store'});const data=await response.json();if(!response.ok)throw Error(data.error||'Không tải được sản phẩm.');if(current!==request)return;products=data.comparedProducts;unavailable=data.unavailableIds.map(Number);same=data.sameCategory;draw();}catch(e){if(current===request){status.textContent='Không tải được. Bấm thử lại.';const retry=document.createElement('button');retry.type='button';retry.className='secondary';retry.textContent='Thử lại';retry.addEventListener('click',()=>{retry.remove();hydrate();});status.append(retry);}}}
  function removeId(id){ids=ids.filter(i=>i!==id);persist();hydrate();if(location.pathname===base+'/compare')location.href=base+'/compare?ids='+ids.join(',');}
  document.querySelectorAll('[data-compare-id]').forEach(b=>b.addEventListener('click',()=>{const id=Number(b.dataset.compareId);if(ids.includes(id)){removeId(id);return;}if(ids.length>=3){toast('Chỉ so sánh tối đa 3 sản phẩm. Hãy bỏ một sản phẩm trước.');return;}
    const p=products.find(p=>Number(p.id)===ids[0]);if(p&&Number(p.category_id)!==Number(b.dataset.compareCategory)){toast('Vui lòng chọn các sản phẩm cùng danh mục.');return;}
    ids.push(id);persist();hydrate();}));
  document.querySelectorAll('[data-remove-compare]').forEach(b=>b.addEventListener('click',()=>{const id=Number(b.dataset.removeCompare);const selected=new URLSearchParams(location.search).get('ids');if(selected)ids=selected.split(',').map(Number).filter(i=>Number.isSafeInteger(i)&&i>0);removeId(id);}));
  // A directly shared comparison URL becomes the current selection; only IDs are stored.
  if(location.pathname===base+'/compare'){const raw=new URLSearchParams(location.search).get('ids');if(raw){const selected=[...new Set(raw.split(',').map(Number))].filter(i=>Number.isSafeInteger(i)&&i>0);if(selected.length<=3){ids=selected;persist();}}}
  window.addEventListener('storage',e=>{if(e.key===KEY){try{ids=JSON.parse(e.newValue||'[]').filter(i=>Number.isSafeInteger(i)&&i>0).slice(0,3);hydrate();}catch{}}});
  hydrate();
})();
