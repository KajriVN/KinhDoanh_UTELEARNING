-- =================================================================
--  UTELearn - Nền tảng kinh doanh khóa học trực tuyến & quản lý học tập số
--  Database: MySQL 8.x
--  Cách dùng: mở MySQL Workbench -> File -> Open SQL Script -> chọn file này -> Execute (⚡)
--
--  Tài khoản mẫu:
--    admin@utelearn.vn      / admin123  (ADMIN)
--    an.nguyen@utelearn.vn  / 123456    (INSTRUCTOR)
--    binh.tran@utelearn.vn  / 123456    (INSTRUCTOR)
--    cuong.le@gmail.com     / 123456    (STUDENT)
--    dung.pham@gmail.com    / 123456    (STUDENT)
--    em.vo@gmail.com        / 123456    (STUDENT)
--
--  Ghi chú: bảng/cột khớp với các entity JPA (Hibernate ddl-auto=update cũng tự tạo bảng nếu thiếu).
-- =================================================================

DROP DATABASE IF EXISTS utelearn;
CREATE DATABASE utelearn CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE utelearn;
SET NAMES utf8mb4;

-- =================================================================
--  PHẦN 1: TẠO BẢNG
-- =================================================================

-- ---------------------------------------------------------------
-- 1. Người dùng (học viên / giảng viên / admin)
-- ---------------------------------------------------------------
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    full_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,              -- BCrypt
    phone       VARCHAR(20),
    avatar_url  VARCHAR(500),
    bio         TEXT,
    role        ENUM('ADMIN','INSTRUCTOR','STUDENT') NOT NULL,
    enabled     BIT          NOT NULL,              -- 0 = bị khóa
    created_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 2. Danh mục khóa học
-- ---------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon        VARCHAR(50),                        -- class Bootstrap Icons
    PRIMARY KEY (id),
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 3. Khóa học
-- ---------------------------------------------------------------
CREATE TABLE courses (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    title             VARCHAR(200)  NOT NULL,
    short_description VARCHAR(500),
    description       TEXT,
    thumbnail_url     VARCHAR(500),
    price             DECIMAL(12,0) NOT NULL,       -- VND, 0 = miễn phí
    level             ENUM('ADVANCED','BEGINNER','INTERMEDIATE') NOT NULL,
    status            ENUM('DRAFT','PENDING','PUBLISHED','REJECTED') NOT NULL,
    reject_reason     VARCHAR(500),
    category_id       BIGINT,
    instructor_id     BIGINT        NOT NULL,
    created_at        DATETIME(6),
    updated_at        DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_courses_category   FOREIGN KEY (category_id)   REFERENCES categories (id),
    CONSTRAINT fk_courses_instructor FOREIGN KEY (instructor_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 4. Chương của khóa học
-- ---------------------------------------------------------------
CREATE TABLE sections (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    title     VARCHAR(200) NOT NULL,
    position  INT          NOT NULL,
    course_id BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sections_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 5. Bài học
-- ---------------------------------------------------------------
CREATE TABLE lessons (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    title            VARCHAR(200) NOT NULL,
    content          TEXT,
    video_url        VARCHAR(500),                  -- Youtube hoặc Cloudinary
    duration_minutes INT          NOT NULL,
    position         INT          NOT NULL,
    preview          BIT          NOT NULL,         -- 1 = cho học thử
    section_id       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lessons_section FOREIGN KEY (section_id) REFERENCES sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 6. Mã giảm giá
-- ---------------------------------------------------------------
CREATE TABLE coupons (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    code             VARCHAR(50) NOT NULL,
    discount_percent INT         NOT NULL,
    max_uses         INT         NOT NULL,          -- 0 = không giới hạn
    used_count       INT         NOT NULL,
    expired_at       DATETIME(6),
    active           BIT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 7. Đơn hàng + chi tiết đơn hàng
-- ---------------------------------------------------------------
CREATE TABLE orders (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    order_code      VARCHAR(30)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    total_amount    DECIMAL(12,0) NOT NULL,
    discount_amount DECIMAL(12,0) NOT NULL,
    final_amount    DECIMAL(12,0) NOT NULL,
    status          ENUM('CANCELLED','FAILED','PAID','PENDING') NOT NULL,
    payment_method  ENUM('MOCK','MOMO','VNPAY') NOT NULL,
    coupon_id       BIGINT,
    created_at      DATETIME(6),
    paid_at         DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_orders_code UNIQUE (order_code),
    CONSTRAINT fk_orders_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_orders_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_items (
    id        BIGINT        NOT NULL AUTO_INCREMENT,
    order_id  BIGINT        NOT NULL,
    course_id BIGINT        NOT NULL,
    price     DECIMAL(12,0) NOT NULL,               -- giá lúc mua
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order  FOREIGN KEY (order_id)  REFERENCES orders (id),
    CONSTRAINT fk_order_items_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 8. Ghi danh + tiến độ học
-- ---------------------------------------------------------------
CREATE TABLE enrollments (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    user_id          BIGINT NOT NULL,
    course_id        BIGINT NOT NULL,
    enrolled_at      DATETIME(6),
    progress_percent INT    NOT NULL,               -- 0..100
    completed_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_enrollments_user_course UNIQUE (user_id, course_id),
    CONSTRAINT fk_enrollments_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_enrollments_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lesson_progress (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT NOT NULL,
    lesson_id     BIGINT NOT NULL,
    completed_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_lesson_progress UNIQUE (enrollment_id, lesson_id),
    CONSTRAINT fk_lesson_progress_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id),
    CONSTRAINT fk_lesson_progress_lesson     FOREIGN KEY (lesson_id)     REFERENCES lessons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 9. Giỏ hàng
-- ---------------------------------------------------------------
CREATE TABLE cart_items (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    user_id   BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    added_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_user_course UNIQUE (user_id, course_id),
    CONSTRAINT fk_cart_items_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_cart_items_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 10. Chat học viên <-> giảng viên (1 cuộc chat = course_id + student_id)
-- ---------------------------------------------------------------
CREATE TABLE chat_messages (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    course_id  BIGINT        NOT NULL,
    student_id BIGINT        NOT NULL,
    sender_id  BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    sent_at    DATETIME(6),
    is_read    BIT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_course  FOREIGN KEY (course_id)  REFERENCES courses (id),
    CONSTRAINT fk_chat_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_chat_sender  FOREIGN KEY (sender_id)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 11. Bài tập + bài nộp
-- ---------------------------------------------------------------
CREATE TABLE assignments (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    course_id      BIGINT       NOT NULL,
    lesson_id      BIGINT,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    attachment_url VARCHAR(500),
    due_date       DATETIME(6),
    max_score      INT          NOT NULL,
    created_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_assignments_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_assignments_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE submissions (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT NOT NULL,
    student_id    BIGINT NOT NULL,
    file_url      VARCHAR(500),
    note          TEXT,
    submitted_at  DATETIME(6),
    score         FLOAT(53),                        -- NULL = chưa chấm
    feedback      TEXT,
    graded_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_submissions UNIQUE (assignment_id, student_id),
    CONSTRAINT fk_submissions_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id),
    CONSTRAINT fk_submissions_student    FOREIGN KEY (student_id)    REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 12. Đánh giá khóa học
-- ---------------------------------------------------------------
CREATE TABLE reviews (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    course_id  BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    rating     INT    NOT NULL,                     -- 1..5 sao
    comment    TEXT,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_reviews_course_user UNIQUE (course_id, user_id),
    CONSTRAINT fk_reviews_course FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT fk_reviews_user   FOREIGN KEY (user_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 13. Thông báo
-- ---------------------------------------------------------------
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    content    VARCHAR(500),
    link       VARCHAR(255),
    is_read    BIT          NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 14. Token quên mật khẩu
-- ---------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(100) NOT NULL,
    expired_at DATETIME(6)  NOT NULL,
    used       BIT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token UNIQUE (token),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 15. Khóa học yêu thích
-- ---------------------------------------------------------------
CREATE TABLE wishlists (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    course_id  BIGINT NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_wishlists UNIQUE (user_id, course_id),
    CONSTRAINT fk_wishlists_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_wishlists_course FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 16. Hỏi đáp dưới bài học (parent_id NULL = câu hỏi, có giá trị = câu trả lời)
-- ---------------------------------------------------------------
CREATE TABLE lesson_comments (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    lesson_id  BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    parent_id  BIGINT,
    content    TEXT   NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_lesson_comments_lesson FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT fk_lesson_comments_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_lesson_comments_parent FOREIGN KEY (parent_id) REFERENCES lesson_comments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 17. Chứng chỉ hoàn thành (file PDF được sinh lại từ dữ liệu này khi tải)
-- ---------------------------------------------------------------
CREATE TABLE certificates (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    enrollment_id BIGINT      NOT NULL,
    code          VARCHAR(30) NOT NULL,             -- mã tra cứu công khai
    issued_at     DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_certificates_enrollment UNIQUE (enrollment_id),
    CONSTRAINT uk_certificates_code UNIQUE (code),
    CONSTRAINT fk_certificates_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------
-- 18. Quiz trắc nghiệm cuối chương
-- ---------------------------------------------------------------
CREATE TABLE quizzes (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    section_id   BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    pass_percent INT          NOT NULL,             -- % câu đúng tối thiểu để đạt
    created_at   DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_quizzes_section UNIQUE (section_id),
    CONSTRAINT fk_quizzes_section FOREIGN KEY (section_id) REFERENCES sections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE quiz_questions (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    quiz_id        BIGINT       NOT NULL,
    content        TEXT         NOT NULL,
    option_a       VARCHAR(500) NOT NULL,
    option_b       VARCHAR(500) NOT NULL,
    option_c       VARCHAR(500) NOT NULL,
    option_d       VARCHAR(500) NOT NULL,
    correct_option INT          NOT NULL,           -- 1..4 = A..D
    position       INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_quiz_questions_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE quiz_attempts (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id  BIGINT NOT NULL,
    user_id  BIGINT NOT NULL,
    score    INT    NOT NULL,                       -- số câu đúng
    total    INT    NOT NULL,
    passed   BIT    NOT NULL,
    answers  VARCHAR(2000),                         -- "idCâuHỏi:đápÁn,..."
    taken_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id),
    CONSTRAINT fk_quiz_attempts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =================================================================
--  PHẦN 2: DỮ LIỆU MẪU
-- =================================================================

-- users (6 dòng)
INSERT INTO users (id, full_name, email, password, phone, avatar_url, bio, role, enabled, created_at) VALUES
    (1, 'Quản Trị Viên', 'admin@utelearn.vn', '$2a$10$CxglGN/QPIVhhOAZeRQiMumHc6nz2MAFLboxqha5Yh67WTTxcjwGe', NULL, NULL, NULL, 'ADMIN', 1, '2026-10-02 21:47:15.930466'),
    (2, 'Nguyễn Văn An', 'an.nguyen@utelearn.vn', '$2a$10$L/wPwaszRExIli0V9uOwQOIbfDun8/oQtWXlLhKHht7569pfPi0C2', NULL, NULL, 'Giảng viên Khoa CNTT - ĐH Công nghệ Kỹ thuật TP.HCM. 8 năm kinh nghiệm Java/Spring.', 'INSTRUCTOR', 1, '2026-10-02 21:47:16.151989'),
    (3, 'Trần Thị Bình', 'binh.tran@utelearn.vn', '$2a$10$rtibxr64L88svBqy.zFXoer6qJrKp.S6wMaEKj8eQRF/zkHV7mzTG', NULL, NULL, 'Frontend & Mobile developer, từng làm việc tại các công ty product ở TP.HCM.', 'INSTRUCTOR', 1, '2026-10-02 21:47:16.302988'),
    (4, 'Lê Minh Cường', 'cuong.le@gmail.com', '$2a$10$z2RQ2QPgT6KktX/rjd9GYeDIbWJPj6t37kZrxARS.E3DXbn87cFyS', NULL, NULL, NULL, 'STUDENT', 1, '2026-10-02 21:47:16.456987'),
    (5, 'Phạm Thùy Dung', 'dung.pham@gmail.com', '$2a$10$/kcu7t/gXnlrj5Gr.NAYDeJw4H7zxtfMLU1yo5KzuY9lblG9rwJTC', NULL, NULL, NULL, 'STUDENT', 1, '2026-10-02 21:47:16.634987'),
    (6, 'Võ Hoàng Em', 'em.vo@gmail.com', '$2a$10$TKLiNHTJm6AYFdqaN5OKlO46CxrsZlG5xOi7hDDXMuOhz6HG3QUSK', NULL, NULL, NULL, 'STUDENT', 1, '2026-10-02 21:47:16.811989');

-- categories (6 dòng)
INSERT INTO categories (id, name, description, icon) VALUES
    (1, 'Lập trình Web', 'HTML, CSS, JavaScript, Spring Boot, React...', 'bi-globe2'),
    (2, 'Lập trình Mobile', 'Android, iOS, Flutter, Kotlin', 'bi-phone'),
    (3, 'Khoa học dữ liệu & AI', 'Python, Machine Learning, phân tích dữ liệu', 'bi-cpu'),
    (4, 'Cơ sở dữ liệu', 'SQL, MySQL, thiết kế CSDL', 'bi-database'),
    (5, 'Thiết kế UI/UX', 'Figma, thiết kế giao diện, trải nghiệm người dùng', 'bi-palette'),
    (6, 'Kỹ năng mềm', 'Thuyết trình, làm việc nhóm, quản lý thời gian', 'bi-people');

-- courses (8 dòng)
INSERT INTO courses (id, title, short_description, description, thumbnail_url, price, level, status, reject_reason, category_id, instructor_id, created_at, updated_at) VALUES
    (1, 'Spring Boot từ cơ bản đến nâng cao', 'Xây dựng web app hoàn chỉnh với Spring Boot, JPA, Security và Thymeleaf.', 'Xây dựng web app hoàn chỉnh với Spring Boot, JPA, Security và Thymeleaf.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 599000, 'INTERMEDIATE', 'PUBLISHED', NULL, 1, 2, '2026-10-02 21:47:16.857987', '2026-10-02 21:47:16.857987'),
    (2, 'HTML, CSS & Bootstrap 5 cho người mới', 'Tự tay dựng giao diện website responsive chỉ sau 2 tuần.', 'Tự tay dựng giao diện website responsive chỉ sau 2 tuần.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 0, 'BEGINNER', 'PUBLISHED', NULL, 1, 2, '2026-10-02 21:47:16.980797', '2026-10-02 21:47:16.980797'),
    (3, 'ReactJS thực chiến', 'Học React qua dự án thật: component, hooks, router, gọi API.', 'Học React qua dự án thật: component, hooks, router, gọi API.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 799000, 'INTERMEDIATE', 'PUBLISHED', NULL, 1, 3, '2026-10-02 21:47:17.010826', '2026-10-02 21:47:17.010826'),
    (4, 'Lập trình Android với Kotlin', 'Viết ứng dụng Android đầu tiên bằng Kotlin và Android Studio.', 'Viết ứng dụng Android đầu tiên bằng Kotlin và Android Studio.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 699000, 'BEGINNER', 'PUBLISHED', NULL, 2, 3, '2026-10-02 21:47:17.0528', '2026-10-02 21:47:17.0528'),
    (5, 'Python cho Khoa học dữ liệu', 'Python, NumPy, Pandas và trực quan hóa dữ liệu cho người mới.', 'Python, NumPy, Pandas và trực quan hóa dữ liệu cho người mới.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 499000, 'BEGINNER', 'PUBLISHED', NULL, 3, 2, '2026-10-02 21:47:17.139818', '2026-10-02 21:47:17.139818'),
    (6, 'MySQL & Thiết kế cơ sở dữ liệu', 'Thiết kế ERD, chuẩn hóa dữ liệu và viết câu truy vấn SQL hiệu quả.', 'Thiết kế ERD, chuẩn hóa dữ liệu và viết câu truy vấn SQL hiệu quả.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 349000, 'BEGINNER', 'PUBLISHED', NULL, 4, 3, '2026-10-02 21:47:17.1808', '2026-10-02 21:47:17.1808'),
    (7, 'Figma UI/UX căn bản', 'Thiết kế giao diện app/web chuyên nghiệp bằng Figma.', 'Thiết kế giao diện app/web chuyên nghiệp bằng Figma.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 299000, 'BEGINNER', 'PENDING', NULL, 5, 3, '2026-10-02 21:47:17.1928', '2026-10-02 21:47:17.1928'),
    (8, 'Kỹ năng thuyết trình trước đám đông', 'Tự tin trình bày đồ án, bảo vệ luận văn và phỏng vấn xin việc.', 'Tự tin trình bày đồ án, bảo vệ luận văn và phỏng vấn xin việc.\n\nBạn sẽ học được gì:\n- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n- Thực hành qua bài tập sau mỗi chương\n- Được giảng viên hỗ trợ trực tiếp qua chat\n\nYêu cầu: có máy tính kết nối Internet, tinh thần tự học.', NULL, 0, 'BEGINNER', 'DRAFT', NULL, 6, 2, '2026-10-02 21:47:17.211799', '2026-10-02 21:47:17.211799');

-- sections (14 dòng)
INSERT INTO sections (id, title, position, course_id) VALUES
    (1, 'Chương 1: Làm quen Spring Boot', 1, 1),
    (2, 'Chương 2: Spring Data JPA', 2, 1),
    (3, 'Chương 3: Spring Security + JWT', 3, 1),
    (4, 'Chương 1: HTML căn bản', 1, 2),
    (5, 'Chương 2: CSS & Bootstrap', 2, 2),
    (6, 'Chương 1: Nền tảng React', 1, 3),
    (7, 'Chương 2: Hooks', 2, 3),
    (8, 'Chương 1: Kotlin cơ bản', 1, 4),
    (9, 'Chương 2: Android căn bản', 2, 4),
    (10, 'Chương 1: Python căn bản', 1, 5),
    (11, 'Chương 2: Pandas', 2, 5),
    (12, 'Chương 1: SQL căn bản', 1, 6),
    (13, 'Chương 1: Làm quen Figma', 1, 7),
    (14, 'Chương 1: Chuẩn bị bài nói', 1, 8);

-- lessons (30 dòng)
INSERT INTO lessons (id, title, content, video_url, duration_minutes, position, preview, section_id) VALUES
    (1, 'Giới thiệu khóa học và cài đặt môi trường', 'Trong bài "Giới thiệu khóa học và cài đặt môi trường" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=9SGDpanrc8U', 12, 1, 1, 1),
    (2, 'Tạo project đầu tiên với Spring Initializr', 'Trong bài "Tạo project đầu tiên với Spring Initializr" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 18, 2, 0, 1),
    (3, 'Cấu trúc project và file application.properties', 'Trong bài "Cấu trúc project và file application.properties" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 15, 3, 0, 1),
    (4, 'Entity, Repository và kết nối MySQL', 'Trong bài "Entity, Repository và kết nối MySQL" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 25, 1, 0, 2),
    (5, 'Quan hệ @OneToMany / @ManyToOne', 'Trong bài "Quan hệ @OneToMany / @ManyToOne" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 22, 2, 0, 2),
    (6, 'Phân trang và tìm kiếm với Pageable', 'Trong bài "Phân trang và tìm kiếm với Pageable" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 20, 3, 0, 2),
    (7, 'Cơ chế hoạt động của Spring Security', 'Trong bài "Cơ chế hoạt động của Spring Security" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 20, 1, 0, 3),
    (8, 'Đăng nhập bằng JWT lưu trong cookie', 'Trong bài "Đăng nhập bằng JWT lưu trong cookie" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 28, 2, 0, 3),
    (9, 'HTML là gì? Cấu trúc một trang web', 'Trong bài "HTML là gì? Cấu trúc một trang web" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=UB1O30fR-EE', 10, 1, 1, 4),
    (10, 'Thẻ văn bản, liên kết, hình ảnh', 'Trong bài "Thẻ văn bản, liên kết, hình ảnh" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=qz0aGYrrlhU', 14, 2, 0, 4),
    (11, 'CSS selector, box model', 'Trong bài "CSS selector, box model" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=1Rs2ND1ryYc', 16, 1, 0, 5),
    (12, 'Grid system của Bootstrap 5', 'Trong bài "Grid system của Bootstrap 5" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 18, 2, 0, 5),
    (13, 'Tổng quan React và Virtual DOM', 'Trong bài "Tổng quan React và Virtual DOM" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=SqcY0GlETPk', 12, 1, 1, 6),
    (14, 'JSX và Component', 'Trong bài "JSX và Component" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=bMknfKXIFA8', 20, 2, 0, 6),
    (15, 'useState và useEffect', 'Trong bài "useState và useEffect" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 25, 1, 0, 7),
    (16, 'Custom hook gọi API', 'Trong bài "Custom hook gọi API" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 22, 2, 0, 7),
    (17, 'Biến, kiểu dữ liệu, hàm trong Kotlin', 'Trong bài "Biến, kiểu dữ liệu, hàm trong Kotlin" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=F9UC9DY-vIU', 18, 1, 1, 8),
    (18, 'Class, data class, null safety', 'Trong bài "Class, data class, null safety" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 20, 2, 0, 8),
    (19, 'Activity và Layout', 'Trong bài "Activity và Layout" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=fis26HvvDII', 22, 1, 0, 9),
    (20, 'RecyclerView hiển thị danh sách', 'Trong bài "RecyclerView hiển thị danh sách" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 25, 2, 0, 9),
    (21, 'Cài đặt Python và Jupyter Notebook', 'Trong bài "Cài đặt Python và Jupyter Notebook" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=kqtD5dpn9C8', 10, 1, 1, 10),
    (22, 'Kiểu dữ liệu, vòng lặp, hàm', 'Trong bài "Kiểu dữ liệu, vòng lặp, hàm" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=rfscVS0vtbw', 24, 2, 0, 10),
    (23, 'DataFrame và đọc file CSV', 'Trong bài "DataFrame và đọc file CSV" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 20, 1, 0, 11),
    (24, 'Lọc, nhóm và thống kê dữ liệu', 'Trong bài "Lọc, nhóm và thống kê dữ liệu" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 26, 2, 0, 11),
    (25, 'Giới thiệu CSDL quan hệ', 'Trong bài "Giới thiệu CSDL quan hệ" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=HXV3zeQKqGY', 12, 1, 1, 12),
    (26, 'SELECT, WHERE, ORDER BY', 'Trong bài "SELECT, WHERE, ORDER BY" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 18, 2, 0, 12),
    (27, 'JOIN nhiều bảng', 'Trong bài "JOIN nhiều bảng" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 22, 3, 0, 12),
    (28, 'Giao diện Figma và các công cụ cơ bản', 'Trong bài "Giao diện Figma và các công cụ cơ bản" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://www.youtube.com/watch?v=FTFaQWZBqQ8', 15, 1, 1, 13),
    (29, 'Frame, Auto layout, Component', 'Trong bài "Frame, Auto layout, Component" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 20, 2, 0, 13),
    (30, 'Xác định mục tiêu và khán giả', 'Trong bài "Xác định mục tiêu và khán giả" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\nGhi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên', 'https://res.cloudinary.com/demo/video/upload/dog.mp4', 10, 1, 0, 14);

-- orders (2 dòng)
INSERT INTO orders (id, order_code, user_id, total_amount, discount_amount, final_amount, status, payment_method, coupon_id, created_at, paid_at) VALUES
    (1, 'UTE17909524372284', 4, 599000, 0, 599000, 'PAID', 'MOCK', NULL, '2026-10-02 21:47:17.240814', '2026-10-02 21:47:17.238819'),
    (2, 'UTE17909524372665', 5, 799000, 0, 799000, 'PAID', 'MOCK', NULL, '2026-10-02 21:47:17.267802', '2026-10-02 21:47:17.266803');

-- order_items (2 dòng)
INSERT INTO order_items (id, order_id, course_id, price) VALUES
    (1, 1, 1, 599000),
    (2, 2, 3, 799000);

-- enrollments (3 dòng)
INSERT INTO enrollments (id, user_id, course_id, enrolled_at, progress_percent, completed_at) VALUES
    (1, 4, 1, '2026-10-02 21:47:17.276802', 25, NULL),
    (2, 4, 2, '2026-10-02 21:47:17.2808', 0, NULL),
    (3, 5, 3, '2026-10-02 21:47:17.28581', 0, NULL);

-- lesson_progress (2 dòng)
INSERT INTO lesson_progress (id, enrollment_id, lesson_id, completed_at) VALUES
    (1, 1, 1, '2026-10-02 21:47:17.290814'),
    (2, 1, 2, '2026-10-02 21:47:17.300818');

-- cart_items (3 dòng)
INSERT INTO cart_items (id, user_id, course_id, added_at) VALUES
    (1, 4, 5, '2026-10-02 21:47:17.3118'),
    (2, 6, 1, '2026-10-02 21:47:17.319808'),
    (3, 6, 6, '2026-10-02 21:47:17.321799');

-- chat_messages (3 dòng)
INSERT INTO chat_messages (id, course_id, student_id, sender_id, content, sent_at, is_read) VALUES
    (1, 1, 4, 4, 'Thầy ơi, em chạy project bị lỗi kết nối MySQL ạ.', '2026-10-02 21:47:17.326802', 0),
    (2, 1, 4, 2, 'Em kiểm tra lại username/password trong application.properties nhé.', '2026-10-02 21:47:17.3368', 0),
    (3, 1, 4, 4, 'Dạ em chạy được rồi, cảm ơn thầy!', '2026-10-02 21:47:17.338803', 0);
