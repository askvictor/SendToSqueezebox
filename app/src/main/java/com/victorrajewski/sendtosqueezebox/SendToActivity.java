package com.victorrajewski.sendtosqueezebox;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SendToActivity extends AppCompatActivity {

    List<String[]> players;
    String url;
    SharedPreferences sharedPref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        url = extras.getString(Intent.EXTRA_TEXT);

        GetPlayersTask playersTask = new GetPlayersTask();
        playersTask.execute();
        try {
            players = playersTask.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(players.size() == 0) {
            Toast toast = Toast.makeText(getApplicationContext(), "No Players Found!", Toast.LENGTH_LONG);
            toast.show();
            finish();
            return;
        } else if(players.size() == 1) {
            sendToSqueezebox(0);
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(SendToActivity.this);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            for(String[] player: players){
                adapter.add(player[0]);
            }
            builder.setTitle("Which Squeezebox?");
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    sendToSqueezebox(item);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        //finish();
    }

    public void sendToSqueezebox(int chosen_player_index) {
        String[] player = players.get(chosen_player_index);
        SendToSqueezeboxTask task = new SendToSqueezeboxTask();
        Uri uri = Uri.parse(url);
        String ytID = extractYTId(url);
        task.execute(player[1], "youtube://" + ytID);

        Toast toast = Toast.makeText(getApplicationContext(), "Sent  to " + player[0], Toast.LENGTH_LONG);
        toast.show();

    }

    public static String extractYTId(String ytUrl) {
        String vId = null;
        Pattern pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()){
            vId = matcher.group(1);
        }
        return vId;
    }
    private class GetPlayersTask extends AsyncTask<Void, Void, List<String[]>> {
        protected List<String[]> doInBackground(Void... args) {
            //Map<String, String> players = new HashMap<String,String>();
            List<String[]> players = new ArrayList<String[]>();
            try {
                final String address = sharedPref.getString("server_address", "");
                final String port = sharedPref.getString("server_port", "");
                Socket socket = new Socket(address, Integer.parseInt(port));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output.println("player count ?");

                String response = input.readLine();
                int playerCount = 0;
                if(response.substring(0,13).equals("player count ")) {
                    String doo = response.substring(13);
                    playerCount = Integer.parseInt(doo);
                } else {
                    //throw "Error contacting server";
                }
                for(int i=0; i<playerCount; i++) {
                    String playerMAC = "";
                    String playerName = "";
                    output.println("player id " + i + " ?");
                    response = input.readLine();
                    if(response.substring(0,11).equals("player id " + i)) { //TODO - handle 2-digits?
                        playerMAC = URLDecoder.decode(response.substring(12), "UTF-8");
                    } // TODO - check for empty response

                    output.println(" player name " + i + " ?");
                    response = input.readLine();
                    if(response.substring(0,13).equals("player name " + i)) { //TODO - handle 2-digits?
                        playerName = URLDecoder.decode(response.substring(14), "UTF-8");
                    } // TODO - check for empty response

                    players.add(new String[]{playerName, playerMAC});
                }

                output.close();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return players;
        }
    }
    private class SendToSqueezeboxTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... args) {
            try {
                final String playerMAC = args[0];
                final String mediaURL = args[1];
                final String address = sharedPref.getString("server_address", "");
                final String port = sharedPref.getString("server_port", "");
                Socket socket = new Socket(address, Integer.parseInt(port));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                output.println(playerMAC + " playlist play " + mediaURL);
                //output.flush();
                output.close();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            finish();
            return null;
        }
    }
}
