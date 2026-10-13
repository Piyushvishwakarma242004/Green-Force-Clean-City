package com.greenforce.repository;

import com.greenforce.entity.Issue;
import com.greenforce.entity.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    Optional<Issue> findByTrackingCode(String trackingCode);

    boolean existsByTrackingCode(String trackingCode);

    long countByStatus(IssueStatus status);

    Page<Issue> findByReporterEmail(String email, Pageable pageable);

    @Query("select i.category, count(i) from Issue i group by i.category order by count(i) desc")
    List<Object[]> countGroupedByCategory();
}
