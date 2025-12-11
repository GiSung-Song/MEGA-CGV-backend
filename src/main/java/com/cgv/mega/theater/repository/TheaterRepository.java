package com.cgv.mega.theater.repository;

import com.cgv.mega.theater.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

}
