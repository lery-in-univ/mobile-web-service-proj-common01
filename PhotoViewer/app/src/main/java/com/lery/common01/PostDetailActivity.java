package com.lery.common01;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class PostDetailActivity extends AppCompatActivity {
    private TextView title, content, message;
    private ImageView image;
    private Button likeButton;
    private String siteUrl, token, detailUrl;
    private boolean liked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        title = findViewById(R.id.postTitle);
        content = findViewById(R.id.postContent);
        message = findViewById(R.id.detailMessage);
        image = findViewById(R.id.postImage);
        likeButton = findViewById(R.id.likeButton);

        siteUrl = getIntent().getStringExtra("site_url");
        token = getIntent().getStringExtra("token");
        detailUrl = siteUrl + "/api_root/Post/" + getIntent().getIntExtra("post_id", 0) + "/";

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        likeButton.setOnClickListener(v -> new DetailTask(true).execute());

        new DetailTask(false).execute();
    }

    private JSONObject request(String url, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "Token " + token);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (method.equals("POST")) {
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(0);
                conn.getOutputStream().close();
            }

            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) throw new Exception("서버 응답: HTTP " + status);
            try (InputStream input = conn.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                return new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            }
        } finally {
            conn.disconnect();
        }
    }

    private class DetailTask extends AsyncTask<Void, Void, JSONObject> {
        private final boolean changeLike;
        private Bitmap bitmap;
        private String error;
        private String imageError;

        DetailTask(boolean changeLike) { this.changeLike = changeLike; }

        @Override
        protected JSONObject doInBackground(Void... ignored) {
            try {
                if (changeLike) {
                    return request(detailUrl + (liked ? "like/cancel/" : "like/"), "POST");
                }

                JSONObject post = request(detailUrl, "GET");
                String imageUrl = post.optString("image", "");

                if (!imageUrl.isEmpty() && !imageUrl.equals("null")) {
                    HttpURLConnection conn = null;
                    try {
                        conn = (HttpURLConnection) new URL(new URL(siteUrl), imageUrl).openConnection();
                        conn.setConnectTimeout(10000);
                        conn.setReadTimeout(10000);
                        try (InputStream input = conn.getInputStream()) {
                            bitmap = BitmapFactory.decodeStream(input);
                            if (bitmap == null) imageError = "사진을 표시할 수 없습니다.";
                        }
                    } catch (Exception e) {
                        imageError = "사진을 불러오지 못했습니다.";
                    } finally {
                        if (conn != null) conn.disconnect();
                    }
                }
                return post;
            } catch (Exception e) {
                error = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (isFinishing() || isDestroyed()) return;

            liked = result.optBoolean("liked", false);
            if (!changeLike) {
                title.setText(result.optString("title"));
                content.setText(result.optString("text"));
                image.setImageBitmap(bitmap);
            }

            likeButton.setText(liked ? "좋아요 취소" : "좋아요");

            if (changeLike) {
                if (liked) {
                    message.setText("좋아요 등록!");
                } else {
                    message.setText("좋아요 취소..!");
                }
            } else {
                message.setText(imageError == null ? "" : imageError);
            }
        }
    }
}
