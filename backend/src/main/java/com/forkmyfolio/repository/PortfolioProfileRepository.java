package com.forkmyfolio.repository;

import com.forkmyfolio.model.PortfolioProfile;
import com.forkmyfolio.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioProfileRepository extends JpaRepository<PortfolioProfile, Long> {

    /**
     * Finds a portfolio profile by user, eagerly fetching the associated user data.
     * Using an explicit JOIN FETCH is the most reliable way to solve
     * LazyInitializationExceptions.
     *
     * @param user The user to find the profile for.
     * @return An Optional containing the profile if found.
     */
    @Query("SELECT p FROM PortfolioProfile p JOIN FETCH p.user WHERE p.user = :user")
    Optional<PortfolioProfile> findByUser(@Param("user") User user);

    void deleteByUser(User user);

    /**
     * Finds a visible portfolio profile by user, eagerly fetching the associated
     * user data.
     *
     * @param user The user to find the profile for.
     * @return An Optional containing the visible profile if found.
     */
    @Query("SELECT p FROM PortfolioProfile p JOIN FETCH p.user WHERE p.user = :user AND p.publiclyVisible = true")
    Optional<PortfolioProfile> findByUserAndVisibleTrue(@Param("user") User user);

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<PortfolioProfile> findById(Long id);
}