package com.xsm.easyrouter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.xsm.easy.annotation.Route;
import com.xsm.easy.core.EasyRouter;

@Route(path = "/main/main")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void startModule1MainActivity(View view) {
        EasyRouter.getsInstance().build("/module1/module1main").navigation();
    }

    public void startModule2MainActivity(View view) {
        EasyRouter.getsInstance().build("/module2/module2main").navigation();
    }
}
