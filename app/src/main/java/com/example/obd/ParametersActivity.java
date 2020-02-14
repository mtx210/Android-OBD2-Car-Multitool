package com.example.obd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.util.ArrayList;

public class ParametersActivity extends Activity {

    private ArrayList<String> chosenParams = new ArrayList<>();
    private int chosenParamsAmount = 0;

    private CheckBox vin, speed, rpm, engineLoad, throttlePosition, coolantTemp, oilTemp, intakeTemp, fuelLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameters);

        Intent oncomingIntent = getIntent();
        chosenParams = (ArrayList<String>) oncomingIntent.getSerializableExtra("currentlySelectedParameters");
        chosenParamsAmount = oncomingIntent.getIntExtra("currentParametersAmount", 3);

        vin = findViewById(R.id.cbVin);
        speed = findViewById(R.id.cbSpeed);
        rpm = findViewById(R.id.cbRpm);
        engineLoad = findViewById(R.id.cbEngineLoad);
        throttlePosition = findViewById(R.id.cbThrottlePosition);
        coolantTemp = findViewById(R.id.cbCoolantTemp);
        oilTemp = findViewById(R.id.cbOilTemp);
        intakeTemp = findViewById(R.id.cbIntakeTemp);
        fuelLevel = findViewById(R.id.cbFuelLevel);

        for(String paramName : chosenParams){
            switch(paramName){
                case "Vehicle Identification Number (VIN)":
                    vin.setChecked(true);
                    break;
                case "Vehicle Speed":
                    speed.setChecked(true);
                    break;
                case "Engine RPM":
                    rpm.setChecked(true);
                    break;
                case "Engine Load":
                    engineLoad.setChecked(true);
                    break;
                case "Throttle Position":
                    throttlePosition.setChecked(true);
                    break;
                case "Engine Coolant Temperature":
                    coolantTemp.setChecked(true);
                    break;
                case "Engine oil temperature":
                    oilTemp.setChecked(true);
                    break;
                case "Intake Manifold Pressure":
                    intakeTemp.setChecked(true);
                    break;
                case "Fuel Level":
                    fuelLevel.setChecked(true);
                    break;
            }
        }

        vin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(vin.isChecked()){
                addParam("Vehicle Identification Number (VIN)", vin);
            } else {
                removeParam("Vehicle Identification Number (VIN)", vin);
            }
        });
        speed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(speed.isChecked()){
                addParam("Vehicle Speed", speed);
            } else {
                removeParam("Vehicle Speed", speed);
            }
        });
        rpm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(rpm.isChecked()){
                addParam("Engine RPM", rpm);
            } else {
                removeParam("Engine RPM", rpm);
            }
        });
        engineLoad.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(engineLoad.isChecked()){
                addParam("Engine Load", engineLoad);
            } else {
                removeParam("Engine Load", engineLoad);
            }
        });
        throttlePosition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(throttlePosition.isChecked()){
                addParam("Throttle Position", throttlePosition);
            } else {
                removeParam("Throttle Position", throttlePosition);
            }
        });
        coolantTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(coolantTemp.isChecked()){
                addParam("Engine Coolant Temperature", coolantTemp);
            } else {
                removeParam("Engine Coolant Temperature", coolantTemp);
            }
        });
        oilTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(oilTemp.isChecked()){
                addParam("Engine oil temperature", oilTemp);
            } else {
                removeParam("Engine oil temperature", oilTemp);
            }
        });
        intakeTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(intakeTemp.isChecked()){
                addParam("Intake Manifold Pressure", intakeTemp);
            } else {
                removeParam("Intake Manifold Pressure", intakeTemp);
            }
        });
        fuelLevel.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(fuelLevel.isChecked()){
                addParam("Fuel Level", fuelLevel);
            } else {
                removeParam("Fuel Level", fuelLevel);
            }
        });

        Button bSave = findViewById(R.id.bDone);
        bSave.setOnClickListener(e -> finishAction());
    }

    private void addParam(String paramName, CheckBox parentBox){
        if(chosenParamsAmount < 3){
            chosenParams.add(paramName);
            chosenParamsAmount++;
            parentBox.setChecked(true);
        } else {
            Toast.makeText(ParametersActivity.this, "Can't add more than 3 parameters", Toast.LENGTH_LONG).show();
            parentBox.setChecked(false);
        }
    }

    private void removeParam(String paramName, CheckBox parentBox){
        if(chosenParamsAmount>1){
            chosenParams.remove(paramName);
            chosenParamsAmount--;
            parentBox.setChecked(false);
        } else {
            Toast.makeText(ParametersActivity.this, "There must be at least one parameter to display", Toast.LENGTH_LONG).show();
            parentBox.setChecked(true);
        }
    }

    public void finishAction(){
        Intent output = new Intent();
        output.putExtra("parametersAmount", chosenParamsAmount);
        output.putExtra("parameters", chosenParams);

        setResult(RESULT_OK, output);
        finish();
    }
}