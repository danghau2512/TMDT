(() => {
  'use strict';
  const form=document.querySelector('[data-search-suggestions]');
  if(!form)return;
  const input=form.elements.keyword,panel=form.querySelector('.search-suggestions');
  const list=panel.querySelector('[role=listbox]'),status=panel.querySelector('[data-search-status]');
  const all=panel.querySelector('.search-all'),announcement=form.querySelector('[data-search-announcement]');
  const endpoint=new URL(form.dataset.suggestUrl,location.origin);
  const context=endpoint.pathname.slice(0,-'/products/suggestions'.length);
  const currency=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0});
  let timer,controller,revision=0,selected=-1,composing=false;
  const invalidate=()=>{++revision;clearTimeout(timer);controller?.abort();list.setAttribute('aria-busy','false');};
  const close=()=>{invalidate();panel.hidden=true;input.setAttribute('aria-expanded','false');input.removeAttribute('aria-activedescendant');selected=-1;};
  const show=()=>{panel.hidden=false;input.setAttribute('aria-expanded','true');};
  const message=text=>{status.textContent=text;status.hidden=!text;announcement.textContent=text;};
  function choose(index) {
    const links=[...list.children];if(!links.length)return;
    selected=(index+links.length)%links.length;
    links.forEach((link,i)=>{link.classList.toggle('is-active',i===selected);link.setAttribute('aria-selected',String(i===selected));});
    input.setAttribute('aria-activedescendant',links[selected].id);links[selected].scrollIntoView({block:'nearest'});
  }
  function render(items) {
    list.replaceChildren();selected=-1;input.removeAttribute('aria-activedescendant');
    items.forEach((p,index)=>{
      const link=document.createElement('a');link.className='search-result';link.href=context+p.productPath;
      link.id='search-result-'+index;link.setAttribute('role','option');link.setAttribute('aria-selected','false');link.tabIndex=-1;
      const image=document.createElement('img');image.src=context+p.imagePath;image.alt='';
      image.addEventListener('error',()=>{const fallback=context+'/assets/images/product-placeholder.svg';if(image.getAttribute('src')!==fallback)image.src=fallback;});
      const copy=document.createElement('span');copy.className='search-result-copy';
      const title=document.createElement('span');title.className='search-result-title';title.textContent=p.title;
      const price=document.createElement('span');price.className='search-result-price';price.textContent=currency.format(p.price);
      copy.append(title,price);link.append(image,copy);list.append(link);
      link.addEventListener('pointermove',()=>{if(selected!==index)choose(index);});
    });
    message(items.length?'':'Chưa tìm thấy sản phẩm phù hợp.');
    if(items.length)announcement.textContent=items.length+' gợi ý. Dùng phím lên xuống để chọn, Enter để xem sản phẩm.';
  }
  async function load(keyword,current) {
    controller=new AbortController();const activeController=controller;
    const timeout=setTimeout(()=>activeController.abort(),8000);
    list.replaceChildren();selected=-1;input.removeAttribute('aria-activedescendant');list.setAttribute('aria-busy','true');show();message('Đang tìm sản phẩm…');
    try {
      const url=new URL(endpoint);url.searchParams.set('keyword',keyword);
      const response=await fetch(url,{signal:activeController.signal,headers:{Accept:'application/json'},cache:'no-store'});
      const data=await response.json();
      if(current!==revision)return;
      if(!response.ok||!Array.isArray(data.products))throw new Error('Search unavailable');
      render(data.products);
    } catch(error) {
      if(current!==revision)return;
      list.replaceChildren();message('Chưa tải được gợi ý. Bạn vẫn có thể bấm Tìm kiếm.');
    } finally {clearTimeout(timeout);if(current===revision)list.setAttribute('aria-busy','false');}
  }
  function schedule() {
    invalidate();panel.hidden=true;input.setAttribute('aria-expanded','false');input.removeAttribute('aria-activedescendant');selected=-1;
    const keyword=input.value.trim();if(!keyword||composing)return;
    const url=new URL(form.action);url.searchParams.set('keyword',keyword);all.href=url.href;
    const current=revision;timer=setTimeout(()=>load(keyword,current),250);
  }
  input.addEventListener('input',event=>{if(!event.isComposing)schedule();});
  input.addEventListener('focus',schedule);
  input.addEventListener('compositionstart',()=>{composing=true;close();});
  input.addEventListener('compositionend',()=>{composing=false;schedule();});
  input.addEventListener('keydown',event=>{
    if(composing||event.isComposing)return;
    if(event.key==='Escape'){event.preventDefault();close();}
    else if(!panel.hidden&&(event.key==='ArrowDown'||event.key==='ArrowUp')){event.preventDefault();choose(selected<0?(event.key==='ArrowDown'?0:list.children.length-1):selected+(event.key==='ArrowDown'?1:-1));}
    else if(event.key==='Enter'&&!panel.hidden&&selected>=0){event.preventDefault();location.assign(list.children[selected].href);}
  });
  form.addEventListener('submit',close);
  document.addEventListener('pointerdown',event=>{if(!form.contains(event.target))close();});
  form.addEventListener('focusout',event=>{if(!form.contains(event.relatedTarget))close();});
  document.addEventListener('visibilitychange',()=>{if(document.hidden)close();});
})();
