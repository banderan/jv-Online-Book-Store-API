package org.example.jvspringbootfirstbook.dto.user.login;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record UserLoginRequestDto(
        @NotEmpty
        @Email
        String email,
        @NotEmpty
        @Size(min = 6, max = 20)
        String password) {
}
