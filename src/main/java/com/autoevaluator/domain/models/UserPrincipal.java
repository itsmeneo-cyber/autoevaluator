package com.autoevaluator.domain.models;

import com.autoevaluator.domain.entity.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private AppUser appUser;

    public UserPrincipal(AppUser user) {
        this.appUser = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        System.out.println(appUser.getRole());
        return Collections.singleton(new SimpleGrantedAuthority(appUser.getRole()));
    }

    @Override
    public String getPassword() {
        return appUser.getPassword();
    }

    @Override
    public String getUsername() {
        return appUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getName() {
        if (appUser instanceof Student student) {
            return student.getName();
        } else if (appUser instanceof Teacher teacher) {
            return teacher.getName();
        } else if (appUser instanceof Admin admin) {
            return admin.getName();
        } else if (appUser instanceof SuperAdmin superAdmin) {
            return superAdmin.getName();
        }
        return "";
    }

}