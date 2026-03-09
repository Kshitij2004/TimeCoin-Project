package com.cs506.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cs506.backend.entity.Listing;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Integer> {
    List<Listing> findBySellerId(Integer sellerId);
    List<Listing> findByStatus(Listing.Status status);
}
