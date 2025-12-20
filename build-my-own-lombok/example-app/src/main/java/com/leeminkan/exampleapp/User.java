package com.leeminkan.exampleapp;

import com.leeminkan.lombok.MyBuilder;

@MyBuilder
public class User {
    private int id;
    private String name;

    // You must provide this for our basic builder to work!
    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }
}