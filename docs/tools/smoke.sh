#!/usr/bin/env bash
# Smoke test UTELearn bằng curl
B=${B:-http://localhost:8088}
D=$(mktemp -d)
PASS=0; FAIL=0

ok()   { echo "  OK   $1"; PASS=$((PASS+1)); }
bad()  { echo "  FAIL $1"; FAIL=$((FAIL+1)); }

# $1 jar, $2 path -> lấy _csrf từ trang
csrf() { curl -s -b "$1" -c "$1" "$B$2" | grep -o 'name="_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//'; }

# $1 jar $2 path -> status code
get()  { curl -s -o "$D/last.html" -w "%{http_code}" -b "$1" -c "$1" "$B$2"; }

# $1 jar $2 path $3.. extra data -> "code location"
post() { local jar=$1 path=$2; shift 2; local t; t=$(csrf "$jar" "${CSRF_PAGE:-/}");
         curl -s -o "$D/last.html" -w "%{http_code} %{redirect_url}" -b "$jar" -c "$jar" -X POST "$B$path" --data-urlencode "_csrf=$t" "$@"; }

login() { local jar=$1; local t; t=$(csrf "$jar" /auth/login);
          curl -s -o /dev/null -w "%{http_code} %{redirect_url}" -b "$jar" -c "$jar" -X POST "$B/auth/login" \
            --data-urlencode "_csrf=$t" --data-urlencode "email=$2" --data-urlencode "password=$3"; }

expect() { # $1 label $2 actual $3 expected-prefix
  if [[ "$2" == $3* ]]; then ok "$1 -> $2"; else bad "$1 -> got '$2' expected '$3'"; fi; }

echo "== Khách (chưa đăng nhập)"
A="$D/anon"
expect "GET /"            "$(get $A /)" 200
expect "GET /courses"     "$(get $A '/courses?keyword=spring')" 200
grep -q "Spring Boot từ cơ bản" "$D/last.html" && ok "tìm kiếm ra Spring Boot" || bad "tìm kiếm"
expect "GET /courses?price=free" "$(get $A '/courses?price=free&sort=price_desc')" 200
expect "GET /courses/1"   "$(get $A /courses/1)" 200
expect "GET /courses/8 (DRAFT)" "$(get $A /courses/8)" 404
expect "GET /my-courses -> login" "$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' $B/my-courses)" "302 $B/auth/login"
expect "login sai mật khẩu" "$(login $A cuong.le@gmail.com sai)" 200

echo "== Đăng ký"
R="$D/reg"; T=$(csrf $R /auth/register)
EM="test$RANDOM@gmail.com"
expect "POST /auth/register" "$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' -b $R -c $R -X POST $B/auth/register --data-urlencode _csrf=$T --data-urlencode 'fullName=Sinh Viên Test' --data-urlencode email=$EM --data-urlencode password=123456 --data-urlencode confirmPassword=123456)" "302 $B/auth/login"
expect "đăng ký trùng email" "$(curl -s -o $D/last.html -w '%{http_code}' -b $R -c $R -X POST $B/auth/register --data-urlencode _csrf=$T --data-urlencode 'fullName=A' --data-urlencode email=$EM --data-urlencode password=123456 --data-urlencode confirmPassword=123456)" 200
grep -q "đã được đăng ký" "$D/last.html" && ok "báo email trùng" || bad "báo email trùng"
expect "login user mới" "$(login $R $EM 123456)" "302 $B/"

echo "== Học viên Cường"
S="$D/stu"
expect "login" "$(login $S cuong.le@gmail.com 123456)" "302 $B/"
expect "GET /my-courses" "$(get $S /my-courses)" 200
grep -q "Spring Boot" "$D/last.html" && ok "có khóa Spring Boot" || bad "my-courses"
expect "GET /learn/1" "$(get $S /learn/1)" 200
expect "GET /learn/1?lessonId=3" "$(get $S '/learn/1?lessonId=3')" 200
CSRF_PAGE=/learn/1 expect "hoàn thành bài 3" "$(CSRF_PAGE=/learn/1 post $S /learn/1/lessons/3/complete)" "302 $B/learn/1?lessonId=4"
expect "GET /learn/3 (chưa mua)" "$(get $S /learn/3)" 302
expect "GET /learn/3?lessonId=(học thử)" "$(get $S "/learn/3?lessonId=$(curl -s $B/courses/3 | grep -o 'lessonId=[0-9]*' | head -1 | cut -d= -f2)")" 200
expect "GET /cart" "$(get $S /cart)" 200
expect "thêm khóa 4 vào giỏ" "$(post $S /cart/add/4)" "302 $B/courses/4"
expect "thêm khóa 1 (đã mua)" "$(post $S /cart/add/1)" "302 $B/courses/1"
expect "thêm khóa 2 (miễn phí)" "$(post $S /cart/add/2)" "302 $B/courses/2"
R1=$(post $S /checkout); expect "checkout" "$R1" "302 $B/payment/UTE"
CODE=$(echo $R1 | sed 's#.*/payment/##')
expect "GET payment page" "$(get $S /payment/$CODE)" 200
expect "xác nhận thanh toán" "$(post $S /payment/$CODE/confirm)" "302 $B/orders/$CODE"
expect "GET order detail" "$(get $S /orders/$CODE)" 200
grep -q "Thanh toán thành công" "$D/last.html" && ok "đơn PAID" || bad "đơn PAID"
expect "giỏ trống sau khi mua" "$(post $S /checkout)" "302 $B/cart"
expect "GET /learn/4 sau khi mua" "$(get $S /learn/4)" 200
expect "GET /orders" "$(get $S /orders)" 200
expect "GET /profile" "$(get $S /profile)" 200
expect "GET /learn/1/chat" "$(get $S /learn/1/chat)" 200
expect "học viên vào /admin" "$(get $S /admin)" 403
expect "học viên vào /instructor/courses" "$(get $S /instructor/courses)" 403
expect "GET /orders của người khác" "$(get $S /orders/KHONGTONTAI)" 404
CSRF_PAGE=/profile expect "đổi mật khẩu sai" "$(CSRF_PAGE=/profile post $S /profile/password --data-urlencode currentPassword=sai --data-urlencode newPassword=1234567 --data-urlencode confirmPassword=1234567)" "302 $B/profile"
T=$(csrf $S /profile)
expect "cập nhật hồ sơ + avatar" "$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' -b $S -c $S -X POST $B/profile -F _csrf=$T -F 'fullName=Lê Minh Cường' -F phone=0909123456 -F 'bio=Sinh viên năm 3' -F "avatar=@${AVATAR:-/dev/null};type=image/png;filename=a.png")" "302 $B/profile"
expect "logout" "$(post $S /auth/logout)" "302 $B/"
expect "sau logout vào /my-courses" "$(get $S /my-courses)" 302

echo "== Admin"
AD="$D/admin"
expect "login admin" "$(login $AD admin@utelearn.vn admin123)" "302 $B/admin"
expect "GET /admin" "$(get $AD /admin)" 200
expect "GET /admin/users" "$(get $AD '/admin/users?keyword=gmail&role=STUDENT')" 200
expect "GET /admin/categories" "$(get $AD /admin/categories)" 200
expect "GET /admin/categories?editId=1" "$(get $AD '/admin/categories?editId=1')" 200
expect "thêm danh mục" "$(post $AD /admin/categories/save --data-urlencode 'name=DevOps' --data-urlencode 'icon=bi-gear')" "302 $B/admin/categories"
expect "thêm danh mục trùng" "$(post $AD /admin/categories/save --data-urlencode 'name=devops')" "302 $B/admin/categories"
expect "xóa danh mục có khóa học" "$(post $AD /admin/categories/1/delete)" "302 $B/admin/categories"
expect "GET /admin/courses?status=PENDING" "$(get $AD '/admin/courses?status=PENDING')" 200
expect "từ chối khóa 7" "$(post $AD /admin/courses/7/reject --data-urlencode 'reason=Video chưa rõ')" "302"
expect "khóa user 6" "$(post $AD /admin/users/6/toggle)" "302 $B/admin/users"
expect "user 6 bị khóa không login được" "$(login "$D/em" em.vo@gmail.com 123456)" 200
expect "mở khóa user 6" "$(post $AD /admin/users/6/toggle)" "302 $B/admin/users"
expect "admin xem khóa DRAFT" "$(get $AD /courses/8)" 200

echo "== Giảng viên Bình"
GV="$D/gv"
expect "login GV" "$(login $GV binh.tran@utelearn.vn 123456)" "302 $B/instructor/courses"
expect "GET /instructor/courses" "$(get $GV /instructor/courses)" 200
grep -q "Video chưa rõ" "$D/last.html" && ok "thấy lý do bị từ chối" || bad "lý do từ chối"
expect "GET form tạo khóa" "$(get $GV /instructor/courses/new)" 200
T=$(csrf $GV /instructor/courses/new)
R2=$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' -b $GV -c $GV -X POST $B/instructor/courses/new -F _csrf=$T -F 'title=Docker cho người mới' -F 'shortDescription=Học Docker' -F 'description=abc' -F price=199000 -F level=BEGINNER -F categoryId=1 -F "thumbnail=@${AVATAR:-/dev/null};type=image/png;filename=t.png")
expect "tạo khóa học (multipart)" "$R2" "302 $B/instructor/courses/"
NEWID=$(echo $R2 | sed 's#.*/instructor/courses/##;s#/curriculum##')
expect "gửi duyệt khi chưa có bài" "$(post $GV /instructor/courses/$NEWID/submit)" "302"
expect "thêm chương" "$(post $GV /instructor/courses/$NEWID/sections --data-urlencode 'title=Chương 1: Cài Docker')" "302 $B/instructor/courses/$NEWID/curriculum"
expect "GET curriculum" "$(get $GV /instructor/courses/$NEWID/curriculum)" 200
SEC=$(grep -o '/instructor/sections/[0-9]*/lessons/new' "$D/last.html" | head -1 | grep -o '[0-9]*')
T=$(csrf $GV /instructor/sections/$SEC/lessons/new)
expect "thêm bài học" "$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' -b $GV -c $GV -X POST $B/instructor/sections/$SEC/lessons/new -F _csrf=$T -F 'title=Docker là gì' -F 'videoUrl=https://youtu.be/9SGDpanrc8U?t=10' -F durationMinutes=12 -F preview=true -F content=abc)" "302 $B/instructor/courses/$NEWID/curriculum"
expect "gửi duyệt" "$(post $GV /instructor/courses/$NEWID/submit)" "302 $B/instructor/courses"
expect "GET /instructor/courses/3/students" "$(get $GV /instructor/courses/3/students)" 200
expect "GV sửa khóa của GV khác" "$(get $GV /instructor/courses/1/edit)" 403
expect "GET /instructor/chats" "$(get $GV /instructor/chats)" 200
expect "GET sửa khóa 7" "$(get $GV /instructor/courses/7/edit)" 200

echo "== Admin duyệt khóa mới"
expect "duyệt khóa $NEWID" "$(post $AD /admin/courses/$NEWID/approve)" "302"
expect "khóa mới public" "$(get $A /courses/$NEWID)" 200
grep -q "youtube.com/embed/9SGDpanrc8U" <(curl -s -b $GV "$B/learn/$NEWID") && ok "link youtu.be -> embed" || bad "embed youtube"

echo; echo "PASS=$PASS FAIL=$FAIL"
rm -rf "$D"
