package t_12.backend.api.staking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import t_12.backend.entity.Wallet;
import t_12.backend.service.StakingService;
import t_12.backend.service.WalletService;

@ExtendWith(MockitoExtension.class)
class StakingControllerTest {

    private static final Integer AUTH_USER_ID = 42;
    private static final String WALLET = "wlt_test_staking_controller";

    @Mock
    private StakingService stakingService;

    @Mock
    private WalletService walletService;

    @InjectMocks
    private StakingController stakingController;

    @BeforeEach
    void setAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(AUTH_USER_ID, null, List.of())
        );
    }

    @AfterEach
    void clearAuthContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stake_usesAuthenticatedUserWalletAndReturnsOverview() {
        Wallet wallet = new Wallet();
        wallet.setUserId(AUTH_USER_ID);
        wallet.setWalletAddress(WALLET);

        StakeRequest request = new StakeRequest();
        request.setAmount(new BigDecimal("10.00000000"));

        StakingOverviewResponse overview = new StakingOverviewResponse(
                WALLET,
                new BigDecimal("90.00000000"),
                new BigDecimal("10.00000000"),
                new BigDecimal("100.00000000"),
                List.of()
        );

        when(walletService.getWalletByUserId(AUTH_USER_ID)).thenReturn(wallet);
        when(stakingService.stake(WALLET, request.getAmount())).thenReturn(overview);

        ResponseEntity<StakingOverviewResponse> response = stakingController.stake(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(WALLET, response.getBody().getWalletAddress());
        verify(walletService).getWalletByUserId(AUTH_USER_ID);
        verify(stakingService).stake(WALLET, request.getAmount());
    }

    @Test
    void unstake_usesAuthenticatedUserWalletAndReturnsOverview() {
        Wallet wallet = new Wallet();
        wallet.setUserId(AUTH_USER_ID);
        wallet.setWalletAddress(WALLET);

        UnstakeRequest request = new UnstakeRequest();
        request.setAmount(new BigDecimal("5.00000000"));

        StakingOverviewResponse overview = new StakingOverviewResponse(
                WALLET,
                new BigDecimal("95.00000000"),
                new BigDecimal("5.00000000"),
                new BigDecimal("100.00000000"),
                List.of()
        );

        when(walletService.getWalletByUserId(AUTH_USER_ID)).thenReturn(wallet);
        when(stakingService.unstake(WALLET, request.getAmount())).thenReturn(overview);

        ResponseEntity<StakingOverviewResponse> response = stakingController.unstake(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("5.00000000"), response.getBody().getStaked());
        verify(walletService).getWalletByUserId(AUTH_USER_ID);
        verify(stakingService).unstake(WALLET, request.getAmount());
    }

    @Test
    void getStaking_returnsWalletOverview() {
        StakingOverviewResponse overview = new StakingOverviewResponse(
                WALLET,
                new BigDecimal("80.00000000"),
                new BigDecimal("20.00000000"),
                new BigDecimal("100.00000000"),
                List.of()
        );
        when(stakingService.getStakingOverview(WALLET)).thenReturn(overview);

        ResponseEntity<StakingOverviewResponse> response = stakingController.getStaking(WALLET);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("20.00000000"), response.getBody().getStaked());
        verify(stakingService).getStakingOverview(WALLET);
    }
}
