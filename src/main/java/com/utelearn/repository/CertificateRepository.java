package com.utelearn.repository;

import com.utelearn.entity.Certificate;
import com.utelearn.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByCode(String code);

    Optional<Certificate> findByEnrollment(Enrollment enrollment);
}
