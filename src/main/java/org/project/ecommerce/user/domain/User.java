package org.project.ecommerce.user.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
public class User {

    private final Long id;

    @Builder
    public User(Long id) {
        this.id = id;
    }
}
