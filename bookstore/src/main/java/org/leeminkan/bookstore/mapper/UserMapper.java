// UserMapper.java
package org.leeminkan.bookstore.mapper;

import org.leeminkan.bookstore.dto.UserResponse;
import org.leeminkan.bookstore.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Converts the sensitive User Entity to the secure UserResponse DTO
    UserResponse toResponse(User user);
}