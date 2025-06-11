package com.autoevaluator.adapter.handler.rest;

import com.autoevaluator.application.JwtService;
import com.autoevaluator.domain.entity.AppUser;
import com.autoevaluator.domain.models.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "http://localhost:3000")
public class UserController {
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    private JwtService jwtService;

    //    @PostMapping(value = "/login")
//    public String login(@RequestBody AppUser appUser) {
//        try {
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword())
//            );
//            if (authentication.isAuthenticated()) {
//
//                return jwtService.generateToken(appUser.getUsername());
//            } else {
//                return "Fail";
//            }
//        } catch (Exception e) {
//            return "Fail"; // or log the exception and return a more detailed response
//        }
//    }
    @PostMapping(value = "/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody AppUser appUser) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(appUser.getUsername(), appUser.getPassword())
            );
            if (authentication.isAuthenticated()) {
                String token = jwtService.generateToken(appUser.getUsername());

                // You must load the full AppUser here (ideally via your user service)
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                String email = userPrincipal.getUsername();

                String role = userPrincipal.getAuthorities().iterator().next().getAuthority();
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("role", role); // assuming getRole() returns String like "ADMIN"
                if (userPrincipal.getName() == null)
                    response.put("name", "user");
                else
                     response.put("name",userPrincipal.getName());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed"));
        }
    }

}

