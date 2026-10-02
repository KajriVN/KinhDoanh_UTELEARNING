// Render ảnh diễn giải source code: đoạn code + số chú thích + chú giải bên dưới
const fs = require('fs');
const path = require('path');
const { launch } = require('./cdp');

const PROJECT = 'E:/Document_3rd_grade/web_dev/Final_project/Kinh doanh khóa học trực tuyến và quản lý học tập số UTELearn';
const J = 'src/main/java/com/utelearn/';
const OUT = path.join(PROJECT, 'docs/images/code');
fs.mkdirSync(OUT, { recursive: true });

// mode "method": lấy từ dòng khớp `from` tới hết khối { } ; mode "range": từ `from` tới `to`
const SNIPPETS = [
  { id: 'code-01-security-config', file: J + 'config/SecurityConfig.java', from: /public SecurityFilterChain filterChain/, notes: [
    ['setCsrfRequestAttributeName', 'Nạp CSRF token ngay đầu request để cookie XSRF-TOKEN kịp gửi về trình duyệt'],
    ['CookieCsrfTokenRepository', 'Bật chống CSRF, token lưu trong cookie, Thymeleaf tự chèn input _csrf vào form'],
    ['STATELESS', 'Không dùng HttpSession vì đăng nhập bằng JWT'],
    ['"/admin/**"', 'Phân quyền theo URL: /admin cho ADMIN, /instructor cho INSTRUCTOR + ADMIN'],
    ['LoginUrlAuthenticationEntryPoint', 'Chưa đăng nhập -> chuyển về /auth/login; sai quyền -> trang 403'],
    ['addFilterBefore', 'Chèn JwtAuthFilter trước filter đăng nhập mặc định']] },
  { id: 'code-02-jwt-filter', file: J + 'security/JwtAuthFilter.java', from: /protected void doFilterInternal/, notes: [
    ['getToken(request)', 'Lấy JWT từ header Authorization hoặc cookie access_token'],
    ['jwtUtil.isValid(token)', 'Kiểm tra chữ ký HS256 và hạn dùng của token'],
    ['loadUserByUsername(email)', 'Đọc lại user từ DB để biết role mới nhất và trạng thái khóa'],
    ['userDetails.isEnabled()', 'Tài khoản bị admin khóa thì coi như chưa đăng nhập'],
    ['setAuthentication(auth)', 'Gắn user vào SecurityContext -> controller dùng được Principal']] },
  { id: 'code-03-auth-login', file: J + 'controller/AuthController.java', from: /public String login\(/, notes: [
    ['authenticationManager.authenticate', 'Spring Security so khớp mật khẩu BCrypt'],
    ['catch (DisabledException', 'Tài khoản bị khóa -> báo lỗi riêng'],
    ['jwtUtil.generateToken', 'Tạo JWT chứa email + role, hết hạn sau 1 ngày'],
    ['.httpOnly(true)', 'Cookie HttpOnly: JavaScript không đọc được token (chống XSS)'],
    ['.sameSite("Lax")', 'SameSite=Lax hạn chế gửi cookie từ trang web khác'],
    ['switch (user.getRole())', 'Chuyển trang theo vai trò sau khi đăng nhập']] },
  { id: 'code-04-register', file: J + 'service/UserService.java', from: /public User register\(/, notes: [
    ['toLowerCase()', 'Chuẩn hóa email về chữ thường'],
    ['getConfirmPassword()', 'Kiểm tra nhập lại mật khẩu'],
    ['existsByEmail', 'Email phải chưa được đăng ký'],
    ['passwordEncoder.encode', 'Mã hóa mật khẩu bằng BCrypt trước khi lưu'],
    ['Role.STUDENT', 'Tài khoản mới mặc định là học viên']] },
  { id: 'code-05-search-repository', file: J + 'repository/CourseRepository.java', mode: 'range', from: /tìm kiếm khóa học/, to: /Pageable pageable\);/, notes: [
    ['c.status = :status', 'Chỉ lấy khóa đã xuất bản'],
    [':keyword IS NULL', 'Tham số null thì bỏ qua điều kiện -> 1 câu query dùng cho mọi kiểu lọc'],
    [':minPrice IS NULL', 'Lọc miễn phí (maxPrice = 0) hoặc có phí (minPrice = 1)'],
    ['Pageable pageable', 'Spring Data tự thêm ORDER BY + LIMIT/OFFSET để phân trang']] },
  { id: 'code-06-search-service', file: J + 'service/CourseService.java', from: /public Page<Course> searchPublished/, notes: [
    ['isBlank()', 'Ô tìm kiếm rỗng thì coi như không lọc'],
    ['"free".equals(priceType)', 'Đổi lựa chọn học phí thành khoảng giá'],
    ['Sort.by("price").ascending()', 'Chọn kiểu sắp xếp'],
    ['PageRequest.of', '9 khóa học mỗi trang']] },
  { id: 'code-07-cart-add', file: J + 'service/CartService.java', from: /public void addToCart/, notes: [
    ['CourseStatus.PUBLISHED', 'Chỉ mua được khóa đã xuất bản'],
    ['course.isFree()', 'Khóa miễn phí không cần giỏ hàng'],
    ['getInstructor().getId().equals', 'Giảng viên không tự mua khóa của mình'],
    ['isEnrolled', 'Không mua lại khóa đã sở hữu'],
    ['cartItemRepository.save', 'Giỏ hàng lưu trong DB, đăng nhập máy khác vẫn còn']] },
  { id: 'code-08-checkout', file: J + 'service/OrderService.java', from: /public Order checkout\(/, notes: [
    ['cartItems.isEmpty()', 'Giỏ trống thì không cho thanh toán'],
    ['generateOrderCode()', 'Mã đơn dạng UTE + thời gian + số ngẫu nhiên'],
    ['continue;', 'Bỏ qua khóa đã mua ở tab khác'],
    ['new OrderItem(order, item.getCourse(), price)', 'Lưu lại giá tại thời điểm mua'],
    ['setFinalAmount', 'Chưa có giảm giá -> finalAmount = total (coupon là phần TODO)']] },
  { id: 'code-09-mark-paid', file: J + 'service/OrderService.java', from: /public void markPaid\(/, notes: [
    ['OrderStatus.PAID', 'Đổi trạng thái đơn sang ĐÃ THANH TOÁN'],
    ['enrollmentService.enroll', 'Ghi danh học viên vào từng khóa trong đơn'],
    ['cartItemRepository::delete', 'Xóa các khóa đã mua khỏi giỏ hàng']] },
  { id: 'code-10-learning-progress', file: J + 'service/LearningService.java', mode: 'range', from: /public Enrollment markLessonComplete/, to: /^    \}\s*$/, secondTo: true, notes: [
    ['getSection().getCourse().getId().equals', 'Bài học phải thuộc đúng khóa học'],
    ['enrollment == null', 'Chưa ghi danh thì không lưu tiến độ'],
    ['existsByEnrollmentAndLesson', 'Mỗi bài chỉ lưu 1 lần (bấm nhiều lần không bị trùng)'],
    ['Math.round(done * 100.0 / totalLessons)', 'Tiến độ % = số bài đã xong / tổng số bài'],
    ['setCompletedAt', 'Đủ 100% thì lưu thời điểm hoàn thành']] },
  { id: 'code-11-chat-controller', file: J + 'controller/ChatController.java', from: /public void sendMessage\(/, notes: [
    ['@MessageMapping("/chat.send")', 'Nhận tin client gửi lên /app/chat.send qua WebSocket'],
    ['principal.getName()', 'Principal lấy từ cookie JWT lúc bắt tay WebSocket'],
    ['chatService.sendMessage', 'Kiểm tra quyền + lưu tin nhắn vào DB'],
    ['convertAndSendToUser(saved.getStudent()', 'Đẩy tin về hàng đợi riêng của học viên'],
    ['getInstructor().getEmail()', '... và của giảng viên'],
    ['"/queue/errors"', 'Lỗi nghiệp vụ gửi riêng cho người gửi']] },
  { id: 'code-12-chat-js', file: 'src/main/resources/static/js/chat.js', lang: 'javascript', mode: 'range', from: /function connect\(\)/, to: /^    form.addEventListener/, trimLast: true, notes: [
    ["new SockJS('/ws')", 'Kết nối WebSocket (SockJS tự fallback nếu trình duyệt không hỗ trợ)'],
    ["subscribe('/user/queue/messages'", 'Nhận tin nhắn của riêng mình'],
    ['m.courseId === courseId', 'Chỉ hiện tin thuộc cuộc trò chuyện đang mở'],
    ['setTimeout(connect, 3000)', 'Mất kết nối thì tự kết nối lại']] },
  { id: 'code-13-upload', file: J + 'service/FileUploadService.java', mode: 'range', from: /private String upload\(MultipartFile/, to: /return \(String\) result.get\("secure_url"\);/, notes: [
    ['isCloudinaryEnabled()', 'Có cấu hình Cloudinary thì upload lên cloud'],
    ['saveToLocal(file, folder)', 'Chưa có key thì lưu vào thư mục uploads/ để demo'],
    ['new Cloudinary(', 'Khởi tạo client Cloudinary 1 lần'],
    ['"resource_type", "auto"', 'Cloudinary tự nhận ảnh hay video'],
    ['secure_url', 'Lấy link HTTPS để lưu vào DB']] },
  { id: 'code-14-course-submit', file: J + 'service/CourseService.java', mode: 'range', from: /public void submitForReview/, to: /^    public List<Course> findPendingCourses/, trimLast: true, skipFrom: /public void deleteCourse/, skipTo: /^    \/\/ ================== PHÍA ADMIN/, notes: [
    ['getOwnedCourse(id, user)', 'Chỉ giảng viên chủ khóa (hoặc admin) được thao tác'],
    ['getTotalLessons() == 0', 'Phải có ít nhất 1 bài học mới được gửi duyệt'],
    ['CourseStatus.PENDING', 'Chuyển sang chờ admin duyệt']] },
  { id: 'code-15-admin-approve', file: J + 'service/CourseService.java', mode: 'range', from: /public void approve\(/, to: /^    public long countByStatus/, trimLast: true, notes: [
    ['!= CourseStatus.PENDING', 'Chỉ duyệt/từ chối khóa đang chờ'],
    ['CourseStatus.PUBLISHED', 'Duyệt: khóa hiện cho học viên'],
    ['reason.isBlank()', 'Từ chối bắt buộc nhập lý do'],
    ['setRejectReason', 'Lưu lý do để giảng viên sửa']] },
  { id: 'code-16-curriculum', file: J + 'service/CurriculumService.java', from: /public Long deleteSection\(/, notes: [
    ['getOwnedSection', 'Kiểm tra quyền sở hữu chương'],
    ['deleteByLesson(lesson)', 'Xóa tiến độ học của các bài trước (tránh lỗi khóa ngoại)'],
    ['getSections().remove(section)', 'Gỡ khỏi danh sách để Hibernate xóa cascade bài học']] },
  { id: 'code-17-admin-user', file: J + 'service/UserService.java', mode: 'range', from: /public void toggleEnabled/, to: /^    public long countByRole/, trimLast: true, notes: [
    ['admin.getId()', 'Admin không tự khóa / tự đổi vai trò của mình'],
    ['!user.isEnabled()', 'Đảo trạng thái khóa / mở'],
    ['user.setRole(role)', 'Cấp quyền giảng viên hoặc admin']] },
  { id: 'code-18-profile', file: J + 'service/UserService.java', from: /public void updateProfile\(/, notes: [
    ['uploadImage(form.getAvatar(), "avatars")', 'Upload ảnh đại diện lên Cloudinary (thư mục avatars)'],
    ['avatarUrl != null', 'Không chọn ảnh mới thì giữ ảnh cũ']] },
  { id: 'code-19-course-card', file: 'src/main/resources/templates/fragments/layout.html', lang: 'xml', mode: 'range', from: /THẺ KHÓA HỌC/, to: /^<\/div>\s*$/, notes: [
    ['th:fragment="courseCard(c)"', 'Fragment dùng lại ở trang chủ, danh sách khóa học'],
    ['thumb(${c})', 'Ảnh bìa; chưa có ảnh thì hiện nền gradient + icon danh mục'],
    ['@fmt.price', 'Bean FormatUtil định dạng tiền: 599000 -> "599.000 đ", 0 -> "Miễn phí"']] },
  { id: 'code-20-category', file: J + 'service/CategoryService.java', from: /public void delete\(Long id\)/, notes: [
    ['countByCategory(category) > 0', 'Không cho xóa danh mục còn khóa học']] },
];

function extract(sn) {
  const lines = fs.readFileSync(path.join(PROJECT, sn.file), 'utf8').replace(/\r/g, '').split('\n');
  let start = lines.findIndex(l => sn.from.test(l));
  if (start < 0) throw new Error('not found ' + sn.id);
  const fromIdx = start;
  // lấy luôn annotation / comment ngay phía trên
  while (start > 0 && /^\s*(@|\/\/)/.test(lines[start - 1])) start--;
  let end;
  if (sn.mode === 'range') {
    end = start + 1;
    let hits = 0;
    while (end < lines.length) {
      if (sn.to.test(lines[end])) { hits++; if (!sn.secondTo || hits === 2) break; }
      end++;
    }
    if (sn.trimLast) end--;
  } else {
    let depth = 0, opened = false;
    for (end = fromIdx; end < lines.length; end++) {
      for (const ch of lines[end]) { if (ch === '{') { depth++; opened = true; } if (ch === '}') depth--; }
      if (opened && depth === 0) break;
    }
  }
  let out = [];
  for (let i = start; i <= end; i++) out.push({ no: i + 1, text: lines[i] });
  if (sn.skipFrom) {
    const a = out.findIndex(o => sn.skipFrom.test(o.text));
    const b = out.findIndex(o => sn.skipTo.test(o.text));
    if (a >= 0 && b > a) {
      let a2 = a; while (a2 > 0 && /^\s*(@|\/\/)/.test(out[a2 - 1].text)) a2--;
      out.splice(a2, b - a2, { no: null, text: '    // ... (lược bớt) ...' });
    }
  }
  while (out.length && out[out.length - 1].text.trim() === '') out.pop();
  const indent = Math.min(...out.filter(o => o.text.trim()).map(o => o.text.match(/^ */)[0].length));
  return out.map(o => ({ no: o.no, text: o.text.slice(indent) }));
}

const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

(async () => {
  const cdp = await launch();
  const ctx = await cdp.newContext();
  const meta = {};
  for (const sn of SNIPPETS) {
    const lines = extract(sn);
    const badges = {};
    sn.notes.forEach(([needle], idx) => {
      const ln = lines.findIndex(l => l.text.includes(needle));
      if (ln < 0) throw new Error(`note "${needle}" not found in ${sn.id}`);
      badges[ln] = (badges[ln] || []).concat(idx + 1);
    });
    const rows = lines.map((l, i) => `<tr><td class="ln">${l.no ?? ''}</td><td class="code"><code class="line" data-lang="${sn.lang || 'java'}">${esc(l.text) || ' '}</code>${(badges[i] || []).map(n => `<span class="badge">${n}</span>`).join('')}</td></tr>`).join('');
    const legend = sn.notes.map(([, note], i) => `<li><span class="badge">${i + 1}</span>${esc(note)}</li>`).join('');
    const html = `<!doctype html><html><head><meta charset="utf-8">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/java.min.js"></script>
<style>
body{margin:0;background:#fff;font-family:"Segoe UI",Arial,sans-serif}
#card{display:inline-block;min-width:760px;max-width:1080px;border:1px solid #d0d7de;border-radius:10px;overflow:hidden;margin:6px}
.head{background:#f6f8fa;border-bottom:1px solid #d0d7de;padding:8px 14px;font-size:13px;color:#57606a}
.head b{color:#24292f}
table{border-collapse:collapse;font-family:Consolas,"Cascadia Code",monospace;font-size:13.5px;line-height:1.55}
td.ln{color:#8c959f;text-align:right;padding:0 10px 0 14px;user-select:none;background:#fafbfc;border-right:1px solid #eaeef2;vertical-align:top}
td.code{padding:0 14px 0 12px;white-space:pre}
code.line{background:none;padding:0;font-family:inherit}
.badge{display:inline-flex;align-items:center;justify-content:center;width:20px;height:20px;border-radius:50%;background:#ea580c;color:#fff;font:700 11.5px "Segoe UI",Arial;margin-left:8px;vertical-align:1px}
ol{list-style:none;margin:0;padding:10px 16px 12px;border-top:1px solid #d0d7de;background:#fffaf5;font-size:13.5px;color:#24292f}
ol li{margin:4px 0;display:flex;gap:8px;align-items:flex-start}
ol .badge{margin:0;flex-shrink:0}
</style></head><body><div id="card">
<div class="head">📄 <b>${esc(sn.file.replace('src/main/java/com/utelearn/', ''))}</b></div>
<table>${rows}</table><ol>${legend}</ol></div>
<script>
document.querySelectorAll('code.line').forEach(c => {
  const lang = c.dataset.lang;
  c.innerHTML = hljs.highlight(c.textContent, { language: lang, ignoreIllegals: true }).value;
});
</script></body></html>`;
    const tmp = path.join(__dirname, 'tmp-code.html');
    fs.writeFileSync(tmp, html);
    const page = await cdp.newPage(ctx, 1120, 900, 1.6);
    await page.goto('file:///' + tmp.replace(/\\/g, '/'), 300);
    await page.shotElement(path.join(OUT, sn.id + '.png'), '#card', 2);
    meta[sn.id] = { file: sn.file, notes: sn.notes.map(n => n[1]) };
    console.log('OK', sn.id, lines.length, 'lines');
  }
  fs.writeFileSync(path.join(__dirname, 'code-meta.json'), JSON.stringify(meta, null, 2));
  await cdp.close();
})().catch(e => { console.error(e); process.exit(1); });
