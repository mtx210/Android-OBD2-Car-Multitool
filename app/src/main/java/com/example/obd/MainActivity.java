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
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    final static int ENABLE_BT_REQUEST = 1;
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket;
    private String chosenDeviceName;
    private String chosenDeviceAddress;
    private boolean readingObd;

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
                Toast.makeText(MainActivity.this, "Application requires Bluetooth enabled", Toast.LENGTH_LONG).show();
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
        try {
            BluetoothDevice device = btAdapter.getRemoteDevice(chosenDeviceAddress);
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //If you are connecting to a Bluetooth serial board then try using the well-known SPP UUID 00001101-0000-1000-8000-00805F9B34FB. However if you are connecting to an Android peer then please generate your own unique UUID.

            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();

            new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());

            Toast.makeText(MainActivity.this, "Connected to OBD", Toast.LENGTH_SHORT).show();

        } catch (IllegalArgumentException e) {
            Toast.makeText(MainActivity.this, "Please choose Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (IOException | InterruptedException e){
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
        try{
            final RPMCommand engineRpmCommand = new RPMCommand();
            final EngineCoolantTemperatureCommand waterTempCommand = new EngineCoolantTemperatureCommand();
            final AirIntakeTemperatureCommand airTempCommand = new AirIntakeTemperatureCommand();

            readingObd = true;
            while(readingObd){
                engineRpmCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                rpmResult.setText(engineRpmCommand.getCalculatedResult());
                airTempCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                airResult.setText(airTempCommand.getFormattedResult());
                waterTempCommand.run(btSocket.getInputStream(), btSocket.getOutputStream());
                waterResult.setText(waterTempCommand.getFormattedResult());                         //at 100ms refresh app crashes??? 500ms is fine tho

                Thread.sleep(1000);
            }
        } catch(NullPointerException e){
            Toast.makeText(MainActivity.this, "Please connect to Bluetooth device first", Toast.LENGTH_LONG).show();
        } catch (InterruptedException | IOException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopOBD() {
        readingObd = false;
        rpmResult.setText("");
        airResult.setText("");
        waterResult.setText("");
    }
}