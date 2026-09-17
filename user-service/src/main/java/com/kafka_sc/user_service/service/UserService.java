package com.kafka_sc.user_service.service;

import com.kafka_sc.user_service.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService {

    public UserDto createUser(UserDto userDto) {
        log.info("Creating user: {}", userDto);
        return userDto;
    }
}
