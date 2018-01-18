package com.gds.gdson;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.gds.amodule.AModule;
import com.gds.amodule.AModuleGDsonModuleTypeAdapterFactory;
import com.gdson.GDsonTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Person person = new Person();
        person.name = "高旭";
        person.birthday = System.currentTimeMillis();
        person.age = 30;
        Book book = new Book();
        book.author = person;
        book.name = "xxx";
        Book.NestClass nestClass = new Book.NestClass();
        nestClass.name = "fadf";
        AModule aModule = new AModule();
        aModule.name = "amodule";
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new GDsonTypeAdapterFactory())
                .registerTypeAdapterFactory(new AModuleGDsonModuleTypeAdapterFactory())
                .create();
        String personStr = gson.toJson(person);
        String bookStr = gson.toJson(book);
        String nestClassStr = gson.toJson(nestClass);
        String amoduleStr = gson.toJson(aModule);

        TextView textView = findViewById(R.id.tv_text);
        textView.setText(personStr + "\n"
                + bookStr + "\n"
                + nestClassStr + "\n"
                + amoduleStr + "\n"
                + gson.fromJson(personStr, Person.class) + "\n"
                + gson.fromJson(bookStr, Book.class) + "\n"
                + gson.fromJson(nestClassStr, Book.NestClass.class) + "\n"
                + gson.fromJson(amoduleStr, AModule.class));
    }
}
