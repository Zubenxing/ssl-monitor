package com.sslmonitor.controller;

import com.sslmonitor.dto.AuthRequest;
import com.sslmonitor.dto.AuthResponse;
import com.sslmonitor.entity.User;
import com.sslmonitor.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        try {
            // 查找用户
            log.debug("Finding user in database: {}", request.getUsername());
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.debug("User found: {}", user.getUsername());
                
                // 简单密码验证 (实际应用中应使用加密)
                if (user.getPassword().equals(request.getPassword())) {
                    // 更新最后登录时间
                    user.setLastLogin(LocalDateTime.now());
                    userRepository.save(user);
                    
                    // 创建简单的JWT令牌 (实际应用中应使用真正的JWT)
                    String token = generateSimpleToken(user);
                    
                    log.info("User {} logged in successfully", user.getUsername());
                    return ResponseEntity.ok(new AuthResponse(token));
                } else {
                    log.warn("Invalid password for user: {}", user.getUsername());
                }
            } else {
                log.warn("User not found: {}", request.getUsername());
            }
            
            log.warn("Invalid login attempt for user: {}", request.getUsername());
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(response);
            
        } catch (Exception e) {
            log.error("Login error: ", e);
            Map<String, String> response = new HashMap<>();
            response.put("message", "登录过程中发生错误");
            return ResponseEntity.status(500).body(response);
        }
    }
    
    private String generateSimpleToken(User user) {
        // 简单令牌生成 (实际应用中应使用JWT库)
        return user.getId() + ":" + user.getUsername() + ":" + System.currentTimeMillis();
    }
} 