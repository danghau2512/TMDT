/* Progressive enhancement only: routes, POST payloads and server validation stay authoritative. */
(() => {
  'use strict';
  document.querySelectorAll('form[data-payment-submit]').forEach(form => {
    form.addEventListener('submit', event => {
      if (form.dataset.submitting) { event.preventDefault(); return; }
      form.dataset.submitting = 'true';
      const button = form.querySelector('button');
      button.disabled = true; button.textContent = form.dataset.paymentSubmit;
      form.setAttribute('aria-busy', 'true');
    });
  });
  window.addEventListener('pageshow', event => { if (event.persisted) location.reload(); });
  const fallback = new URL('../images/product-placeholder.svg', document.currentScript?.src ||
    document.querySelector('script[src*="marketplace.js"]').src).href;
  const imageFallback = image => {
    image.addEventListener('error', () => {
      if (image.src !== fallback) { image.src = fallback; image.alt = 'Ảnh chưa tải được'; }
    });
    if (image.complete && image.naturalWidth === 0) image.src = fallback;
  };
  document.querySelectorAll('img').forEach(imageFallback);
  document.querySelectorAll('.account-menu').forEach(menu => {
    document.addEventListener('click', event => { if (!menu.contains(event.target)) menu.open = false; });
    menu.addEventListener('keydown', event => {
      if (event.key === 'Escape') { menu.open = false; menu.querySelector('summary').focus(); }
    });
    menu.querySelectorAll('a').forEach(link => {
      if (new URL(link.href).pathname === location.pathname) link.setAttribute('aria-current', 'page');
    });
  });
  document.querySelectorAll('input[type="password"]').forEach(input => {
    const wrapper = document.createElement('div'); wrapper.className = 'password-field';
    input.before(wrapper); wrapper.append(input);
    const toggle = document.createElement('button'); toggle.type = 'button';
    toggle.className = 'password-toggle'; toggle.textContent = 'Hiện';
    toggle.setAttribute('aria-label', 'Hiện mật khẩu'); toggle.setAttribute('aria-pressed', 'false');
    toggle.addEventListener('click', () => {
      const show = input.type === 'password'; input.type = show ? 'text' : 'password';
      toggle.textContent = show ? 'Ẩn' : 'Hiện'; toggle.setAttribute('aria-pressed', String(show));
      toggle.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    }); wrapper.append(toggle);
  });
  document.querySelectorAll('.gallery').forEach(gallery => {
    const main = gallery.querySelector('.gallery-main');
    gallery.querySelectorAll('[data-gallery-image]').forEach(link => {
      link.addEventListener('click', event => {
        event.preventDefault(); main.src = link.href;
        gallery.querySelectorAll('[data-gallery-image]').forEach(item => item.removeAttribute('aria-current'));
        link.setAttribute('aria-current', 'true');
      });
    });
  });
  const dialog = document.querySelector('.lightbox');
  const enlarge = (image, trigger) => {
    if (!dialog?.showModal) return;
    trigger.addEventListener('click', event => {
      event.preventDefault(); dialog.querySelector('img').src = image.src;
      dialog.querySelector('img').alt = image.alt; dialog.showModal();
    });
  };
  document.querySelectorAll('.evidence-gallery a, .gallery-enlarge').forEach(link => enlarge(link.querySelector('img'), link));
  dialog?.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
  document.querySelectorAll('input[type="file"][name="photos"]:not([data-photo-picker])').forEach(input => {
    const preview = document.createElement('div'); preview.className = 'image-preview';
    preview.setAttribute('aria-label', 'Xem trước ảnh đã chọn'); input.closest('label').after(preview);
    let urls = [];
    input.addEventListener('change', () => {
      urls.forEach(url => URL.revokeObjectURL(url)); urls = []; preview.replaceChildren();
      [...input.files].slice(0, 5).filter(file => ['image/jpeg', 'image/png'].includes(file.type) && file.size <= 5 * 1024 * 1024)
        .forEach(file => { const image = document.createElement('img'); const url = URL.createObjectURL(file);
          urls.push(url); image.src = url; image.alt = 'Xem trước ảnh đã chọn'; preview.append(image); });
    });
  });
})();
