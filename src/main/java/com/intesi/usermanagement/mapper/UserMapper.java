package com.intesi.usermanagement.mapper;

import com.intesi.usermanagement.domain.model.User;
import com.intesi.usermanagement.dto.response.UserResponse;

public interface UserMapper {

    UserResponse toResponse(User user);
}
