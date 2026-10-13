package com.greenforce.repository;

import com.greenforce.entity.IssueUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueUpdateRepository extends JpaRepository<IssueUpdate, Long> {

    List<IssueUpdate> findByIssueIdOrderByChangedAtAscIdAsc(Long issueId);

    void deleteByIssueId(Long issueId);
}
