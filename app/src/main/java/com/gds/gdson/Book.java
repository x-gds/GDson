package com.gds.gdson;

import com.gdson.GDson;

@GDson
public class Book {
    public String name;
    public Person author;

    @GDson
    public static class NestClass {
        public String name;
    }
}
