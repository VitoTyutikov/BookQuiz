package org.vito.server.auth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vito.server.auth.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

//    Optional<User> findById(Long id);

//    List<User> findAll();

    void deleteByEmail(String email);

    void deleteByUsername(String username);


}
