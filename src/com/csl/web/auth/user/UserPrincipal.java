package com.csl.web.auth.user;

import lombok.Getter;

import java.util.List;

@Getter
public final class UserPrincipal {
    private final String userName;
    private final List<Role> roles;

    private UserPrincipal(String userName, List<Role> roles) {
        this.userName = userName;
        this.roles = roles;
    }

    public static UserPrincipal of(String userName, List<Role> roles) {
        return new UserPrincipal(userName, roles);
    }
}
