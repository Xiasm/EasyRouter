package com.xsm.module1;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.xsm.easy.annotation.Extra;
import com.xsm.easy.annotation.Route;
import com.xsm.easy.core.EasyRouter;

@Route(path = "/module1/module1main")
public class Module1MainActivity extends AppCompatActivity {

    @Extra
    String msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module1_main);
        EasyRouter.getsInstance().inject(this);
        Toast.makeText(this, "msg=" + msg, Toast.LENGTH_SHORT).show();

    }
}
