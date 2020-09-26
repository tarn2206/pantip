package tarn.pantip.app;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Tarn on 22 December 2016
 */

public class SplashActivity extends AppCompatActivity
{
    @Override
    protected void onResume()
    {
        super.onResume();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}