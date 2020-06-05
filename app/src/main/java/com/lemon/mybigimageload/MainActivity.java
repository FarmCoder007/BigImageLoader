package com.lemon.mybigimageload;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BigImageViewHv bigimage = findViewById(R.id.bigimage);
        AssetManager assetManager = getResources().getAssets();
        try {
            bigimage.setBigImage(assetManager.open("one.png"));
        } catch (IOException e) {
            Log.e("tag","-----------------e:"+e.getMessage()+"--:"+ e.toString());
            e.printStackTrace();
        }
    }
}
