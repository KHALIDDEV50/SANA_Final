package com.example.sana.Repository;

import com.example.sana.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Serializes subscription activation and project creation for one account.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    java.util.Optional<User> findBillingUserForUpdate(@org.springframework.data.repository.query.Param("id") Integer id);


    User findUserById(Integer id);

    User findUserByEmail(String email);
}