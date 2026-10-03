package com.utelearn.controller.api;

import com.utelearn.dto.ApiDtos.CategoryInfo;
import com.utelearn.dto.ApiDtos.CourseDetail;
import com.utelearn.dto.ApiDtos.CourseSummary;
import com.utelearn.dto.ApiDtos.EnrollmentInfo;
import com.utelearn.dto.ApiDtos.LoginRequest;
import com.utelearn.dto.ApiDtos.LoginResponse;
import com.utelearn.dto.ApiDtos.PageResponse;
import com.utelearn.dto.ApiDtos.SectionInfo;
import com.utelearn.dto.ApiDtos.UserInfo;
import com.utelearn.entity.Course;
import com.utelearn.entity.User;
import com.utelearn.security.JwtUtil;
import com.utelearn.service.CategoryService;
import com.utelearn.service.CourseService;
import com.utelearn.service.EnrollmentService;
import com.utelearn.service.LearningService;
import com.utelearn.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

// REST API cho app mobile. Đăng nhập lấy token ở /api/auth/login rồi gửi kèm header "Authorization: Bearer <token>".
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final CourseService courseService;
    private final CategoryService categoryService;
    private final EnrollmentService enrollmentService;
    private final LearningService learningService;

    public ApiController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService,
                         CourseService courseService, CategoryService categoryService,
                         EnrollmentService enrollmentService, LearningService learningService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.courseService = courseService;
        this.categoryService = categoryService;
        this.enrollmentService = enrollmentService;
        this.learningService = learningService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // sai mật khẩu / tài khoản bị khóa -> ApiExceptionHandler trả 401 / 403
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userService.findByEmail(email);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, "Bearer", jwtUtil.getExpirationSeconds(), UserInfo.of(user));
    }

    @GetMapping("/me")
    public UserInfo me(Principal principal) {
        return UserInfo.of(userService.findByEmail(principal.getName()));
    }

    @GetMapping("/categories")
    public List<CategoryInfo> categories() {
        return categoryService.findAll().stream().map(CategoryInfo::of).toList();
    }

    @GetMapping("/courses")
    public PageResponse<CourseSummary> courses(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) String price,
                                               @RequestParam(defaultValue = "newest") String sort,
                                               @RequestParam(defaultValue = "0") int page) {
        return PageResponse.of(courseService.searchPublished(keyword, categoryId, price, sort, page), CourseSummary::of);
    }

    @GetMapping("/courses/{id}")
    public CourseDetail course(@PathVariable Long id, Principal principal) {
        User viewer = userService.getCurrentUser(principal);
        Course course = courseService.getCourseForDetail(id, viewer);
        List<SectionInfo> sections = course.getSections().stream()
                .map(s -> SectionInfo.of(s, lesson -> learningService.canViewLesson(viewer, course, lesson)))
                .toList();
        return new CourseDetail(CourseSummary.of(course), course.getDescription(), courseService.countStudents(course),
                enrollmentService.isEnrolled(viewer, course), sections);
    }

    @GetMapping("/my-courses")
    public List<EnrollmentInfo> myCourses(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        return enrollmentService.getMyCourses(user).stream().map(EnrollmentInfo::of).toList();
    }
}
