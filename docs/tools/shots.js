// Chụp ảnh giao diện UTELearn (app test chạy ở cổng 8089, dữ liệu mẫu từ DataSeeder)
const fs = require('fs');
const path = require('path');
const { launch, sleep } = require('./cdp');

const B = 'http://localhost:8089';
const PROJECT = 'E:/Document_3rd_grade/web_dev/Final_project/Kinh doanh khóa học trực tuyến và quản lý học tập số UTELearn';
const OUT = path.join(PROJECT, 'docs/images/ui');
fs.mkdirSync(OUT, { recursive: true });
const only = process.argv[2];

async function login(page, email, password) {
  await page.goto(B + '/auth/login', 600);
  await page.evalNav(`(() => {
    document.querySelector('input[name=email]').value = ${JSON.stringify(email)};
    document.querySelector('input[name=password]').value = ${JSON.stringify(password)};
    document.querySelector('form[action="/auth/login"]').submit();
  })()`, 800);
}

async function shot(page, name, url, opts = {}) {
  if (only && !name.includes(only)) return;
  if (url) await page.goto(B + url, opts.wait || 1500);
  if (opts.before) await page.eval(opts.before);
  if (opts.before) await sleep(400);
  await page.shotPage(path.join(OUT, name + '.png'), opts.maxHeight || 1000);
  console.log('OK', name);
}

(async () => {
  const cdp = await launch();

  // ===== Khách =====
  let ctx = await cdp.newContext();
  let p = await cdp.newPage(ctx, 1366, 860, 1);
  await shot(p, 'ui-01-trang-chu', '/', { maxHeight: 1500 });
  await shot(p, 'ui-02-danh-sach-khoa-hoc', '/courses?sort=newest', { maxHeight: 1250 });
  await shot(p, 'ui-03-tim-kiem-loc', '/courses?keyword=react&price=paid');
  await shot(p, 'ui-04-chi-tiet-khoa-hoc', '/courses/1', { maxHeight: 1300 });
  await shot(p, 'ui-05-dang-nhap', '/auth/login', { maxHeight: 760 });
  await shot(p, 'ui-06-dang-ky', '/auth/register', { maxHeight: 900 });
  // đăng ký sai để thấy thông báo validate
  if (!only || 'ui-07'.includes(only) || only === 'ui-07') {
    await p.goto(B + '/auth/register', 600);
    await p.evalNav(`(() => { document.querySelector('#fullName').value=''; document.querySelector('#email').value='abc';
      document.querySelector('#password').value='123'; document.querySelector('form[action="/auth/register"]').submit(); })()`, 800);
    await shot(p, 'ui-07-dang-ky-loi', null, { maxHeight: 900 });
  }

  // ===== Học viên =====
  ctx = await cdp.newContext();
  p = await cdp.newPage(ctx, 1366, 860, 1);
  await login(p, 'cuong.le@gmail.com', '123456');
  await shot(p, 'ui-08-chi-tiet-da-mua', '/courses/1', { maxHeight: 900 });
  await p.goto(B + '/courses/4', 800);
  await p.evalNav(`document.querySelector('form[action="/cart/add/4"] input[name=buyNow]').closest('form').submit()`, 800);
  await shot(p, 'ui-09-gio-hang', '/cart', { maxHeight: 900 });
  await p.goto(B + '/cart', 600);
  await p.evalNav(`document.querySelector('form[action="/checkout"]').submit()`, 1000);
  await shot(p, 'ui-10-cong-thanh-toan', null, { maxHeight: 980 });
  await p.evalNav(`document.querySelector('form[action$="/confirm"]').submit()`, 1000);
  await shot(p, 'ui-11-thanh-toan-thanh-cong', null, { maxHeight: 980 });
  await shot(p, 'ui-12-don-hang', '/orders', { maxHeight: 700 });
  await shot(p, 'ui-13-khoa-hoc-cua-toi', '/my-courses', { maxHeight: 900 });
  await shot(p, 'ui-14-hoc-bai', '/learn/1?lessonId=3', { maxHeight: 1100, wait: 2500 });
  await shot(p, 'ui-15-chat-hoc-vien', '/learn/1/chat', { maxHeight: 980, wait: 2500 });
  await shot(p, 'ui-16-ho-so', '/profile', { maxHeight: 900 });
  await shot(p, 'ui-17-403', '/admin', { maxHeight: 700 });

  // ===== Giảng viên =====
  ctx = await cdp.newContext();
  p = await cdp.newPage(ctx, 1366, 860, 1);
  await login(p, 'an.nguyen@utelearn.vn', '123456');
  await shot(p, 'ui-18-gv-khoa-hoc', '/instructor/courses', { maxHeight: 900 });
  await shot(p, 'ui-19-gv-tao-khoa-hoc', '/instructor/courses/new', { maxHeight: 1000 });
  await shot(p, 'ui-20-gv-sua-khoa-hoc', '/instructor/courses/1/edit', { maxHeight: 1000 });
  await shot(p, 'ui-21-gv-noi-dung', '/instructor/courses/1/curriculum', { maxHeight: 1250 });
  await shot(p, 'ui-22-gv-bai-hoc', '/instructor/lessons/1/edit', { maxHeight: 1000 });
  await shot(p, 'ui-23-gv-hoc-vien', '/instructor/courses/1/students', { maxHeight: 700 });
  await shot(p, 'ui-24-gv-tin-nhan', '/instructor/chats', { maxHeight: 700 });
  await shot(p, 'ui-25-gv-chat', '/instructor/chats/1/4', { maxHeight: 980, wait: 2500 });

  // ===== Admin =====
  ctx = await cdp.newContext();
  p = await cdp.newPage(ctx, 1366, 860, 1);
  await login(p, 'admin@utelearn.vn', 'admin123');
  await shot(p, 'ui-26-admin-dashboard', '/admin', { maxHeight: 1000 });
  await shot(p, 'ui-27-admin-nguoi-dung', '/admin/users', { maxHeight: 1000 });
  await shot(p, 'ui-28-admin-danh-muc', '/admin/categories', { maxHeight: 900 });
  await shot(p, 'ui-29-admin-duyet-khoa-hoc', '/admin/courses?status=PENDING', { maxHeight: 700 });
  await shot(p, 'ui-30-admin-xem-truoc', '/courses/7', { maxHeight: 900 });

  // ===== Mobile (responsive) =====
  ctx = await cdp.newContext();
  p = await cdp.newPage(ctx, 390, 844, 2);
  await shot(p, 'ui-31-mobile-trang-chu', '/', { maxHeight: 844 });

  await cdp.close();
})().catch(e => { console.error(e); process.exit(1); });
