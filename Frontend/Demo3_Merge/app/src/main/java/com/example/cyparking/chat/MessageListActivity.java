//https://blog.sendbird.com/android-chat-tutorial-building-a-messaging-ui
//https://github.com/square/okhttp/blob/master/CHANGELOG.md#version-350
//https://medium.com/@ssaurel/learn-to-use-websockets-on-android-with-okhttp-ba5f00aea988

package com.example.cyparking.chat;

import android.os.Looper;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.cyparking.DefaultUserSchema;
import com.example.cyparking.LoginActivity;
import com.example.cyparking.R;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import com.example.cyparking.chat.Socket.OnEventListener;
import com.example.cyparking.chat.Socket.OnEventResponseListener;

import com.example.cyparking.parkinglog.ParkingLog;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageListActivity extends AppCompatActivity {
    private static String URL = "http://cs309-yt-2.misc.iastate.edu:8080";
    private static String WEBSOCKET_URL = "ws://cs309-yt-2.misc.iastate.edu:8080/websocket/chat";

    //For Tests
    public String userToken = "";

    private Button mChatBoxSend;
    private EditText mChatBox;

    private RequestQueue mQueue; //Volley Request Queue

    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private ArrayList<Message> messageList = new ArrayList<>();

    public static ChatUser thisUser;

    private OkHttpClient client;
    private Listener socketOpenListener;
    private ResponseListener socketResponseListener;
    Socket webSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_list);

        if (!(userToken.length() > 0)){
            userToken = LoginActivity.getToken();
        }
        //Load current user username
        mQueue = Volley.newRequestQueue(this);
        StringRequest getUser = new StringRequest(Request.Method.POST, URL + "/get/default",
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        String[] userInfo = response.split(",");
                        DefaultUserSchema userData = new DefaultUserSchema(userInfo[0], userInfo[1], userInfo[2], userInfo[3], userInfo[4], userInfo[5]);
                        thisUser = new ChatUser(userData.getUsername());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Toast.makeText(getBaseContext(), "Unable to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("token", userToken);
                return params;
            }
        };
        mQueue.add(getUser);

        //Init
        client = new OkHttpClient();
        socketOpenListener = new Listener();
        socketResponseListener = new ResponseListener();
        start();

        //Message View
        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        mMessageRecycler.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mMessageRecycler.setLayoutManager(linearLayoutManager);
        mMessageAdapter = new MessageListAdapter(messageList);
        mMessageRecycler.setAdapter(mMessageAdapter);
        ((LinearLayoutManager)mMessageRecycler.getLayoutManager()).setStackFromEnd(true);


        //Send Message action
        mChatBox = findViewById(R.id.edittext_chatbox);
        mChatBoxSend = findViewById(R.id.button_chatbox_send);
        mChatBoxSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(mChatBox.getText().toString());
                mChatBox.setText("");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class Listener extends OnEventListener {
        @Override
        public void onMessage(String event) {
            Toast.makeText(getBaseContext(), "Chat Connected", Toast.LENGTH_SHORT).show();
        }
    }

    private class ResponseListener extends OnEventResponseListener {
        @Override
        public void onMessage(String event, String data) {
            try {
                JSONObject js = new JSONObject(data);
                Message message = new Message(new ChatUser((js.get("User").toString())), js.get("Message").toString(), js.get("Date").toString());
                messageList.add(message);
                mMessageAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static ChatUser getCurrentUser() {
        return thisUser;
    }

    private void start() {
        webSocket = Socket.Builder.with(WEBSOCKET_URL).build();
        webSocket.connect();
        webSocket.onEvent(Socket.EVENT_OPEN, socketOpenListener);
        webSocket.onEventResponse("Sent Message", socketResponseListener); //Trigger when "Sent Message" fires
        //webSocket.onEvent(Socket.EVENT_RECONNECT_ATTEMPT, .....);
        //webSocket.onEvent(Socket.EVENT_CLOSED, .....);
        //webSocket.send("Some event", "{"some data":"in JSON format"}");
        //webSocket.sendOnOpen("Some event", "{"some data":"in JSON format"}");
    }

    private void sendMessage(String rawMsg) {
        Date todaysDate = new Date();
        long millis = todaysDate.getTime();
        String formattedDate = DateUtils.formatDateTime(millis);
        Message message = new Message(thisUser, rawMsg, formattedDate);
        //Fire "Sent Message" Event
        webSocket.send("Sent Message",
                "{\"Message\":\"" + message.getMessage() +
                        "\",\"User\":\"" + message.getSender().getUsername() +
                        "\",\"Date\":\"" + message.getCreatedAt() + "\"}");
    }

}
