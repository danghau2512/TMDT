(() => {
  'use strict';
  document.querySelectorAll('[data-document-input]').forEach(input=>{
    let url;const preview=input.parentElement.querySelector('[data-document-preview]');
    input.addEventListener('change',()=>{if(url)URL.revokeObjectURL(url);preview.hidden=true;input.setCustomValidity('');const file=input.files[0];if(!file)return;
      if(!['image/jpeg','image/png'].includes(file.type)||file.size>5*1024*1024){input.setCustomValidity('Chọn ảnh JPEG/PNG tối đa 5 MB.');input.reportValidity();return;}
      url=URL.createObjectURL(file);preview.src=url;preview.hidden=false;const name=input.parentElement.querySelector('[data-document-name]');if(name)name.textContent='Đã chọn ảnh · '+(file.size/(1024*1024)).toFixed(2)+' MB';
    });
  });
  document.querySelectorAll('[data-verification-upload],[data-verification-read]').forEach(form=>form.addEventListener('submit',()=>{
    const loading=form.querySelector('[data-verification-loading]');loading.textContent='Đang đọc mã QR…';loading.hidden=false;form.querySelector('button[type=submit],button:not([type])').disabled=true;
    if(form.matches('[data-verification-read]')){const identity=document.querySelector('[data-identity-form]');for(const key of ['idNumber','fullName','birthDate','gender','residence','issueDate']){const field=identity?.elements[key];if(!field)continue;let hidden=form.querySelector('[name="'+key+'"]');if(!hidden){hidden=document.createElement('input');hidden.type='hidden';hidden.name=key;form.append(hidden);}hidden.value=field.value;}}

  }));
  window.addEventListener('pageshow',event=>{if(event.persisted)document.querySelectorAll('[data-verification-upload],[data-verification-read]').forEach(form=>{form.querySelector('[data-verification-loading]').hidden=true;form.querySelector('button[type=submit],button:not([type])').disabled=false;});});
})();
