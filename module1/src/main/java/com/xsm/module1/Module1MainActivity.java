package com.xsm.module1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.xsm.easy.annotation.Route;

@Route(path = "/module1/module1main")
public class Module1MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module1_main);
    }
}
