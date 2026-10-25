package com.example.football.competition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, UUID> {

    Optional<Competition> findByProviderCompetitionId(String providerCompetitionId);

    List<Competition> findByIsActiveTrue();

    boolean existsByProviderCompetitionId(String providerCompetitionId);
}