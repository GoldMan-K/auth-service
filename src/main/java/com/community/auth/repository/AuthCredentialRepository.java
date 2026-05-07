package com.community.auth.repository;

import com.community.auth.domain.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthCredentialRepository extends JpaRepository<AuthCredential, Long> {

    Optional<AuthCredential> findByUsername(String username);

    boolean existsByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
