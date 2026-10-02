# Kinh doanh khóa học trực tuyến và quản lý học tập số UTELearn
Nền tảng tích hợp hỗ trợ kinh doanh khóa học trực tuyến và quản lý học tập số. Hệ thống cho phép người dùng tìm kiếm, đăng ký và thanh toán các khóa học linh hoạt; đồng thời cung cấp môi trường học tập số tiện ích với các tính năng theo dõi tiến độ, nộp bài tập và tương tác trực tiếp giữa học viên với giảng viên.

> Trạng thái: đã làm khoảng **50%** chức năng. Xem [hand-off.html](hand-off.html) để biết phần đã xong, phần còn lại và cách làm tiếp.

## Công nghệ
Spring Boot 3.5 · Thymeleaf · Bootstrap 5 · Spring Data JPA · MySQL 8 · Spring Security + JWT · WebSocket (STOMP) · Cloudinary

## Yêu cầu
- JDK 17 trở lên
- Maven 3.6+ (hoặc mở bằng IntelliJ IDEA)
- MySQL 8 (không bắt buộc nếu chạy profile `h2`)

## Cài đặt & chạy

### Cách 1: chạy nhanh với H2 (không cần MySQL)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```
Mở http://localhost:8080. Lần chạy đầu tự tạo dữ liệu mẫu. Xóa thư mục `data/` để reset.

### Cách 2: chạy với MySQL
1. Mở MySQL Workbench, chạy file `database/utelearn.sql` (tạo database `utelearn`, 17 bảng và dữ liệu mẫu).
2. Sửa tài khoản MySQL trong `src/main/resources/application.properties` (mặc định `root` / `123456`) hoặc đặt biến môi trường `DB_USERNAME`, `DB_PASSWORD`.
3. Chạy:
```bash
mvn spring-boot:run
```

Nếu cổng 8080 bị chiếm, thêm `--server.port=8088`:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8088
```

### Cloudinary (upload ảnh/video)
Đặt 3 biến môi trường `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` (lấy ở Dashboard của cloudinary.com).
Nếu không đặt, file upload được lưu vào thư mục `uploads/` để chạy demo.

## Tài khoản mẫu
| Email | Mật khẩu | Vai trò |
|---|---|---|
| admin@utelearn.vn | admin123 | Quản trị viên |
| an.nguyen@utelearn.vn | 123456 | Giảng viên |
| binh.tran@utelearn.vn | 123456 | Giảng viên |
| cuong.le@gmail.com | 123456 | Học viên |
| dung.pham@gmail.com | 123456 | Học viên |
| em.vo@gmail.com | 123456 | Học viên |

## Thư mục
```
database/utelearn.sql   script MySQL
docs/                   báo cáo .docx, sơ đồ, ảnh giao diện, datasheet prompt AI
hand-off.html           tài liệu bàn giao cho người làm tiếp
src/main/java/com/utelearn
  config/ security/ entity/ repository/ service/ controller/ dto/ exception/ util/
src/main/resources/templates   giao diện Thymeleaf
```
