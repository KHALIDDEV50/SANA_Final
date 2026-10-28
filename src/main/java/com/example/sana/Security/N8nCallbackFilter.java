package com.example.sana.Security;

import com.example.sana.Config.N8nProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class N8nCallbackFilter extends OncePerRequestFilter {
    public static final String PATH = "/api/integrations/n8n/ai/callback";
    private final N8nProperties properties;
    public N8nCallbackFilter(N8nProperties properties) { this.properties = properties; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PATH.equals(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String expected = properties.getCallbackKey(); String actual = request.getHeader("X-Sana-Callback-Key");
        if (expected == null || expected.isBlank()) {deny(response, 503, "Callback authentication is not configured"); return;}
        if (actual == null || actual.length() > 4096 || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            deny(response, 401, "Invalid callback credentials"); return;
        }
        int limit = properties.getMaxCallbackBytes();
        if (request.getContentLengthLong() > limit) {deny(response, 413, "Callback payload exceeds the byte limit"); return;}
        byte[] bytes = request.getInputStream().readNBytes(limit + 1);
        if (bytes.length > limit) {deny(response, 413, "Callback payload exceeds the byte limit"); return;}
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(bytes);
                return new ServletInputStream() {
                    public int read() {return input.read();}
                    public boolean isFinished() {return input.available() == 0;}
                    public boolean isReady() {return true;}
                    public void setReadListener(ReadListener listener) {throw new UnsupportedOperationException();}
                };
            }
            @Override public BufferedReader getReader() {return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));}
            @Override public int getContentLength() {return bytes.length;}
            @Override public long getContentLengthLong() {return bytes.length;}
        }, response);
    }

    private void deny(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
