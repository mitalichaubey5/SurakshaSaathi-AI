package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.dto.trusted.CreateTrustedContactRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedContactResponse;
import com.surakshasaathi.ai.dto.trusted.UpdateTrustedContactRequest;
import com.surakshasaathi.ai.model.TrustedContact;
import com.surakshasaathi.ai.model.User;
import com.surakshasaathi.ai.repository.TrustedContactRepository;
import com.surakshasaathi.ai.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TrustedContactServiceImplTest {
    @Mock private TrustedContactRepository repository;
    @Mock private UserRepository userRepository;
    @InjectMocks private TrustedContactServiceImpl service;

    private AutoCloseable mocks;
    private User currentUser;
    private User otherUser;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("test@example.com");
        currentUser.setName("Test");
        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        // set auth
        UserDetails ud = org.springframework.security.core.userdetails.User
                .withUsername("test@example.com")
                .password("pass")
                .authorities("ROLE_USER")
                .build();
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities()));
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        if (mocks != null) mocks.close();
    }

    @Test
    void createAssignsAuthenticatedUser() {
        CreateTrustedContactRequest req = new CreateTrustedContactRequest("Ravi", "9876543210", null, "Son", "en");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(repository.save(any(TrustedContact.class))).thenAnswer(i -> {
            TrustedContact t = i.getArgument(0);
            t.setId(10L);
            return t;
        });
        TrustedContactResponse resp = service.create(req);
        assertThat(resp.id()).isEqualTo(10L);
        ArgumentCaptor<TrustedContact> cap = ArgumentCaptor.forClass(TrustedContact.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getUser().getId()).isEqualTo(1L);
    }

    @Test
    void listQueriesOnlyCurrentUser() {
        TrustedContact t = new TrustedContact();
        t.setId(1L); t.setName("R"); t.setPhone("123"); t.setUser(currentUser);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(repository.findAllByUserId(1L)).thenReturn(List.of(t));
        List<TrustedContactResponse> list = service.listAll();
        assertThat(list).hasSize(1);
        verify(repository).findAllByUserId(1L);
    }

    @Test
    void getAnotherUsersContactFails() {
        TrustedContact other = new TrustedContact();
        other.setId(5L); other.setUser(otherUser);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAnotherUsersContactFails() {
        UpdateTrustedContactRequest req = new UpdateTrustedContactRequest("X", "123", null, null, "en", true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(5L, req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteAnotherUsersContactFails() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).deleteByIdAndUserId(any(), any());
    }

    @Test
    void missingAuthRejected() {
        SecurityContextHolder.clearContext();
        CreateTrustedContactRequest req = new CreateTrustedContactRequest("R", "1", null, null, "en");
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validOwnedContactWorks() {
        TrustedContact t = new TrustedContact();
        t.setId(1L); t.setName("Ravi"); t.setPhone("9876543210"); t.setUser(currentUser);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(repository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(t));
        TrustedContactResponse resp = service.getById(1L);
        assertThat(resp.name()).isEqualTo("Ravi");
    }

    @Test
    void createRequiresContactMethod() {
        CreateTrustedContactRequest req = new CreateTrustedContactRequest("R", null, null, null, "en");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
