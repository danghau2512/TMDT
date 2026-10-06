(() => {
  'use strict';
  const root=document.querySelector('[data-cart]');if(!root)return;
  const feedback=root.querySelector('[data-cart-feedback]'),status=root.querySelector('[data-cart-status]');
  const refresh=root.querySelector('[data-cart-refresh]'),reload=root.querySelector('[data-cart-reload]');
  const checkout=root.querySelector('[data-cart-checkout]');
  const money=new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0});
  let busy=false,uncertain=false,needsReload=false;
  const rows=()=>[...root.querySelectorAll('[data-cart-item]')];
  const tell=(text,error=false)=>{feedback.hidden=false;feedback.classList.toggle('is-error',error);status.textContent=text;};
  function controls() {
    for(const row of rows()) {
      const input=row.querySelector('[data-cart-quantity]'),quantity=Number(input.dataset.confirmedQuantity),max=Number(input.dataset.maxQuantity);
      const canChange=row.dataset.canChange==='true';
      input.disabled=!canChange;input.readOnly=busy||uncertain||needsReload;
      row.querySelector('[data-cart-step="-1"]').disabled=busy||uncertain||needsReload||!canChange||quantity<=1;
      row.querySelector('[data-cart-step="1"]').disabled=busy||uncertain||needsReload||!canChange||quantity>=max;
      row.querySelector('[data-cart-remove] button').disabled=busy||uncertain||needsReload;
    }
    const blocked=busy||uncertain||needsReload||rows().length===0||!!root.querySelector('[data-cart-unavailable]:not([hidden])');
    checkout.setAttribute('aria-disabled',String(blocked));checkout.tabIndex=blocked?-1:0;
    root.setAttribute('aria-busy',String(busy));refresh.disabled=busy;refresh.hidden=!uncertain;reload.hidden=!needsReload;
  }
  function apply(cart) {
    if(!Array.isArray(cart.items)||typeof cart.total!=='number')throw new Error('Invalid cart response');
    const current=new Map(rows().map(row=>[row.dataset.productId,row])),ids=new Set(cart.items.map(item=>item.id));
    needsReload=cart.items.some(item=>!current.has(item.id));
    for(const [id,row]of current)if(!ids.has(id))row.remove();
    for(const item of cart.items) {
      const row=current.get(item.id);if(!row)continue;
      const input=row.querySelector('[data-cart-quantity]');input.value=String(item.quantity);input.dataset.confirmedQuantity=String(item.quantity);input.dataset.maxQuantity=String(item.maxQuantity);
      row.dataset.canChange=String(item.canChangeQuantity);row.querySelector('[data-cart-count]').textContent=String(item.quantity);
      row.querySelector('[data-cart-unit-price]').textContent=money.format(item.price);row.querySelector('[data-cart-line-total]').textContent=money.format(item.lineTotal);
      row.querySelector('[data-cart-unavailable]').hidden=item.available;
    }
    root.querySelectorAll('[data-cart-total]').forEach(node=>node.textContent=money.format(cart.total));
    root.querySelector('[data-cart-empty]').hidden=cart.items.length!==0;root.querySelector('[data-cart-layout]').hidden=cart.items.length===0;
    uncertain=false;controls();
    if(needsReload)tell('Giỏ hàng đã thay đổi ở cửa sổ khác. Tải lại giỏ hàng để xem đầy đủ.',true);
  }
  async function request(url,fields) {
    const controller=new AbortController(),timeout=setTimeout(()=>controller.abort(),15000);
    try {
      const response=await fetch(url,{method:fields?'POST':'GET',credentials:'same-origin',headers:{Accept:'application/json'},body:fields?new URLSearchParams(fields):undefined,cache:'no-store',signal:controller.signal});
      let data;try{data=await response.json();}catch{throw new Error('Chưa nhận được phản hồi giỏ hàng. Vui lòng thử lại.');}
      if(!response.ok){const error=new Error(data.error||'Không thể cập nhật giỏ hàng.');error.status=response.status;throw error;}
      return data;
    } catch(error) {
      if(error.name==='AbortError'||error instanceof TypeError)throw new Error('Chưa xác nhận được thay đổi do kết nối gián đoạn. Vui lòng kiểm tra lại giỏ hàng.');
      throw error;
    } finally {clearTimeout(timeout);}
  }
  function restore(){rows().forEach(row=>{const input=row.querySelector('[data-cart-quantity]');input.value=input.dataset.confirmedQuantity;});}
  async function send(form,quantity,remove,trigger) {
    if(busy||uncertain||needsReload)return;
    if(!remove&&(!Number.isInteger(quantity)||quantity<1||quantity>999)){restore();tell('Số lượng phải là số nguyên từ 1 đến 999.',true);return;}
    busy=true;controls();tell(remove?'Đang xóa sản phẩm…':'Đang cập nhật giỏ hàng…');
    try {
      apply(await request(form.action,{csrfToken:form.elements.csrfToken.value,productId:form.elements.productId.value,...(remove?{}:{quantity:String(quantity)})}));
      if(!needsReload)tell(remove?'Đã xóa sản phẩm khỏi giỏ.':'Đã cập nhật số lượng và tổng tiền.');
    } catch(error) {
      restore();
      // POST có thể đã ghi dù response bị mất; đọc lại trạng thái server trước khi cho sửa tiếp.
      try {apply(await request(root.dataset.cartUrl));}
      catch {uncertain=true;}
      if(!needsReload)tell(error.message+(uncertain?' Nhấn “Kiểm tra lại giỏ hàng” để đồng bộ.':''),true);
    } finally {busy=false;controls();if(trigger?.isConnected&&!trigger.disabled)trigger.focus({preventScroll:true});}
  }
  for(const row of rows()) {
    const form=row.querySelector('[data-cart-quantity-form]'),input=form.elements.quantity;
    form.querySelectorAll('[data-cart-step]').forEach(button=>button.addEventListener('click',event=>{event.preventDefault();send(form,Number(input.value)+Number(button.dataset.cartStep),false,button);}));
    form.addEventListener('submit',event=>{event.preventDefault();send(form,Number(input.value),false,input);});
    input.addEventListener('change',()=>send(form,Number(input.value),false,input));
    input.addEventListener('keydown',event=>{if(event.key==='Enter'){event.preventDefault();send(form,Number(input.value),false,input);}});
    const remove=row.querySelector('[data-cart-remove]');remove.addEventListener('submit',event=>{event.preventDefault();send(remove,0,true);});
  }
  checkout.addEventListener('click',event=>{if(checkout.getAttribute('aria-disabled')==='true')event.preventDefault();});
  refresh.addEventListener('click',async()=>{if(busy)return;busy=true;controls();try{apply(await request(root.dataset.cartUrl));if(!needsReload)tell('Đã đồng bộ lại giỏ hàng.');}catch(error){tell(error.message,true);}finally{busy=false;controls();}});
  controls();
})();
