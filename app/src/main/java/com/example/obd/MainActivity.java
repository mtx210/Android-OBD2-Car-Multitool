package com.example.obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.control.VinCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
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

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket;

    private String chosenDeviceName;
    private String chosenDeviceAddress;

    final static int ENABLE_BT_REQUEST = 1;

    Timer timer;

    TextView rpmResult, vinResult, airResult, waterResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rpmResult = findViewById(R.id.RpmResult);
        vinResult = findViewById(R.id.labelVin);
        airResult = findViewById(R.id.airTempResult);
        waterResult = findViewById(R.id.waterTempResult);

        Button bChooseDevice = findViewById(R.id.bChooseDevice);
        bChooseDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseBluetoothDevice();
            }
        });

        Button bConnect = findViewById(R.id.bConnect);
        bConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectOBD();
            }
        });

        Button bStart = findViewById(R.id.bStart);
        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOBD();
            }
        });

        Button bStop = findViewById(R.id.bStop);
        bStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopOBD();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ENABLE_BT_REQUEST){
            if(resultCode == RESULT_OK){
                continueBluetooth();
            } if(resultCode == RESULT_CANCELED){
                Toast.makeText(MainActivity.this, "App required Bluetooth enabled", Toast.LENGTH_SHORT).show();
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

    private void connectOBD() {
        BluetoothDevice device = btAdapter.getRemoteDevice(chosenDeviceAddress);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you are connecting to an Android peer then please generate your own unique UUID.

        try {
            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();

            new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            //new TimeoutCommand(1).run(socket.getInputStream(), socket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());

            Toast.makeText(MainActivity.this, "Connected to OBD", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        } catch (InterruptedException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
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
        } else{
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }

        final String[] devicesString = pairedDevicesNames.toArray(new String[pairedDevicesNames.size()]);

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle("Choose OBD device:");
        mBuilder.setSingleChoiceItems(devicesString, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                chosenDeviceAddress = pairedDevicesAddresses.get(position);
                chosenDeviceName = pairedDevicesNames.get(position);
                Toast.makeText(MainActivity.this, "Chosen: " + chosenDeviceName, Toast.LENGTH_SHORT).show();

                TextView info = findViewById(R.id.info);
                info.setText(chosenDeviceName + chosenDeviceAddress);
            }
        });

        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
    }

    private void startOBD() {
/*
        try{
            VinCommand vinCommand = new VinCommand();
            vinCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
            vinResult.setText(vinCommand.getFormattedResult());
        } catch (InterruptedException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
*/
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    RPMCommand engineRpmCommand = new RPMCommand();
                    engineRpmCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                    rpmResult.setText(engineRpmCommand.getCalculatedResult());
/*
                    EngineCoolantTemperatureCommand waterTempCommand = new EngineCoolantTemperatureCommand();
                    waterTempCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                    waterResult.setText(waterTempCommand.getFormattedResult());

                    AirIntakeTemperatureCommand airTempCommand = new AirIntakeTemperatureCommand();
                    airTempCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                    airResult.setText(airTempCommand.getFormattedResult());
*/
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                } catch (InterruptedException e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }, 0, 500);     //at 100ms refresh app crashes??? 500ms is fine tho
    }

    private void stopOBD() {
        timer.cancel();
        rpmResult.setText("");
        airResult.setText("");
        waterResult.setText("");
    }
}