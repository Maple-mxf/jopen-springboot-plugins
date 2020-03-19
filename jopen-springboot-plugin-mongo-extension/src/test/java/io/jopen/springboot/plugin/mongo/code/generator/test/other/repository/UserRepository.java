package io.jopen.springboot.plugin.mongo.code.generator.test.other.repository;

import io.jopen.springboot.plugin.mongo.code.generator.test.User;
import io.jopen.springboot.plugin.mongo.repository.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * @author maxuefeng
 * @since 2020/02/09
 */
@Repository
public interface UserRepository extends BaseRepository<User, String> {
}
