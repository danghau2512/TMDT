(() => {
  'use strict';
  const root = document.querySelector('[data-chat]');
  const badge = document.querySelector('[data-chat-unread]');
  const context = root?.dataset.context || badge?.dataset.context || '';
  const visible = () => document.visibilityState === 'visible';
  const count = value => {
    if (!badge) return;
    badge.textContent = String(value); badge.hidden = Number(value) === 0;
    badge.setAttribute('aria-label', `${value} tin chưa đọc`);
  };
  async function request(path, fields) {
    const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const options = {credentials: 'same-origin', headers: {Accept: 'application/json'}, signal: controller.signal};
      if (fields) { options.method = 'POST'; options.body = new URLSearchParams(fields); }
      const response = await fetch(context + path, options);
      let data; try { data = await response.json(); } catch { throw new Error('Chưa nhận được phản hồi. Bản nháp vẫn được giữ; hãy thử lại.'); }
      if (!response.ok) throw new Error(data.error || 'Không thể xử lý yêu cầu. Hãy thử lại.');
      return data;
    } catch (error) {
      // Fetch báo TypeError bằng tiếng Anh khi mất mạng; giữ thông báo UI tiếng Việt.
      if (error instanceof TypeError) throw new Error('Không thể kết nối. Vui lòng thử lại.');
      throw error;
    } finally { clearTimeout(timeout); }
  }
  if (!root) {
    if (!badge) return;
    let timer, busy = false;
    const update = async () => {
      if (!visible() || busy) return; busy = true;
      try { count((await request('/messages/unread')).unreadTotal); } catch { /* Trang chính vẫn dùng được khi chat chưa sẵn sàng. */ }
      finally { busy = false; if (visible()) timer = setTimeout(update, 20000); }
    };
    document.addEventListener('visibilitychange', () => { clearTimeout(timer); if (visible()) update(); });
    update(); return;
  }
  const id = root.dataset.conversation;
  const list = root.querySelector('[data-chat-list]');
  const connection = root.querySelector('[data-chat-connection]');
  const history = root.querySelector('[data-chat-history]');
  const messages = root.querySelector('[data-chat-messages]');
  const older = root.querySelector('[data-chat-older]');
  const newButton = root.querySelector('[data-chat-new]');
  const form = root.querySelector('[data-chat-form]');
  const status = root.querySelector('[data-chat-send-status]');
  const seen = new Set([...root.querySelectorAll('[data-message-id]')].map(e => e.dataset.messageId));
  const compare = (a,b) => BigInt(a) < BigInt(b) ? -1 : BigInt(a) > BigInt(b) ? 1 : 0;
  let cursor = [...seen].sort(compare).at(-1) || '0';
  let acknowledged = '0', polling = false, timer, reading = false, sending = false;
  const bottom = () => !history || history.scrollHeight - history.scrollTop - history.clientHeight < 85;
  const end = () => { if (history) history.scrollTop = history.scrollHeight; if (newButton) newButton.hidden = true; };
  const node = (tag, cls, text) => { const e = document.createElement(tag); if (cls) e.className = cls; if (text !== undefined) e.textContent = text; return e; };
  function renderList(items) {
    const scroll = list.scrollTop; const focused = document.activeElement?.closest('[data-chat-id]')?.dataset.chatId;
    const fragment = document.createDocumentFragment();
    for (const c of items) {
      const a = node('a', `chat-preview${c.id === id ? ' is-selected' : ''}`); a.dataset.chatId = c.id;
      a.href = context + '/messages?id=' + encodeURIComponent(c.id); if (c.id === id) a.setAttribute('aria-current','page');
      const image = node('img'); image.src = context + c.imagePath; image.alt = ''; a.append(image);
      const copy = node('span','chat-preview-copy'), top = node('span','chat-preview-top'); top.append(node('strong','',c.otherName));
      if (Number(c.unread) > 0) { const n = node('span','chat-count',String(c.unread)); n.setAttribute('aria-label','Tin chưa đọc'); top.append(n); }
      copy.append(top,node('span','chat-preview-title',c.title),node('span','chat-preview-body',c.lastBody || 'Chưa có tin nhắn · Hãy chào nhau!'),node('time','',c.when));
      a.append(copy); fragment.append(a);
    }
    if (!items.length) { const empty = node('div','chat-list-empty'); empty.append(node('strong','','Hộp thư đang trống'),node('p','','Chọn “Chat với người bán” tại một sản phẩm để bắt đầu.')); fragment.append(empty); }
    list.replaceChildren(fragment); list.scrollTop = scroll;
    if (focused) [...list.querySelectorAll('[data-chat-id]')].find(e => e.dataset.chatId === focused)?.focus({preventScroll:true});
  }
  function append(items) {
    let added = false;
    for (const m of items) {
      if (seen.has(m.id)) continue; seen.add(m.id); added = true;
      const bubble = node('div', `chat-message${m.mine ? ' is-mine' : ''}`); bubble.dataset.messageId = m.id;
      bubble.append(node('p','',m.body),node('time','',m.sentAt));
      const next = [...messages.children].find(e => compare(e.dataset.messageId,m.id) > 0);
      messages.insertBefore(bubble,next || null);
    }
    if (added) root.querySelector('[data-chat-empty]').hidden = true;
    return added;
  }
  function metadata(c) {
    const verification=root.querySelector('[data-chat-verification]');if(verification)verification.hidden=!c.sellerVerified;
    root.querySelector('[data-chat-product-title]').textContent = c.title;
    root.querySelector('[data-chat-product-image]').src = context + c.imagePath;
    root.querySelector('[data-chat-product-state]').textContent = c.available ? 'Đang được công khai' : 'Tin không còn công khai · Lịch sử vẫn được giữ';
    const link = root.querySelector('[data-chat-product-link]'); link.hidden = !c.available; if (c.available) link.href = context + c.productPath; else link.removeAttribute('href');
    if (form && !sending) { form.elements.body.disabled = !c.canSend; form.querySelector('button').disabled = !c.canSend; }
    if (!c.canSend && status) status.textContent = 'Người còn lại đã ngừng hoạt động; bạn vẫn xem được lịch sử.';
  }
  async function markRead() {
    // Chỉ ACK đến cursor đã tải và đang nhìn thấy; không ACK ID của send trước khi poll bù tin.
    if (!id || !visible() || !bottom() || reading || cursor === '0' || compare(cursor,acknowledged) <= 0) return;
    reading = true; const through = cursor;
    try { const r = await request('/messages/read',{csrfToken:root.dataset.csrf,conversationId:id,through}); acknowledged = through; count(r.unreadTotal); }
    catch { connection.textContent = 'Chưa đánh dấu đã đọc; sẽ thử lại.'; }
    finally { reading = false; }
  }
  async function poll() {
    if (!visible() || polling) return; polling = true; let delay = 4000;
    try {
      const data = await request('/messages/api' + (id ? `?conversationId=${encodeURIComponent(id)}&cursor=${encodeURIComponent(cursor)}` : ''));
      const atBottom = bottom();
      if (id) {
        const added = append(data.messages); if (data.messages.length) cursor = data.messages.at(-1).id;
        metadata(data.conversation);
        if (added && atBottom) end(); else if (added) newButton.hidden = false;
        if (data.more) delay = 250;
      }
      renderList(data.conversations); count(data.unreadTotal); connection.textContent = 'Tự cập nhật khi mở trang';
      await markRead();
      // Đồng bộ badge của thread sau ACK; poll sau vẫn kiểm lại số đếm từ server.
      if (id && bottom() && acknowledged === cursor && !data.more) {
        list.querySelector('[aria-current="page"] .chat-count')?.remove();
      }
    } catch (error) { connection.textContent = error.name === 'AbortError' ? 'Kết nối chậm; đang thử lại…' : error.message; delay = 5000; }
    finally { polling = false; if (visible()) { clearTimeout(timer); timer = setTimeout(poll,delay); } }
  }
  document.addEventListener('visibilitychange', () => { clearTimeout(timer); if (visible()) poll(); });
  if (form) {
    const input = form.elements.body, nonce = form.elements.nonce, length = root.querySelector('[data-chat-length]');
    const updateLength = () => length.textContent = String([...input.value].length); input.addEventListener('input',updateLength); updateLength();
    let previous = input.value;
    form.addEventListener('submit', async event => {
      event.preventDefault(); if (sending) return;
      if (!input.value.trim()) { status.textContent = 'Hãy nhập nội dung tin nhắn.'; status.classList.add('is-error'); return; }
      // Retry cùng nội dung giữ nonce. Nếu người dùng sửa bản nháp, dùng nonce mới.
      if (previous !== input.value) { nonce.value = crypto.randomUUID(); previous = input.value; }
      const draft = input.value; sending = true; form.querySelector('button').disabled = true; input.readOnly = true;
      status.classList.remove('is-error'); status.textContent = 'Đang gửi…';
      try {
        const data = await request('/messages/send',{csrfToken:root.dataset.csrf,conversationId:id,nonce:nonce.value,body:draft});
        const atBottom = bottom(); append([data.message]); if (atBottom) end(); else newButton.hidden = false;
        input.value = ''; previous = ''; nonce.value = crypto.randomUUID(); updateLength(); status.textContent = 'Đã gửi tin nhắn.';
        clearTimeout(timer); if (!polling) timer = setTimeout(poll,100);
      } catch (error) { status.textContent = error.name === 'AbortError' ? 'Chưa nhận được xác nhận. Nội dung vẫn được giữ; bấm Gửi để thử lại.' : error.message; status.classList.add('is-error'); }
      finally { sending = false; input.readOnly = false; form.querySelector('button').disabled = input.disabled; input.focus({preventScroll:true}); }
    });
    newButton.addEventListener('click',() => {end(); markRead();});
    history.addEventListener('scroll',() => { if (bottom()) {newButton.hidden = true; markRead();} },{passive:true});
    older.addEventListener('click', async () => {
      if (older.disabled) return; older.disabled = true;
      try {
        const first = [...seen].sort(compare)[0] || '0';
        const data = await request(`/messages/api?conversationId=${encodeURIComponent(id)}&direction=older&cursor=${encodeURIComponent(first)}`);
        const height = history.scrollHeight, top = history.scrollTop; append(data.messages); older.hidden = !data.more;
        history.scrollTop = top + history.scrollHeight - height;
      } catch { connection.textContent = 'Chưa tải được tin trước. Hãy thử lại.'; }
      finally { older.disabled = false; }
    });
    end(); markRead();
  }
  poll();
})();
