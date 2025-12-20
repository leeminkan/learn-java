package com.leeminkan.exampleapp;

public class Main {
    public static void main(String[] args) {
        User user = new UserBuilder()
                .id(1)
                .name("Kan")
                .build();

        System.out.println("Created user: " + user);
    }
}
