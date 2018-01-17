package com.example.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;
import ua.naiksoftware.stomp.client.StompMessage;

import static com.example.chat.RestClient.ANDROID_EMULATOR_LOCALHOST;


public class CheatActivity extends AppCompatActivity {

    private EditText etInput;
    private Button btnSend;
    private TextView tvService;

    private StringBuffer sb=new StringBuffer();

    private StompClient mStompClient;

    private static final String TAG="CheatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cheat);

        etInput=(EditText) findViewById(R.id.et_inputMsg);
        btnSend=(Button)findViewById(R.id.btn_click_send);
        tvService=(TextView)findViewById(R.id.tv_service_msg);
        createStompClient();
        registerStompTopic();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg=etInput.getText().toString().trim();
                JSONObject jsonObject=new JSONObject();

                try {
                    jsonObject.put("userId","kobe");
                    jsonObject.put("message",msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mStompClient.send("/app/cheat",jsonObject.toString()).subscribe(new Subscriber<Void>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(Void aVoid) {

                    }

                    @Override
                    public void onError(Throwable t) {
                        toast("发送错误");
                    }

                    @Override
                    public void onComplete() {
                        toast("发送成功");
                    }
                });

            }
        });
    }



    private void createStompClient() {
        String url = "ws://" + ANDROID_EMULATOR_LOCALHOST
                + ":" + RestClient.SERVER_PORT + "/hello/websocket";
        mStompClient = Stomp.over(WebSocket.class, url);
        mStompClient.connect();
        mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            Log.i(TAG,"Stomp connection opened");
                            toast("Stomp connection opened");
                            break;
                        case ERROR:
                            Log.i(TAG, "Stomp connection error", lifecycleEvent.getException());
                            toast("Stomp connection error");
                            break;
                        case CLOSED:
                            toast("Stomp connection closed");
                            break;
                        default:
                            break;
                    }
                });
    }


    private void registerStompTopic() {
        mStompClient.topic("/user/kobe/message")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.i(TAG, "Received " + topicMessage.getPayload());
                    showMessage(topicMessage);
                });
    }

    private void showMessage(final StompMessage stompMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String msg=stompMessage.getPayload();
                sb.append(msg+"-->");
                tvService.setText(sb.toString());

            }
        });
    }


    private void toast(String text) {
        Log.i(TAG, text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}
