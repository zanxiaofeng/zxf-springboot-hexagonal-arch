package com.zxf.hexagonal.infrastructure.adapter.in.web.dto;

import com.zxf.hexagonal.domain.model.UserStatus;
import jakarta.validation.constraints.NotNull;

/** 用户状态流转 HTTP 请求。 */
public record ChangeUserStatusRequest(

        @NotNull(message = "Status is required")
        UserStatus status
) {
}
