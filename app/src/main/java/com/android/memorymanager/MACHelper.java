package com.android.memorymanager;

import android.os.Handler;

import com.yandex.metrica.YandexMetrica;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MACHelper {
    private static String getMACAddress(){
        try {
            // Run the command
            Process process = Runtime.getRuntime().exec("ip link | grep -A1 wlan0");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Grab the results
            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line + "\n");
            }
            process.destroy();
            return log.toString();
        } catch (IOException e) {
        }
        return "n/a";
    }

    public static void sendMACtoUDP(final String strAddress){
        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            String message = getMACAddress();
            @Override
            public void run() {
                String address=strAddress;
                DatagramSocket ds = null;
                try {
                    ds = new DatagramSocket();
                    // IP Address below is the IP address of that Device where server socket is opened.
                    int port = 8000;
                    int pos = address.lastIndexOf(":");
                    if (pos!=-1){
                        port=Integer.valueOf(address.substring(pos+1));
                        address=address.substring(0,pos);
                    }
                    InetAddress serverAddr = InetAddress.getByName(address);
                    DatagramPacket dp;
                    dp = new DatagramPacket(message.getBytes(), message.length(), serverAddr, port);
                    ds.send(dp);
                } catch (IOException e) {
                    YandexMetrica.reportEvent(e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (ds != null) {
                        ds.close();
                    }
                }
            }
        });
        thread.start();
    }
}

