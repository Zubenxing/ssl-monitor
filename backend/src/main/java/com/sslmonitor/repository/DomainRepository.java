package com.sslmonitor.repository;

import com.sslmonitor.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    Optional<Domain> findByDomainName(String domainName);
    List<Domain> findByAutoRenewalTrue();
} 