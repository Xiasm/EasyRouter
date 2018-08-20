package com.xsm.easyrouter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.xsm.base.providers.module1.Module1Providers;
import com.xsm.easy.annotation.Route;
import com.xsm.easy.core.EasyRouter;

@Route(path = "/main/main")
public class MainActivity extends AppCompatActivity {

    private Module1Providers module1Providers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initProviders();
    }

    private void initProviders() {
        module1Providers = (Module1Providers) EasyRouter.getsInstance().build("/module1/providers").navigation();
    }

    public void startModule1MainActivity(View view) {
//        EasyRouter.getsInstance().build("/module1/module1main").navigation();
        EasyRouter.getsInstance().build("/module1/module1main")
                .withString("msg", "从MainActivity").navigation();
    }

    public void startModule2MainActivity(View view) {
        EasyRouter.getsInstance().build("/module2/module2main").navigation();
    }

    public void add(View view) {
        int num = module1Providers.add(5, 6);
        Toast.makeText(this, "5+6=" + num, Toast.LENGTH_SHORT).show();
    }
}
