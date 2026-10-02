package com.utelearn.config;

import com.utelearn.entity.*;
import com.utelearn.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Tạo dữ liệu mẫu khi database còn trống (lần chạy đầu tiên).
// Nếu đã import database/utelearn.sql thì bảng users có dữ liệu -> bỏ qua.
@Component
public class DataSeeder implements CommandLineRunner {

    private static final String DOG_VIDEO = "https://res.cloudinary.com/demo/video/upload/dog.mp4";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                      CourseRepository courseRepository, EnrollmentRepository enrollmentRepository,
                      LessonProgressRepository lessonProgressRepository, OrderRepository orderRepository,
                      CartItemRepository cartItemRepository, ChatMessageRepository chatMessageRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // ===== Tài khoản =====
        User admin = createUser("Quản Trị Viên", "admin@utelearn.vn", "admin123", Role.ADMIN, null);
        User an = createUser("Nguyễn Văn An", "an.nguyen@utelearn.vn", "123456", Role.INSTRUCTOR,
                "Giảng viên Khoa CNTT - ĐH Công nghệ Kỹ thuật TP.HCM. 8 năm kinh nghiệm Java/Spring.");
        User binh = createUser("Trần Thị Bình", "binh.tran@utelearn.vn", "123456", Role.INSTRUCTOR,
                "Frontend & Mobile developer, từng làm việc tại các công ty product ở TP.HCM.");
        User cuong = createUser("Lê Minh Cường", "cuong.le@gmail.com", "123456", Role.STUDENT, null);
        User dung = createUser("Phạm Thùy Dung", "dung.pham@gmail.com", "123456", Role.STUDENT, null);
        User em = createUser("Võ Hoàng Em", "em.vo@gmail.com", "123456", Role.STUDENT, null);

        // ===== Danh mục =====
        Category web = categoryRepository.save(new Category("Lập trình Web", "HTML, CSS, JavaScript, Spring Boot, React...", "bi-globe2"));
        Category mobile = categoryRepository.save(new Category("Lập trình Mobile", "Android, iOS, Flutter, Kotlin", "bi-phone"));
        Category data = categoryRepository.save(new Category("Khoa học dữ liệu & AI", "Python, Machine Learning, phân tích dữ liệu", "bi-cpu"));
        Category db = categoryRepository.save(new Category("Cơ sở dữ liệu", "SQL, MySQL, thiết kế CSDL", "bi-database"));
        Category design = categoryRepository.save(new Category("Thiết kế UI/UX", "Figma, thiết kế giao diện, trải nghiệm người dùng", "bi-palette"));
        Category soft = categoryRepository.save(new Category("Kỹ năng mềm", "Thuyết trình, làm việc nhóm, quản lý thời gian", "bi-people"));

        // ===== Khóa học =====
        Course spring = createCourse("Spring Boot từ cơ bản đến nâng cao",
                "Xây dựng web app hoàn chỉnh với Spring Boot, JPA, Security và Thymeleaf.",
                599000, CourseLevel.INTERMEDIATE, CourseStatus.PUBLISHED, web, an);
        addSection(spring, "Chương 1: Làm quen Spring Boot",
                lesson("Giới thiệu khóa học và cài đặt môi trường", 12, "https://www.youtube.com/watch?v=9SGDpanrc8U", true),
                lesson("Tạo project đầu tiên với Spring Initializr", 18, DOG_VIDEO, false),
                lesson("Cấu trúc project và file application.properties", 15, DOG_VIDEO, false));
        addSection(spring, "Chương 2: Spring Data JPA",
                lesson("Entity, Repository và kết nối MySQL", 25, DOG_VIDEO, false),
                lesson("Quan hệ @OneToMany / @ManyToOne", 22, DOG_VIDEO, false),
                lesson("Phân trang và tìm kiếm với Pageable", 20, DOG_VIDEO, false));
        addSection(spring, "Chương 3: Spring Security + JWT",
                lesson("Cơ chế hoạt động của Spring Security", 20, DOG_VIDEO, false),
                lesson("Đăng nhập bằng JWT lưu trong cookie", 28, DOG_VIDEO, false));
        courseRepository.save(spring);

        Course html = createCourse("HTML, CSS & Bootstrap 5 cho người mới",
                "Tự tay dựng giao diện website responsive chỉ sau 2 tuần.",
                0, CourseLevel.BEGINNER, CourseStatus.PUBLISHED, web, an);
        addSection(html, "Chương 1: HTML căn bản",
                lesson("HTML là gì? Cấu trúc một trang web", 10, "https://www.youtube.com/watch?v=UB1O30fR-EE", true),
                lesson("Thẻ văn bản, liên kết, hình ảnh", 14, "https://www.youtube.com/watch?v=qz0aGYrrlhU", false));
        addSection(html, "Chương 2: CSS & Bootstrap",
                lesson("CSS selector, box model", 16, "https://www.youtube.com/watch?v=1Rs2ND1ryYc", false),
                lesson("Grid system của Bootstrap 5", 18, DOG_VIDEO, false));
        courseRepository.save(html);

        Course react = createCourse("ReactJS thực chiến",
                "Học React qua dự án thật: component, hooks, router, gọi API.",
                799000, CourseLevel.INTERMEDIATE, CourseStatus.PUBLISHED, web, binh);
        addSection(react, "Chương 1: Nền tảng React",
                lesson("Tổng quan React và Virtual DOM", 12, "https://www.youtube.com/watch?v=SqcY0GlETPk", true),
                lesson("JSX và Component", 20, "https://www.youtube.com/watch?v=bMknfKXIFA8", false));
        addSection(react, "Chương 2: Hooks",
                lesson("useState và useEffect", 25, DOG_VIDEO, false),
                lesson("Custom hook gọi API", 22, DOG_VIDEO, false));
        courseRepository.save(react);

        Course android = createCourse("Lập trình Android với Kotlin",
                "Viết ứng dụng Android đầu tiên bằng Kotlin và Android Studio.",
                699000, CourseLevel.BEGINNER, CourseStatus.PUBLISHED, mobile, binh);
        addSection(android, "Chương 1: Kotlin cơ bản",
                lesson("Biến, kiểu dữ liệu, hàm trong Kotlin", 18, "https://www.youtube.com/watch?v=F9UC9DY-vIU", true),
                lesson("Class, data class, null safety", 20, DOG_VIDEO, false));
        addSection(android, "Chương 2: Android căn bản",
                lesson("Activity và Layout", 22, "https://www.youtube.com/watch?v=fis26HvvDII", false),
                lesson("RecyclerView hiển thị danh sách", 25, DOG_VIDEO, false));
        courseRepository.save(android);

        Course python = createCourse("Python cho Khoa học dữ liệu",
                "Python, NumPy, Pandas và trực quan hóa dữ liệu cho người mới.",
                499000, CourseLevel.BEGINNER, CourseStatus.PUBLISHED, data, an);
        addSection(python, "Chương 1: Python căn bản",
                lesson("Cài đặt Python và Jupyter Notebook", 10, "https://www.youtube.com/watch?v=kqtD5dpn9C8", true),
                lesson("Kiểu dữ liệu, vòng lặp, hàm", 24, "https://www.youtube.com/watch?v=rfscVS0vtbw", false));
        addSection(python, "Chương 2: Pandas",
                lesson("DataFrame và đọc file CSV", 20, DOG_VIDEO, false),
                lesson("Lọc, nhóm và thống kê dữ liệu", 26, DOG_VIDEO, false));
        courseRepository.save(python);

        Course mysql = createCourse("MySQL & Thiết kế cơ sở dữ liệu",
                "Thiết kế ERD, chuẩn hóa dữ liệu và viết câu truy vấn SQL hiệu quả.",
                349000, CourseLevel.BEGINNER, CourseStatus.PUBLISHED, db, binh);
        addSection(mysql, "Chương 1: SQL căn bản",
                lesson("Giới thiệu CSDL quan hệ", 12, "https://www.youtube.com/watch?v=HXV3zeQKqGY", true),
                lesson("SELECT, WHERE, ORDER BY", 18, DOG_VIDEO, false),
                lesson("JOIN nhiều bảng", 22, DOG_VIDEO, false));
        courseRepository.save(mysql);

        // khóa đang chờ duyệt -> để demo chức năng duyệt của admin
        Course figma = createCourse("Figma UI/UX căn bản",
                "Thiết kế giao diện app/web chuyên nghiệp bằng Figma.",
                299000, CourseLevel.BEGINNER, CourseStatus.PENDING, design, binh);
        addSection(figma, "Chương 1: Làm quen Figma",
                lesson("Giao diện Figma và các công cụ cơ bản", 15, "https://www.youtube.com/watch?v=FTFaQWZBqQ8", true),
                lesson("Frame, Auto layout, Component", 20, DOG_VIDEO, false));
        courseRepository.save(figma);

        // khóa nháp -> giảng viên đang soạn
        Course speaking = createCourse("Kỹ năng thuyết trình trước đám đông",
                "Tự tin trình bày đồ án, bảo vệ luận văn và phỏng vấn xin việc.",
                0, CourseLevel.BEGINNER, CourseStatus.DRAFT, soft, an);
        addSection(speaking, "Chương 1: Chuẩn bị bài nói",
                lesson("Xác định mục tiêu và khán giả", 10, DOG_VIDEO, false));
        courseRepository.save(speaking);

        // ===== Đơn hàng + ghi danh =====
        createPaidOrder(cuong, spring);
        createPaidOrder(dung, react);
        Enrollment cuongSpring = enrollmentRepository.save(new Enrollment(cuong, spring));
        enrollmentRepository.save(new Enrollment(cuong, html)); // khóa miễn phí
        enrollmentRepository.save(new Enrollment(dung, react));

        // Cường đã học xong 2 bài đầu của khóa Spring Boot
        lessonProgressRepository.save(new LessonProgress(cuongSpring, spring.getAllLessons().get(0)));
        lessonProgressRepository.save(new LessonProgress(cuongSpring, spring.getAllLessons().get(1)));
        cuongSpring.setProgressPercent(Math.round(2 * 100f / spring.getTotalLessons()));
        enrollmentRepository.save(cuongSpring);

        // ===== Giỏ hàng =====
        cartItemRepository.save(new CartItem(cuong, python));
        cartItemRepository.save(new CartItem(em, spring));
        cartItemRepository.save(new CartItem(em, mysql));

        // ===== Chat =====
        sendChat(spring, cuong, cuong, "Thầy ơi, em chạy project bị lỗi kết nối MySQL ạ.");
        sendChat(spring, cuong, an, "Em kiểm tra lại username/password trong application.properties nhé.");
        sendChat(spring, cuong, cuong, "Dạ em chạy được rồi, cảm ơn thầy!");

        System.out.println(">>> Đã tạo dữ liệu mẫu. Admin: admin@utelearn.vn / admin123 (" + admin.getId() + ")");
    }

    private User createUser(String fullName, String email, String rawPassword, Role role, String bio) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setBio(bio);
        return userRepository.save(user);
    }

    private Course createCourse(String title, String shortDescription, long price, CourseLevel level,
                                CourseStatus status, Category category, User instructor) {
        Course course = new Course();
        course.setTitle(title);
        course.setShortDescription(shortDescription);
        course.setDescription(shortDescription + "\n\nBạn sẽ học được gì:\n"
                + "- Nắm vững kiến thức nền tảng và cách áp dụng vào dự án thực tế\n"
                + "- Thực hành qua bài tập sau mỗi chương\n"
                + "- Được giảng viên hỗ trợ trực tiếp qua chat\n\n"
                + "Yêu cầu: có máy tính kết nối Internet, tinh thần tự học.");
        course.setPrice(BigDecimal.valueOf(price));
        course.setLevel(level);
        course.setStatus(status);
        course.setCategory(category);
        course.setInstructor(instructor);
        return courseRepository.save(course);
    }

    private Lesson lesson(String title, int minutes, String videoUrl, boolean preview) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setDurationMinutes(minutes);
        lesson.setVideoUrl(videoUrl);
        lesson.setPreview(preview);
        lesson.setContent("Trong bài \"" + title + "\" chúng ta sẽ đi qua lý thuyết và làm ví dụ minh họa.\n\n"
                + "Ghi chú:\n- Xem hết video trước khi làm bài tập\n- Có thắc mắc hãy nhắn tin cho giảng viên");
        return lesson;
    }

    private void addSection(Course course, String title, Lesson... lessons) {
        Section section = new Section();
        section.setTitle(title);
        section.setCourse(course);
        section.setPosition(course.getSections().size() + 1);
        for (int i = 0; i < lessons.length; i++) {
            lessons[i].setSection(section);
            lessons[i].setPosition(i + 1);
            section.getLessons().add(lessons[i]);
        }
        course.getSections().add(section);
    }

    private void createPaidOrder(User user, Course course) {
        Order order = new Order();
        order.setOrderCode("UTE" + System.currentTimeMillis() + user.getId());
        order.setUser(user);
        order.setTotalAmount(course.getPrice());
        order.setFinalAmount(course.getPrice());
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(PaymentMethod.MOCK);
        order.setPaidAt(java.time.LocalDateTime.now());
        order.getItems().add(new OrderItem(order, course, course.getPrice()));
        orderRepository.save(order);
    }

    private void sendChat(Course course, User student, User sender, String content) {
        ChatMessage m = new ChatMessage();
        m.setCourse(course);
        m.setStudent(student);
        m.setSender(sender);
        m.setContent(content);
        chatMessageRepository.save(m);
    }
}
