package com.spotme.domain.model.user;

import java.util.Objects;
import java.util.UUID;

public record UserId(
        UUID value)
 {
     public UserId {
         Objects.requireNonNull(value, "UserId cannot be null");
     }

     /** Generate a new random UserId. */
     public static UserId random() {
         return new UserId(UUID.randomUUID());
     }

     /** Parse from canonical UUID string. */
     public static UserId fromString(String s) {
         return new UserId(UUID.fromString(s));
     }

     @Override
     public String toString() {
         return value.toString();
     }
 }

