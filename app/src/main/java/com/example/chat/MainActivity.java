package com.example.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.FlowableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.client.StompClient;

import static com.example.chat.RestClient.ANDROID_EMULATOR_LOCALHOST;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private SimpleAdapter mAdapter;
    private List<String> mDataSet = new ArrayList<>();
    private StompClient mStompClient;
    private Disposable mRestPingDisposable;
    private final SimpleDateFormat mTimeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private RecyclerView mRecyclerView;
    private Gson mGson = new GsonBuilder().create();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler_view);
        mAdapter = new SimpleAdapter(mDataSet);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
    }

    public void disconnectStomp(View view) {
        mStompClient.disconnect();
    }

    public void connectStomp(View view) {
        String url = "ws://" + ANDROID_EMULATOR_LOCALHOST
                + ":" + RestClient.SERVER_PORT + "/hello/websocket";
        Log.i("执行地址信息", url);
        mStompClient = Stomp.over(WebSocket.class, url);

        mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lifecycleEvent -> {
                    switch (lifecycleEvent.getType()) {
                        case OPENED:
                            toast("Stomp connection opened");
                            break;
                        case ERROR:
                            Log.e(TAG, "Stomp connection error", lifecycleEvent.getException());
                            toast("Stomp connection error");
                            break;
                        case CLOSED:
                            toast("Stomp connection closed");
                            break;
                        default:
                            break;
                    }
                });

        // Receive greetings
        mStompClient.topic("/topic/getResponse")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(topicMessage -> {
                    Log.i(TAG, "Received " + topicMessage.getPayload());
                    String msg = topicMessage.getPayload();
                    Log.w("响应的数据信息", msg);
                    addItem(mGson.fromJson(msg, EchoModel.class));
                });

        mStompClient.connect();
    }

    public void sendEchoViaStomp(View v) {
        JSONObject param = new JSONObject();
        try {
            param.put("name", "24");
        } catch (JSONException e) {

        }
        Log.i("传入的数据信息", param.toString());
        mStompClient.send("/app/welcome", param.toString())
                .compose(applySchedulers())
                .subscribe(aVoid -> {
                    Log.i(TAG, "STOMP echo send successfully");
                }, throwable -> {
                    Log.i(TAG, "Error send STOMP echo", throwable);
                    toast(throwable.getMessage());
                });
    }

    public void sendEchoViaRest(View v) {
        mRestPingDisposable = RestClient.getInstance().getExampleRepository()
                .sendRestEcho("Echo REST " + mTimeFormat.format(new Date()))
                .compose(applySchedulers())
                .subscribe(aVoid -> {
                    Log.d(TAG, "REST echo send successfully");
                }, throwable -> {
                    Log.e(TAG, "Error send REST echo", throwable);
                    toast(throwable.getMessage());
                });
    }

    private void addItem(EchoModel echoModel) {
        mDataSet.add(echoModel.getResponseMessage() + " - " + mTimeFormat.format(new Date()));
        mAdapter.notifyDataSetChanged();
        mRecyclerView.smoothScrollToPosition(mDataSet.size() - 1);
    }

    private void toast(String text) {
        Log.i(TAG, text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    protected <T> FlowableTransformer<T, T> applySchedulers() {
        return tFlowable -> tFlowable
                .unsubscribeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected void onDestroy() {
        mStompClient.disconnect();
        if (mRestPingDisposable != null) {
            mRestPingDisposable.dispose();
        }
        super.onDestroy();
    }
}
