package com.example.obd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.pires.obd.commands.SpeedCommand;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        /////
        /////
        /////

       // nibba();

    }

    public void nibba(){
        TextView resultScreen = findViewById(R.id.textView2);

        ArrayList<String> deviceStrs = new ArrayList();
        final ArrayList<String> devices = new ArrayList();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }

        // show list
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice, deviceStrs.toArray(new String[deviceStrs.size()]));

        final String[] deviceAddress2 = {null};
        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress2[0] = devices.get(position);
                // TODO save deviceAddress
            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();

        BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress2[0]);


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
        SpeedCommand speedCommand = new SpeedCommand();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                engineRpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                speedCommand.run(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // TODO handle commands result
            resultScreen.setText("RPM: " + engineRpmCommand.getFormattedResult());
            //("Speed: " + speedCommand.getFormattedResult());
        }
    }
}
