package t_12.backend.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import t_12.backend.service.RateLimiterService;

class RateLimitFilterTest {

    private RateLimiterService rateLimiterService;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        // small capacities for testing
        rateLimiterService = new RateLimiterService(
                3,  // login
                2,  // register
                5,  // refresh
                3,  // tfa
                4,  // transaction
                5,  // coin-read
                5   // mine
        );
        filter = new RateLimitFilter(rateLimiterService);
    }

    @Test
    void loginPost_underLimit_passesThrough() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = loginRequest("1.1.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertEquals(200, response.getStatus());  // default ok
    }

    @Test
    void loginPost_overLimit_returns429WithRetryAfter() throws Exception {
        // exhaust the 3-request budget
        for (int i = 0; i < 3; i++) {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(loginRequest("5.5.5.5"), new MockHttpServletResponse(), chain);
            verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }

        // 4th request should be blocked
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest("5.5.5.5"), response, chain);

        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertEquals(429, response.getStatus());
        assertNotNull(response.getHeader("Retry-After"));
        assertTrue(Long.parseLong(response.getHeader("Retry-After")) > 0);
        assertTrue(response.getContentAsString().contains("Too Many Requests"));
    }

    @Test
    void unmatchedEndpoint_passesThroughUnthrottled() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/some/unrelated/path");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // hit it way more than any configured limit; should never throttle
        for (int i = 0; i < 1000; i++) {
            filter.doFilter(request, response, chain);
        }

        verify(chain, times(1000)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void differentIps_getIndependentBuckets() throws Exception {
        // exhaust for IP A
        for (int i = 0; i < 3; i++) {
            filter.doFilter(loginRequest("10.0.0.1"), new MockHttpServletResponse(), mock(FilterChain.class));
        }

        // IP A is now blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.1"), blockedResponse, mock(FilterChain.class));
        assertEquals(429, blockedResponse.getStatus());

        // IP B still has capacity
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse okResponse = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.2"), okResponse, chain);
        verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void coinGet_rateLimited() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        // coin-read capacity is 5
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/coin");
            req.setRemoteAddr("8.8.8.8");
            filter.doFilter(req, new MockHttpServletResponse(), chain);
        }

        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/coin");
        blocked.setRemoteAddr("8.8.8.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(blocked, response, mock(FilterChain.class));

        assertEquals(429, response.getStatus());
    }

    @Test
    void xForwardedFor_usedAsClientIp() throws Exception {
        // 5 requests from "real" IP through proxy
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = loginRequest("proxy.internal");
            req.addHeader("X-Forwarded-For", "203.0.113.5");
            filter.doFilter(req, new MockHttpServletResponse(), mock(FilterChain.class));
        }

        // 4th from same real IP blocked
        MockHttpServletRequest blocked = loginRequest("proxy.internal");
        blocked.addHeader("X-Forwarded-For", "203.0.113.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(blocked, response, mock(FilterChain.class));
        assertEquals(429, response.getStatus());

        // different real IP through same proxy still allowed
        MockHttpServletRequest differentUser = loginRequest("proxy.internal");
        differentUser.addHeader("X-Forwarded-For", "203.0.113.9");
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(differentUser, new MockHttpServletResponse(), chain);
        verify(chain, times(1)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(remoteAddr);
        return req;
    }
}