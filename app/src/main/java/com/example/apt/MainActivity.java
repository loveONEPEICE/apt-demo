package com.example.apt;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.annotation.BindView;
import com.example.butterknife_java.ButterKnife;

/**
 * @author lhl
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.main_tv)
    TextView textView;
/*    @BindView(R.id.bottom_tv)
    TextView bottomTv;*/

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        textView.postDelayed(() -> {
            textView.setText("Hello APT!");
            textView.setTextSize(25f);
//            bottomTv.setTextColor(Color.RED);
        },1500);
    }
}
