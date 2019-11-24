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
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

    private String chosenDeviceName;
    private String chosenDeviceAddress;

    final static int ENABLE_BT_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bChooseDevice = findViewById(R.id.bChooseDevice);
        bChooseDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseBluetoothDevice();
            }
        });

        Button bStart = findViewById(R.id.bStart);
        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOBD();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == ENABLE_BT_REQUEST){
            if(resultCode == RESULT_OK){
                continueBluetooth();
            } else{
                Toast.makeText(MainActivity.this, "Allow app to enable BT!", Toast.LENGTH_SHORT);
            }
        }
    }

    private void chooseBluetoothDevice(){

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null){
            Toast.makeText(this, "NO BT", Toast.LENGTH_LONG).show();
        }
        if(!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BT_REQUEST);
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
            Toast.makeText(this, "No paired devices found!", Toast.LENGTH_SHORT).show();
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
            }
        });

        AlertDialog mDialog = mBuilder.create();
        mDialog.show();

        TextView info = findViewById(R.id.info);
        info.setText(chosenDeviceName + chosenDeviceAddress);
    }

    private void startOBD() {

        BluetoothDevice device = btAdapter.getRemoteDevice(chosenDeviceAddress);
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        BluetoothSocket socket = null;
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            socket.connect();

            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());

            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());

            new TimeoutCommand(1).run(socket.getInputStream(), socket.getOutputStream());

            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ///
        ///
        ///

        RPMCommand engineRpmCommand = new RPMCommand();

        try {
            engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        TextView rpmResult = findViewById(R.id.RpmResult);
        rpmResult.setText("RPM: " + engineRpmCommand.getFormattedResult());
    }
}