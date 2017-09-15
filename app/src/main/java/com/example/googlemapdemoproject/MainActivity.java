package com.example.googlemapdemoproject;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.example.googlemapdemoproject.fragments.MapFagment;

public class MainActivity extends AppCompatActivity  {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /**
         * loading fragmen to container
         */
        FragmentManager fragmentManager=getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();

        MapFagment mapFagment=new MapFagment();
        fragmentTransaction.add(R.id.container, mapFagment);
        fragmentTransaction.commit();

    }


}
