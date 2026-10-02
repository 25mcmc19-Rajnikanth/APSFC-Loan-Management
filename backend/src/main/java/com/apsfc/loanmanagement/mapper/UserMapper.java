package com.apsfc.loanmanagement.mapper;

import com.apsfc.loanmanagement.dto.response.UserProfileResponse;
import com.apsfc.loanmanagement.dto.response.UserResponse;
import com.apsfc.loanmanagement.entity.Customer;
import com.apsfc.loanmanagement.entity.Employee;
import com.apsfc.loanmanagement.entity.User;
import org.springframework.stereotype.Component;

/**
 * Entity-to-DTO mapping for user-related responses.
 */
@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus());
    }

    public UserProfileResponse toUserProfileResponse(User user, Customer customer, Employee employee) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getStatus(),
                customer != null ? customer.getId() : null,
                employee != null ? employee.getId() : null);
    }
}