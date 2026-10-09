package com.tradex.gateway;

import com.tradex.gateway.dto.AuthResponse;
import com.tradex.gateway.dto.RegisterRequest;
import com.tradex.gateway.entity.User;
import com.tradex.gateway.entity.Wallet;
import com.tradex.gateway.entity.WalletTransaction;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.repository.WalletRepository;
import com.tradex.gateway.repository.WalletTransactionRepository;
import com.tradex.gateway.security.JwtService;
import com.tradex.gateway.service.AuthService;
import com.tradex.gateway.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Phase2UserAndWalletTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository txRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testWalletDepositSuccess() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("10000.00"));
        wallet.setAvailableBalance(new BigDecimal("10000.00"));
        wallet.setReservedBalance(BigDecimal.ZERO);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        Wallet updated = walletService.deposit("alice", new BigDecimal("500.00"));

        assertNotNull(updated);
        assertEquals(new BigDecimal("10500.0000"), updated.getBalance());
        assertEquals(new BigDecimal("10500.0000"), updated.getAvailableBalance());
        verify(txRepository, times(1)).save(any(WalletTransaction.class));
    }

    @Test
    void testWalletWithdrawInsufficientFundsThrowsException() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setAvailableBalance(new BigDecimal("100.00"));

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(wallet));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            walletService.withdraw("bob", new BigDecimal("500.00"))
        );

        assertTrue(exception.getMessage().contains("Insufficient available balance"));
        verify(txRepository, never()).save(any());
    }

    @Test
    void testReserveAndReleaseFunds() {
        User user = new User();
        user.setId(3L);
        user.setUsername("charlie");

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setAvailableBalance(new BigDecimal("1000.00"));
        wallet.setReservedBalance(BigDecimal.ZERO);

        when(userRepository.findByUsername("charlie")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdForUpdate(3L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        // Reserve 300
        Wallet reserved = walletService.reserveFunds("charlie", new BigDecimal("300.00"));
        assertEquals(new BigDecimal("700.0000"), reserved.getAvailableBalance());
        assertEquals(new BigDecimal("300.0000"), reserved.getReservedBalance());

        // Release 100
        Wallet released = walletService.releaseFunds("charlie", new BigDecimal("100.00"));
        assertEquals(new BigDecimal("800.0000"), released.getAvailableBalance());
        assertEquals(new BigDecimal("200.0000"), released.getReservedBalance());
    }
}
