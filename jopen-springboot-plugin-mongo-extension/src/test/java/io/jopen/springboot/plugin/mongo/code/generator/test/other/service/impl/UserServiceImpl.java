package io.jopen.springboot.plugin.mongo.code.generator.test.other.service.impl;

import io.jopen.springboot.plugin.mongo.code.generator.test.User;
import io.jopen.springboot.plugin.mongo.code.generator.test.other.repository.UserRepository;
import io.jopen.springboot.plugin.mongo.code.generator.test.other.service.UserService;
import io.jopen.springboot.plugin.mongo.repository.BaseServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author maxuefeng
 * @since 2020/02/09
 */
@Service
@Transactional
public class UserServiceImpl
        extends BaseServiceImpl<String, User, UserRepository>
        implements UserService{
}
