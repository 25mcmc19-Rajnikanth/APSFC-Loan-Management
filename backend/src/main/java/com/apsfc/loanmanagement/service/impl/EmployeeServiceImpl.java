package com.apsfc.loanmanagement.service.impl;

import com.apsfc.loanmanagement.dto.request.CreateEmployeeRequest;
import com.apsfc.loanmanagement.dto.request.UpdateEmployeeRequest;
import com.apsfc.loanmanagement.dto.response.EmployeeResponse;
import com.apsfc.loanmanagement.entity.Employee;
import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;
import com.apsfc.loanmanagement.exception.BusinessViolationException;
import com.apsfc.loanmanagement.exception.DuplicateResourceException;
import com.apsfc.loanmanagement.exception.ResourceNotFoundException;
import com.apsfc.loanmanagement.mapper.EmployeeMapper;
import com.apsfc.loanmanagement.repository.EmployeeRepository;
import com.apsfc.loanmanagement.repository.EmployeeSpecifications;
import com.apsfc.loanmanagement.repository.UserRepository;
import com.apsfc.loanmanagement.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private static final Set<Role> ASSIGNABLE_ROLES = Set.of(Role.LOAN_OFFICER, Role.MANAGER);

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeMapper = employeeMapper;
    }

    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        if (!ASSIGNABLE_ROLES.contains(request.role())) {
            throw new BusinessViolationException(
                    "Only LOAN_OFFICER or MANAGER roles can be provisioned here");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken: " + request.username());
        }
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("Employee code already exists: " + request.employeeCode());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        Employee employee = Employee.builder()
                .user(user)
                .employeeCode(request.employeeCode())
                .department(request.department())
                .designation(request.designation())
                .joiningDate(request.joiningDate())
                .status(UserStatus.ACTIVE)
                .build();
        employee = employeeRepository.save(employee);

        log.info("Employee provisioned: code={}, role={}, user={}",
                request.employeeCode(), request.role(), user.getUsername());
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Employee employee = findEmployee(id);
        if (request.department() != null) {
            employee.setDepartment(request.department());
        }
        if (request.designation() != null) {
            employee.setDesignation(request.designation());
        }
        if (request.joiningDate() != null) {
            employee.setJoiningDate(request.joiningDate());
        }
        if (request.phone() != null) {
            employee.getUser().setPhone(request.phone());
            userRepository.save(employee.getUser());
        }
        employee = employeeRepository.save(employee);
        log.info("Employee {} updated", employee.getEmployeeCode());
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return employeeMapper.toResponse(findEmployee(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponse> list(Role role, UserStatus status, String search, Pageable pageable) {
        Specification<Employee> spec = Specification.where(EmployeeSpecifications.search(search));
        if (role != null) {
            spec = spec.and(EmployeeSpecifications.hasRole(role));
        }
        if (status != null) {
            spec = spec.and(EmployeeSpecifications.hasStatus(status));
        }
        return employeeRepository.findAll(spec, pageable).map(employeeMapper::toResponse);
    }

    @Override
    @Transactional
    public EmployeeResponse updateStatus(Long id, UserStatus status) {
        if (status == UserStatus.LOCKED) {
            throw new BusinessViolationException("LOCKED cannot be set via admin; use ACTIVE or INACTIVE");
        }
        Employee employee = findEmployee(id);
        employee.setStatus(status);
        employee.getUser().setStatus(status);
        employeeRepository.save(employee);
        userRepository.save(employee.getUser());
        log.info("Employee {} status set to {}", employee.getEmployeeCode(), status);
        return employeeMapper.toResponse(employee);
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }
}