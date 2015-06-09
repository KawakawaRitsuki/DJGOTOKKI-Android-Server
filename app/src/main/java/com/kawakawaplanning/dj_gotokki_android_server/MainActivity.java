package com.kawakawaplanning.dj_gotokki_android_server;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements YouTubePlayer.OnInitializedListener  {

    //クラス変数定義
    static ArrayList<String> stockId = new ArrayList<String>();
    static ArrayList<String> stockTi = new ArrayList<String>();
//    static int PORT = 10000;
    static boolean playing = false;
    static Thread thread;
    static YouTubePlayer YP;
    static TextView textView;
    static Handler handler;
    static ListView listView;
    static Context con;
    static SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getSharedPreferences("user_data", MODE_PRIVATE);

        con = this;

        // フラグメントインスタンスを取得
        YouTubePlayerFragment youTubePlayerFragment =
                (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_fragment);
        textView = (TextView)findViewById(R.id.tv);
        listView = (ListView)findViewById(R.id.listView);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,final int position, long id) {
                if (position != 0){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(con);
                    alertDialogBuilder.setTitle("確認");
                    alertDialogBuilder.setMessage("この項目を削除しますか？");
                    alertDialogBuilder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    stockTi.remove(position);
                                    stockId.remove(position);
                                    MainActivity.handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(con,
                                                    android.R.layout.simple_list_item_1, stockTi);
                                            listView.setAdapter(adapter);

                                        }
                                    });
                                }
                            });
                    alertDialogBuilder.setNegativeButton("Cancel",null);
                    // アラートダイアログのキャンセルが可能かどうかを設定します
                    alertDialogBuilder.setCancelable(true);

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // アラートダイアログを表示します
                    alertDialog.show();

                }else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(con);
                    alertDialogBuilder.setTitle("確認");
                    alertDialogBuilder.setMessage("この項目は現在再生中です。削除する変わりにスキップボタンをタップしてください。");
                    alertDialogBuilder.setPositiveButton("OK", null);
                    // アラートダイアログのキャンセルが可能かどうかを設定します
                    alertDialogBuilder.setCancelable(true);

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // アラートダイアログを表示します
                    alertDialog.show();
                }
                return false;
            }
        });
        handler = new Handler(); // (1)
        MainActivity.handler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.textView.setText("未再生／曲送信受付中。");
            }
        });

        // フラグメントのプレーヤーを初期化する
        youTubePlayerFragment.initialize("AIzaSyBIOfjpJSTTe1zdviuoiF_ngwlYRUbNCao", this);
//
//        SubThread sub = new SubThread();
//        thread = new Thread(sub);
//        thread.start();
    }


    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider,
                                        YouTubeInitializationResult error) {
        // プレーヤーの初期化失敗時に呼ばれる
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        // プレーヤーの初期化成功時に呼ばれる
        YP = player;
        YP.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
        YP.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
            public void onVideoStarted() {
            }

            public void onVideoEnded() {
                next();
            }

            public void onLoading() {
            }

            public void onLoaded(String arg0) {
            }

            public void onError(YouTubePlayer.ErrorReason arg0) {
            }

            public void onAdStarted() {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{SubThread.socket.close();}catch(Exception e){System.out.println(e);}
        SubThread.socket = null;
        try{SubThread.serverSocket.close();} catch (Exception e) {System.out.println(e);}
        SubThread.serverSocket = null;

        try{thread.stop();}catch (Exception e){System.out.println(e);}

        SubThread sub = new SubThread();

        /* 別のスレッドを作成し、スレッドを開始する */
        thread = new Thread(sub);
        thread.start();
    }

    public void skip(View v){
        next();
    }

    static void next() {
        if (playing == true){

            playing = false;
            if(stockId.size() > 1){
                stockTi.remove(0);
                stockId.remove(0);
                YP.loadVideo(stockId.get(0));
                playing = true;

                MainActivity.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("再生中／曲送信受付中。");
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(con,
                                android.R.layout.simple_list_item_1, stockTi);
                        listView.setAdapter(adapter);

                    }
                });

            }else{

                stockTi.remove(0);
                stockId.remove(0);
                playing = false;
                YP.pause();
                MainActivity.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.textView.setText("未再生／曲送信受付中。");
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(con,
                                android.R.layout.simple_list_item_1, stockTi);
                        listView.setAdapter(adapter);
                    }
                });
            }
        }
    }

    static void first() {

        MainActivity.handler.post(new Runnable() {
            @Override
            public void run() {
                textView.setText("再生中／曲送信受付中。");
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(con,
                        android.R.layout.simple_list_item_1, stockTi);
                listView.setAdapter(adapter);
            }
        });
        YP.loadVideo(stockId.get(0));
        playing = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClassName("com.kawakawaplanning.dj_gotokki_android_server", "com.kawakawaplanning.dj_gotokki_android_server.Preference");
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
class SubThread implements Runnable{
    static boolean flag = true;
    static ServerSocket serverSocket;
    static Socket socket;
    public void run(){
        check();
    }

    public static void check(){

        serverSocket = null;
        try {
            serverSocket = new ServerSocket(MainActivity.pref.getInt("port", 10000));

            System.out.println("start wait...");



            MainActivity.handler.post(new Runnable() {
                @Override
                public void run() {
                    if (MainActivity.playing) {
                        MainActivity.textView.setText("再生中／曲送信受付中。");
                    }else{
                        MainActivity.textView.setText("未再生／曲送信受付中。");
                    }
                }
            });


            // 接続があるまでブロック
            socket = serverSocket.accept();
            BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));
            final String str = br.readLine();
            System.out.println(str);

            final String stri[] = str.split(",");

            if (stri[0].equals("getAdmin")){

//                MainActivity.display.asyncExec(new Runnable() {
//                    public void run() {
//                        if(!MainActivity.adminEn){
//                            MainActivity.box.setMessage("'"+stri[1]+"'がAdminister権限を要求してきました。\n要求を許可すると、その端末でサーバーの操作ができるようになります。\n許可しますか？");
//                            MainActivity.box.setText("管理者権限の要求");
//                            int result = MainActivity.box.open();
//                            if(result==SWT.YES){
//                                MainActivity.adminEn = true;
//                                MainActivity.adminIp = stri[1];
//                            }
//                        }
//                    }
//                });

            }else if(stri[0].equals("skip")){
//                if(MainActivity.adminEn && stri[1].equals(MainActivity.adminIp)){
//                    MainActivity.next();
//                }
            }else if(stri[0].equals("reload")){
//                if(MainActivity.adminEn && stri[1].equals(MainActivity.adminIp)){
//                    sendList(MainActivity.adminIp, "12000");
//                }

            }else{

                Thread th = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        if(flag){
                            flag = false;
                        }

                        try {


                            String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&key=AIzaSyBEdFSE1PClEDQ2AnvQJ-SGe5QM9VIXJBQ";
                            url += "&id=" + stri[0];
                            HttpUriRequest httpGet = new HttpGet(url);
                            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
                            HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
                            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                                try {
                                    JSONObject json = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
                                    JSONArray items = json.getJSONArray("items");

                                    final JSONObject ob2 = items.getJSONObject(0);
                                    try {
                                        MainActivity.stockTi.add(ob2.getJSONObject("snippet").get("title").toString());
                                        MainActivity.stockId.add(stri[0]);
                                        MainActivity.handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.con,
                                                        android.R.layout.simple_list_item_1, MainActivity.stockTi);
                                                MainActivity.listView.setAdapter(adapter);
                                            }
                                        });
                                        
                                    } catch (JSONException e) {
                                        System.out.println(e.toString());
                                    }


                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(MainActivity.playing == false){
                                MainActivity.first();
                            }
                        } catch (NumberFormatException e) {
                            // TODO Auto-generated catch block
                            System.out.println(e);
                        } catch (ClientProtocolException e1) {
                            // TODO 自動生成された catch ブロック
                            e1.printStackTrace();
                        } catch (IOException e1) {
                            // TODO 自動生成された catch ブロック
                            e1.printStackTrace();
                        }

                    }
                });
                th.start();
            }
            socket.close();
            socket = null;
            serverSocket.close();
            serverSocket = null;
            check();
        } catch (IOException e) {
            System.out.println(e);
//            MainActivity.display.asyncExec(new Runnable() {
//                public void run() {
//                    MainActivity.statusLabel.setText("ポートエラー／ポート設定をしてください。");
//                }
//            });
           MainActivity.handler.post(new Runnable() {
               @Override
               public void run() {
                   MainActivity.textView.setText("ポートエラー／ポート設定をしてください。");
               }
           });

        }


    }
//    public static void sendList(String HOST,String PORT){
//        System.out.println("ktkr");
//        Socket socket = null;
//        int port = Integer.parseInt(PORT);
//        try {
//            socket = new Socket(HOST,port);
//            PrintWriter pw = new PrintWriter(socket.getOutputStream(),true);
//            for(int i = 0;i <= MainActivity.stockTi.size()-1;i++){
//                pw.println( MainActivity.stockTi.get(i)+"");
//            }
//
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        if( socket != null){
//            try {
//                socket.close();
//                socket = null;
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
    public static void sendPoricy(String HOST,String PORT){
        System.out.println("ktkr");
        Socket socket = null;
        int port = Integer.parseInt(PORT);
        try {
            socket = new Socket(HOST,port);
            PrintWriter pw = new PrintWriter(socket.getOutputStream(),true);
            pw.println("<?xml version=\"1.0\"?><cross-doMainActivity-policy><allow-access-from doMainActivity=\"*\" to-ports=\"10000\" /></cross-doMainActivity-policy>\0");

//            socket.puts('<?xml version="1.0"?>')
//            socket.puts('<!DOCTYPE cross-doMainActivity-policy SYSTEM ' +
//              '"http://www.macromedia.com/xml/dtds/cross-doMainActivity-policy.dtd">')
//            socket.puts('')
//            for doMainActivity in @accepted_doMainActivitys
//              next if doMainActivity == "file://"
//              socket.puts("")
//            end
//            socket.puts('</cross-doMainActivity-policy>')

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if( socket != null){
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}