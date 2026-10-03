package com.utelearn.service;

import com.utelearn.dto.AssignmentForm;
import com.utelearn.entity.Assignment;
import com.utelearn.entity.Course;
import com.utelearn.entity.Lesson;
import com.utelearn.entity.Submission;
import com.utelearn.entity.User;
import com.utelearn.exception.BusinessException;
import com.utelearn.exception.NotFoundException;
import com.utelearn.repository.AssignmentRepository;
import com.utelearn.repository.SubmissionRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Bài tập: giảng viên giao bài, học viên nộp file, giảng viên chấm điểm
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final FileUploadService fileUploadService;
    private final NotificationService notificationService;

    public AssignmentService(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository,
                             CourseService courseService, EnrollmentService enrollmentService,
                             FileUploadService fileUploadService, NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.fileUploadService = fileUploadService;
        this.notificationService = notificationService;
    }

    public Assignment findById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài tập"));
    }

    public List<Assignment> findByCourse(Course course) {
        return assignmentRepository.findByCourseOrderByCreatedAtDesc(course);
    }

    public List<Assignment> findByLesson(Lesson lesson) {
        return assignmentRepository.findByLessonOrderByCreatedAtAsc(lesson);
    }

    public long countByCourse(Course course) {
        return assignmentRepository.countByCourse(course);
    }

    public long countUngraded(Course course) {
        return submissionRepository.countByAssignment_CourseAndScoreIsNull(course);
    }

    // số bài nộp của từng bài tập, key = id bài tập
    public Map<Long, Long> countSubmissions(List<Assignment> assignments) {
        Map<Long, Long> counts = new HashMap<>();
        for (Assignment a : assignments) {
            counts.put(a.getId(), submissionRepository.countByAssignment(a));
        }
        return counts;
    }

    // ================== PHÍA GIẢNG VIÊN ==================

    public Assignment getOwnedAssignment(Long id, User user) {
        Assignment assignment = findById(id);
        if (!courseService.canManage(assignment.getCourse(), user)) {
            throw new AccessDeniedException("Bạn không phải giảng viên của khóa học này");
        }
        return assignment;
    }

    @Transactional
    public Assignment create(Long courseId, AssignmentForm form, User user) {
        Course course = courseService.getOwnedCourse(courseId, user);
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        copyFormToAssignment(form, assignment);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Assignment update(Long id, AssignmentForm form, User user) {
        Assignment assignment = getOwnedAssignment(id, user);
        copyFormToAssignment(form, assignment);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Long delete(Long id, User user) {
        Assignment assignment = getOwnedAssignment(id, user);
        Long courseId = assignment.getCourse().getId();
        submissionRepository.deleteByAssignment(assignment);
        assignmentRepository.delete(assignment);
        return courseId;
    }

    public List<Submission> getSubmissions(Assignment assignment) {
        return submissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment);
    }

    public Submission getOwnedSubmission(Long submissionId, User user) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài nộp"));
        if (!courseService.canManage(submission.getAssignment().getCourse(), user)) {
            throw new AccessDeniedException("Bạn không phải giảng viên của khóa học này");
        }
        return submission;
    }

    @Transactional
    public Submission grade(Long submissionId, Double score, String feedback, User user) {
        Submission submission = getOwnedSubmission(submissionId, user);
        Assignment assignment = submission.getAssignment();
        if (score == null || score < 0 || score > assignment.getMaxScore()) {
            throw new BusinessException("Điểm phải từ 0 đến " + assignment.getMaxScore());
        }
        submission.setScore(score);
        submission.setFeedback(feedback);
        submission.setGradedAt(LocalDateTime.now());
        submissionRepository.save(submission);
        notificationService.notify(submission.getStudent(), "Bài tập đã được chấm",
                "Bài \"" + assignment.getTitle() + "\": " + score + "/" + assignment.getMaxScore() + " điểm.",
                "/learn/" + assignment.getCourse().getId() + "/assignments#a" + assignment.getId());
        return submission;
    }

    private void copyFormToAssignment(AssignmentForm form, Assignment assignment) {
        assignment.setTitle(form.getTitle().trim());
        assignment.setDescription(form.getDescription());
        assignment.setDueDate(form.getDueDate());
        assignment.setMaxScore(form.getMaxScore());
        assignment.setLesson(findLessonInCourse(assignment.getCourse(), form.getLessonId()));

        String fileUrl = fileUploadService.uploadDocument(form.getAttachmentFile(), "assignments");
        if (fileUrl != null) {
            assignment.setAttachmentUrl(fileUrl);
        }
    }

    // chỉ cho gắn với bài học thuộc chính khóa này
    private Lesson findLessonInCourse(Course course, Long lessonId) {
        if (lessonId == null) {
            return null;
        }
        for (Lesson l : course.getAllLessons()) {
            if (l.getId().equals(lessonId)) {
                return l;
            }
        }
        throw new BusinessException("Bài học không thuộc khóa học này");
    }

    // ================== PHÍA HỌC VIÊN ==================

    // bài nộp của học viên trong khóa, key = id bài tập
    public Map<Long, Submission> getMySubmissions(User student, Course course) {
        Map<Long, Submission> map = new HashMap<>();
        for (Submission s : submissionRepository.findByStudentAndAssignment_Course(student, course)) {
            map.put(s.getAssignment().getId(), s);
        }
        return map;
    }

    @Transactional
    public Submission submit(User student, Long assignmentId, MultipartFile file, String note) {
        Assignment assignment = findById(assignmentId);
        if (!enrollmentService.isEnrolled(student, assignment.getCourse())) {
            throw new BusinessException("Bạn cần đăng ký khóa học để nộp bài");
        }
        if (assignment.isOverdue()) {
            throw new BusinessException("Đã quá hạn nộp bài");
        }

        Submission submission = submissionRepository.findByAssignmentAndStudent(assignment, student).orElse(null);
        if (submission != null && submission.getScore() != null) {
            throw new BusinessException("Bài đã được chấm, không thể nộp lại");
        }

        String fileUrl = fileUploadService.uploadDocument(file, "submissions");
        if (submission == null) {
            if (fileUrl == null) {
                throw new BusinessException("Vui lòng chọn file bài làm");
            }
            submission = new Submission();
            submission.setAssignment(assignment);
            submission.setStudent(student);
        }
        // nộp lại mà không chọn file mới thì giữ file cũ, chỉ cập nhật ghi chú
        if (fileUrl != null) {
            submission.setFileUrl(fileUrl);
        }
        submission.setNote(note);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(submission);
        notificationService.notify(assignment.getCourse().getInstructor(), "Có bài nộp mới",
                student.getFullName() + " đã nộp bài \"" + assignment.getTitle() + "\".",
                "/instructor/assignments/" + assignment.getId() + "/submissions");
        return submission;
    }
}
