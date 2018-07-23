package com.xsm.easyrouter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.xsm.easy.annotation.Route;

@Route(path = "/show/main2")
public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }
}
