package com.example.obd;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.LoadCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.engine.ThrottlePositionCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.pressure.IntakeManifoldPressureCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends Activity {

    final static int ENABLE_BT_REQUEST = 1;
    final static int CHOOSE_PARAMS_REQUEST = 2;

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket;
    private String chosenDeviceName, chosenDeviceAddress;
    private Button bConnect, bStart, bStop;
    private TextView command1Label, command2Label, command3Label;
    private TextView command1Result, command2Result, command3Result;

    private ObdCommand command1 = new RPMCommand();
    private ObdCommand command2 = new SpeedCommand();
    private ObdCommand command3 = new LoadCommand();

    private ArrayList<ObdCommand> chosenParameters = new ArrayList<ObdCommand>(){{ add(command1); add(command2); add(command3); }};
    private int chosenParametersAmount = 3;

    private Timer timer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        command1Label = findViewById(R.id.command1Label);
        command2Label = findViewById(R.id.command2Label);
        command3Label = findViewById(R.id.command3Label);

        command1Result = findViewById(R.id.command1Result);
        command2Result = findViewById(R.id.command2Result);
        command3Result = findViewById(R.id.command3Result);

        Button bChooseDevice = findViewById(R.id.bChooseDevice);
        bChooseDevice.setOnClickListener(e -> chooseBluetoothDevice());

        Button bChooseParams = findViewById(R.id.bChooseParams);
        bChooseParams.setOnClickListener(e -> {
            Intent chooseParametersIntent = new Intent(this, ParametersActivity.class);
            ArrayList<String> chosenParametersNames = new ArrayList<>();
            for(ObdCommand obdCommand : chosenParameters){
                chosenParametersNames.add(obdCommand.getName());
            }
            chooseParametersIntent.putExtra("currentlySelectedParameters", chosenParametersNames);
            chooseParametersIntent.putExtra("currentParametersAmount", chosenParametersAmount);
            startActivityForResult(chooseParametersIntent, CHOOSE_PARAMS_REQUEST);
        });

        bConnect = findViewById(R.id.bConnect);
        bConnect.setOnClickListener(e -> connectOBD());
        bConnect.setEnabled(false);

        bStart = findViewById(R.id.bStart);
        bStart.setOnClickListener(e -> startOBD());
        bStart.setEnabled(false);

        bStop = findViewById(R.id.bStop);
        bStop.setOnClickListener(e -> stopOBD());
        bStop.setEnabled(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ENABLE_BT_REQUEST){
            if(resultCode == RESULT_OK){
                continueBluetooth();
            } if(resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this, "Application requires Bluetooth enabled", Toast.LENGTH_LONG).show();
            }
        } else if(requestCode == CHOOSE_PARAMS_REQUEST){
            if(resultCode == RESULT_OK && data != null){
                chosenParametersAmount = data.getIntExtra("parametersAmount", 3);
                ArrayList<String> newParametersNames = (ArrayList<String>) data.getSerializableExtra("parameters");
                chosenParameters.clear();
                for(String paramName : newParametersNames){
                    switch(paramName){
                        case "Vehicle Identification Number (VIN)":
                            chosenParameters.add(new VinCommand());
                            break;
                        case "Vehicle Speed":
                            chosenParameters.add(new SpeedCommand());
                            break;
                        case "Engine RPM":
                            chosenParameters.add(new RPMCommand());
                            break;
                        case "Engine Load":
                            chosenParameters.add(new LoadCommand());
                            break;
                        case "Throttle Position":
                            chosenParameters.add(new ThrottlePositionCommand());
                            break;
                        case "Engine Coolant Temperature":
                            chosenParameters.add(new EngineCoolantTemperatureCommand());
                            break;
                        case "Engine oil temperature":
                            chosenParameters.add(new OilTempCommand());
                            break;
                        case "Intake Manifold Pressure":
                            chosenParameters.add(new IntakeManifoldPressureCommand());
                            break;
                        case "Fuel Level":
                            chosenParameters.add(new FuelLevelCommand());
                            break;
                    }
                }
                try{
                    command1Label.setText(String.format("%s:", chosenParameters.get(0).getName()));
                    command1 = chosenParameters.get(0);
                } catch (IndexOutOfBoundsException ex){
                    command1Label.setText("");
                    command1 = null;
                }
                try{
                    command2Label.setText(String.format("%s:", chosenParameters.get(1).getName()));
                    command2 = chosenParameters.get(1);
                } catch (IndexOutOfBoundsException ex){
                    command2Label.setText("");
                    command2 = null;
                }
                try {
                    command3Label.setText(String.format("%s:", chosenParameters.get(2).getName()));
                    command3 = chosenParameters.get(2);
                } catch (IndexOutOfBoundsException ex){
                    command3Label.setText("");
                    command3 = null;
                }
            } else {
                Toast.makeText(MainActivity.this, "Preferred parameters not saved correctly", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void chooseBluetoothDevice(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null){
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
        if(!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST);
        } else{
            continueBluetooth();
        }
    }

    private void continueBluetooth(){
        final ArrayList<String> pairedDevicesNames = new ArrayList<>();
        final ArrayList<String> pairedDevicesAddresses = new ArrayList<>();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesNames.add(device.getName());
                pairedDevicesAddresses.add(device.getAddress());
            }

            final String[] devicesString = pairedDevicesNames.toArray(new String[0]);
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
            mBuilder.setTitle("Choose OBD device:");
            mBuilder.setSingleChoiceItems(devicesString, -1, (dialog, i) -> {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                chosenDeviceAddress = pairedDevicesAddresses.get(position);
                chosenDeviceName = pairedDevicesNames.get(position);
                Toast.makeText(MainActivity.this, "Chosen: " + chosenDeviceName, Toast.LENGTH_SHORT).show();

                TextView info = findViewById(R.id.info);
                info.setText(String.format("Name: %s\tAddress: %s", chosenDeviceName, chosenDeviceAddress));
                bConnect.setEnabled(true);
            });

            AlertDialog mDialog = mBuilder.create();
            mDialog.show();
        } else{
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectOBD() {
        try {
            BluetoothDevice device = btAdapter.getRemoteDevice(chosenDeviceAddress);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();

            new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());

            Toast.makeText(MainActivity.this, "Connected to OBD", Toast.LENGTH_SHORT).show();
            bStart.setEnabled(true);
            bStop.setEnabled(true);
            bConnect.setEnabled(false);

        } catch (IllegalArgumentException e) {
            Toast.makeText(MainActivity.this, "Please choose Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, "Unable to establish connection", Toast.LENGTH_LONG).show();
        } catch (Exception e){
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void startOBD() {
        /*try {
            command1.run(btSocket.getInputStream(), btSocket.getOutputStream());
            command1Result.setText(command1.getCalculatedResult());
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, "Please connect to Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
        try {
            command2.run(btSocket.getInputStream(), btSocket.getOutputStream());
            command2Result.setText(command2.getCalculatedResult());
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, "Please connect to Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
        try {
            command3.run(btSocket.getInputStream(), btSocket.getOutputStream());
            command3Result.setText(command3.getCalculatedResult());
        } catch (NullPointerException e) {
            Toast.makeText(MainActivity.this, "Please connect to Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }*/
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    command1.run(btSocket.getInputStream(), btSocket.getOutputStream());
                    command1Result.setText(command1.getCalculatedResult());
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, 100, 1000);
    }

    private void stopOBD() {
        command1Result.setText("");
        command2Result.setText("");
        command3Result.setText("");
        timer.cancel();
    }
}




//If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you are connecting to an Android peer then please generate your own unique UUID.
