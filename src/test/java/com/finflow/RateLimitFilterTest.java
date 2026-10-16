package com.finflow;

import com.finflow.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    @Test
    void loginIsLimitedPerIpAndPathWithoutConsumingBody() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 5, 12, 30, 5, 60);
        for (int attempt = 0; attempt < 2; attempt++) {
            MockHttpServletRequest request = login("10.0.0.1", "user" + attempt + "@example.test");
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, new MockHttpServletResponse(), chain);
            assertNotNull(chain.getRequest());
            assertTrue(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8).contains("@example.test"));
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();
        filter.doFilter(login("10.0.0.1", "rotated@example.test"), blocked, blockedChain);
        assertEquals(429, blocked.getStatus());
        assertNull(blockedChain.getRequest());

        MockHttpServletResponse otherIp = new MockHttpServletResponse();
        filter.doFilter(login("10.0.0.2", "user0@example.test"), otherIp, new MockFilterChain());
        assertEquals(200, otherIp.getStatus());

        MockHttpServletRequest register = login("10.0.0.1", "new@example.test");
        register.setRequestURI("/api/auth/register");
        MockHttpServletResponse registerResponse = new MockHttpServletResponse();
        filter.doFilter(register, registerResponse, new MockFilterChain());
        assertEquals(200, registerResponse.getStatus());
    }

    private MockHttpServletRequest login(String ip, String email) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(ip);
        request.setContentType("application/json");
        request.setContent(("{\"email\":\"" + email + "\",\"password\":\"x\"}").getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
