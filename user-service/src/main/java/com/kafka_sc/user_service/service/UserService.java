package com.kafka_sc.user_service.service;

import com.kafka_sc.user_service.dto.UserDto;
import com.kafka_sc.user_service.entity.User;
import com.kafka_sc.user_service.exception.EmailAlreadyExistsException;
import com.kafka_sc.user_service.exception.UserNotFoundException;
import com.kafka_sc.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserDto createUser(UserDto input) {
        if (userRepository.existsByEmail(input.getEmail())) {
            throw new EmailAlreadyExistsException(input.getEmail());
        }
        final User createdUser = User.builder().name(input.getName()).surname(input.getSurname()).email(input.getEmail()).address(input.getAddress()).alerting(input.isAlerting()).energyAlertingThreshold(input.getEnergyAlertingThreshold()).build();

        User saved = userRepository.save(createdUser);
        return toDto(saved);
    }

    public UserDto getUserById(Long id) {
        return userRepository.findById(id).map(this::toDto).orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        if (userRepository.existsByEmailAndIdNot(userDto.getEmail(), id)) {
            throw new EmailAlreadyExistsException(userDto.getEmail());
        }

        user.setName(userDto.getName());
        user.setSurname(userDto.getSurname());
        user.setEmail(userDto.getEmail());
        user.setAddress(userDto.getAddress());
        user.setAlerting(userDto.isAlerting());
        user.setEnergyAlertingThreshold(userDto.getEnergyAlertingThreshold());

        return toDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    private UserDto toDto(User user) {
        return UserDto.builder().id(user.getId()).name(user.getName()).surname(user.getSurname()).email(user.getEmail()).address(user.getAddress()).alerting(user.isAlerting()).energyAlertingThreshold(user.getEnergyAlertingThreshold()).build();
    }
}
