package com.cloudcostai.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Marker repository base interface for CloudCost AI domain repositories.
 * Concrete repositories will be added in Milestone 2.
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends Repository<T, ID> {
}
