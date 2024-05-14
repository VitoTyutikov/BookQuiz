package org.vito.server.auth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vito.server.auth.entity.BlackListToken;

@Repository
public interface BlackListTokenRepository extends JpaRepository<BlackListToken, Long> {
    BlackListToken findByToken(String token);
}