package com.apsfc.loanmanagement.config;

import com.apsfc.loanmanagement.entity.Employee;
import com.apsfc.loanmanagement.entity.User;
import com.apsfc.loanmanagement.enums.Role;
import com.apsfc.loanmanagement.enums.UserStatus;
import com.apsfc.loanmanagement.repository.EmployeeRepository;
import com.apsfc.loanmanagement.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Creates the initial ADMIN / MANAGER / LOAN_OFFICER accounts on first boot.
 * Skipped if a user with the same email already exists, so it is idempotent.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BootstrapProperties properties;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(BootstrapProperties properties, UserRepository userRepository,
                           EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!properties.enabled()) {
            return;
        }
        createAdmin();
        createStaff("manager", properties.manager(), Role.MANAGER);
        createStaff("officer", properties.officer(), Role.LOAN_OFFICER);
    }

    private void createAdmin() {
        BootstrapProperties.AdminAccount admin = properties.admin();
        if (userRepository.existsByEmail(admin.email())) {
            return;
        }
        warnIfDefaultsUsed("admin", admin.password());
        userRepository.save(User.builder()
                .username(admin.username())
                .email(admin.email())
                .passwordHash(passwordEncoder.encode(admin.password()))
                .fullName(admin.fullName())
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        log.info("Bootstrapped ADMIN user: {}", admin.username());
    }

    private void createStaff(String label, BootstrapProperties.StaffAccount staff, Role role) {
        if (userRepository.existsByEmail(staff.email())) {
            return;
        }
        warnIfDefaultsUsed(label, staff.password());
        User user = userRepository.save(User.builder()
                .username(staff.username())
                .email(staff.email())
                .passwordHash(passwordEncoder.encode(staff.password()))
                .fullName(staff.fullName())
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());

        employeeRepository.save(Employee.builder()
                .user(user)
                .employeeCode(staff.employeeCode())
                .department(staff.department())
                .designation(staff.designation())
                .joiningDate(LocalDate.now())
                .status(UserStatus.ACTIVE)
                .build());
        log.info("Bootstrapped {} user with employee record: {}", role, staff.username());
    }

    private void warnIfDefaultsUsed(String account, String password) {
        if (password.equals("Admin@123") || password.equals("Manager@123") || password.equals("Officer@123")) {
            log.warn("Bootstrapped '{}' account is using a DEFAULT password - "
                    + "change it before any shared/remote deployment.", account);
        }
    }
}