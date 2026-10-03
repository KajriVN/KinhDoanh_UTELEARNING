# Sinh database/utelearn.sql = DDL viết tay (khớp entity) + dữ liệu mẫu xuất từ H2 (do DataSeeder tạo)
import csv, glob, os, re, subprocess, sys

SP = os.path.dirname(os.path.abspath(__file__))
DUMP = os.path.join(SP, "dump")
PROJECT = r"E:\Document_3rd_grade\web_dev\Final_project\Kinh doanh khóa học trực tuyến và quản lý học tập số UTELearn"
H2JAR = glob.glob(os.path.expanduser(r"~\.m2\repository\com\h2database\h2\2.3.232\h2-2.3.232.jar"))[0]
URL = "jdbc:h2:file:" + DUMP.replace("\\", "/") + "/db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE"

DDL = """
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
"""

# bảng theo thứ tự tạo (để insert không lỗi khóa ngoại)
TABLES = re.findall(r"CREATE TABLE (\w+) \(", DDL)


def columns_of(table):
    body = re.search(r"CREATE TABLE " + table + r" \((.*?)\n\) ENGINE", DDL, re.S).group(1)
    cols = []
    for line in body.split("\n"):
        line = line.strip()
        m = re.match(r"(\w+)\s+([A-Z]+)", line)
        if m and m.group(1) not in ("PRIMARY", "CONSTRAINT"):
            cols.append((m.group(1), m.group(2)))
    return cols


def export_csv(table):
    out = os.path.join(DUMP, table + ".csv").replace("\\", "/")
    sql = f"CALL CSVWRITE('{out}', 'SELECT * FROM {table} ORDER BY id', 'charset=UTF-8 null=__NULL__')"
    subprocess.run(["java", "-cp", H2JAR, "org.h2.tools.Shell", "-url", URL, "-user", "sa", "-sql", sql],
                   check=True, capture_output=True)
    with open(out, encoding="utf-8") as f:
        return list(csv.DictReader(f))


def fmt(value, sqltype):
    if value == "__NULL__":
        return "NULL"
    if sqltype == "BIT":
        return "1" if value.upper() in ("TRUE", "1") else "0"
    if sqltype in ("BIGINT", "INT", "DECIMAL", "FLOAT"):
        return value
    value = value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "\\n")
    return "'" + value + "'"


inserts = []
for t in TABLES:
    rows = export_csv(t)
    if not rows:
        continue
    cols = columns_of(t)
    lower = {k.lower(): k for k in rows[0].keys()}
    inserts.append(f"\n-- {t} ({len(rows)} dòng)")
    inserts.append(f"INSERT INTO {t} ({', '.join(c for c, _ in cols)}) VALUES")
    vals = []
    for r in rows:
        vals.append("    (" + ", ".join(fmt(r[lower[c]], ty) for c, ty in cols) + ")")
    inserts.append(",\n".join(vals) + ";")

HEADER = """-- =================================================================
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
"""

with open(os.path.join(PROJECT, "database", "utelearn.sql"), "w", encoding="utf-8", newline="\n") as f:
    f.write(HEADER)
    f.write("\n-- =================================================================\n--  PHẦN 1: TẠO BẢNG\n-- =================================================================\n")
    f.write(DDL)
    f.write("\n-- =================================================================\n--  PHẦN 2: DỮ LIỆU MẪU\n-- =================================================================\n")
    f.write("\n".join(inserts) + "\n")

# bản dùng để test trên H2 (bỏ lệnh riêng của MySQL)
with open(os.path.join(DUMP, "utelearn-h2test.sql"), "w", encoding="utf-8") as f:
    body = DDL.replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4", "")
    f.write(body + "\n" + "\n".join(inserts).replace("\\n", "\n").replace("\\'", "''") + "\n")

print("tables:", TABLES)
print("insert blocks:", len([i for i in inserts if i.startswith("INSERT")]))
