package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.request.LoginRequest;
import com.apsfc.loanmanagement.dto.request.RegisterRequest;
import com.apsfc.loanmanagement.dto.response.AuthResponse;
import com.apsfc.loanmanagement.dto.response.UserProfileResponse;
import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.entity.Employee;
import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;
import com.apsfc.loanmanagement.exception.AuthenticationFailedException;
import com.apsfc.loanmanagement.exception.DuplicateResourceException;
import com.apsfc.loanmanagement.mapper.UserMapper;
import com.apsfc.loanmanagement.repository.CustomerRepository;
import com.apsfc.loanmanagement.repository.EmployeeRepository;
import com.apsfc.loanmanagement.repository.UserRepository;
import com.apsfc.loanmanagement.security.JwtService;
import com.apsfc.loanmanagement.security.SecurityUtil;
import com.apsfc.loanmanagement.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, CustomerRepository customerRepository,
                           EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken: " + request.username());
        }
        if (customerRepository.existsByAadhaarNumber(request.aadhaarNumber())) {
            throw new DuplicateResourceException("Aadhaar number is already registered");
        }
        if (customerRepository.existsByPanNumber(request.panNumber())) {
            throw new DuplicateResourceException("PAN number is already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        Customer customer = Customer.builder()
                .user(user)
                .aadhaarNumber(request.aadhaarNumber())
                .panNumber(request.panNumber().toUpperCase())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .employmentType(request.employmentType())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .state(request.state())
                .pincode(request.pincode())
                .kycVerified(false)
                .build();
        customerRepository.save(customer);

        log.info("New customer registered: username={}, customerId planned", user.getUsername());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailOrUsername(request.identifier(), request.identifier())
                .orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for identifier={}", request.identifier());
            throw new AuthenticationFailedException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException();
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse currentProfile() {
        User user = SecurityUtil.currentUser();
        Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
        return userMapper.toUserProfileResponse(user, customer, employee);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.expirationMs(),
                userMapper.toUserResponse(user));
    }
}