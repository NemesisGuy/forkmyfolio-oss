package com.forkmyfolio.mapper;

import com.forkmyfolio.model.User;
import com.forkmyfolio.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserMapperReproTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveRoles_withGuestAuth_shouldReturnGuestRole() {
        User user = new User();
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        // Authorities are ROLE_GUEST
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUEST"))).when(auth).getAuthorities();

        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        // We can't access private resolveRoles easily, so we call toDto which uses it
        var dto = userMapper.toDto(user);

        assertEquals(1, dto.getRoles().size());
        assertTrue(dto.getRoles().contains(Role.GUEST));
    }

    @Test
    void resolveRoles_withNullAuth_shouldReturnUserRoles() {
        User user = new User();
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);

        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(sc);

        var dto = userMapper.toDto(user);

        assertEquals(1, dto.getRoles().size());
        assertTrue(dto.getRoles().contains(Role.USER));
    }

    @Test
    void resolveRoles_withNoAuthorities_shouldReturnUserRoles() {
        User user = new User();
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        user.setRoles(roles);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getAuthorities()).thenReturn(Collections.emptyList());

        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        var dto = userMapper.toDto(user);

        assertEquals(1, dto.getRoles().size());
        assertTrue(dto.getRoles().contains(Role.USER));
    }
}
