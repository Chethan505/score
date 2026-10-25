package com.example.football.favorite;

import com.example.football.auth.User;
import com.example.football.match.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteMatchRepository extends JpaRepository<FavoriteMatch, UUID> {

    List<FavoriteMatch> findByUserId(UUID userId);

    Optional<FavoriteMatch> findByUserIdAndMatchId(UUID userId, UUID matchId);

    boolean existsByUserIdAndMatchId(UUID userId, UUID matchId);

    void deleteByUserIdAndMatchId(UUID userId, UUID matchId);
}