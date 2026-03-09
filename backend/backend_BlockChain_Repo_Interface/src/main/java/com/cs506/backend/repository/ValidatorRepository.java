package com.cs506.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cs506.backend.entity.Validator;

@Repository
public interface ValidatorRepository extends JpaRepository<Validator, Integer> {
    Optional<Validator> findByWalletAddress(String walletAddress);
    List<Validator> findByStatus(Validator.Status status);
}
