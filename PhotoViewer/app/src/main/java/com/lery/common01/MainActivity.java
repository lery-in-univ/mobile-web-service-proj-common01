package com.lery.common01;

import android.database.Cursor;
import android.provider.OpenableColumns;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    JSONObject post_json;
    String imageUrl = null;
    String token = "53a6b4128358f3e8631661cc317f16765295699d";

    RecyclerView recyclerView;

    CloadImage taskDownload;
    CuploadImage taskUpload;

    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    taskUpload = new CuploadImage();
                    taskUpload.execute(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
    }

    public void onClickDownload(View v) {
        if(taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }

        taskDownload = new CloadImage();

        taskDownload.execute(site_url + "/api_root/Post/");
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }

    public void onClickUpload(View v) {
        if (taskUpload != null && taskUpload.getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, "업로드 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        imagePicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Bitmap>> {
        @Override
        protected List<Bitmap> doInBackground(String... urls) {
            List<Bitmap> bitmapList = new ArrayList<>();

            try {
                String apiUrl = urls[0];
                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONObject obj = new JSONObject(strJson);
                    JSONArray results = obj.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        post_json = (JSONObject) results.get(i);
                        imageUrl = post_json.getString("image");

                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            conn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = conn.getInputStream();

                            Bitmap imageBitmap = BitmapFactory.decodeStream(imgStream);
                            bitmapList.add(imageBitmap);
                            imgStream.close();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return bitmapList;
        }

        @Override
        protected void onPostExecute(List<Bitmap> bitmaps) {
            recyclerView.setAdapter(new ImageAdapter(bitmaps));
            textView.setText("이미지 로드 완료!");
        }
    }

    private String readResponse(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
            }
            return result.toString();
        }
    }

    private class CuploadImage extends AsyncTask<Uri, Void, Boolean> {
        private String errorMessage = "";

        @Override
        protected Boolean doInBackground(Uri... uris) {
            HttpURLConnection conn = null;
            try {
                Uri uri = uris[0];
                String boundary = "PhotoViewer-" + System.currentTimeMillis();
                String fileName = "upload_image";
                try (Cursor cursor = getContentResolver().query(
                        uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index >= 0) {
                            String name = cursor.getString(index);
                            if (name != null && !name.isEmpty()) {
                                fileName = name;
                            }
                        }
                    }
                }
                fileName = fileName
                        .replace("\r", "_")
                        .replace("\n", "_")
                        .replace("\"", "_")
                        .replace("\\", "_");
                String mimeType = getContentResolver().getType(uri);
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                // 서버에 보낼 글과 이미지 정보
                String data = "";
                data += "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"author\"\r\n"
                        + "\r\n"
                        + "1\r\n"; // 작성자 번호
                data += "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"title\"\r\n"
                        + "\r\n"
                        + fileName + "\r\n";
                data += "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"text\"\r\n"
                        + "\r\n"
                        + "안드로이드에서 업로드한 이미지입니다.\r\n";
                data += "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"image\"; "
                        + "filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: " + mimeType + "\r\n"
                        + "\r\n";

                byte[] start = data.getBytes(StandardCharsets.UTF_8);
                byte[] end = ("\r\n--" + boundary + "--\r\n")
                        .getBytes(StandardCharsets.UTF_8);

                // 보내기 전에 이미지 크기 세기
                long imageSize = 0;
                byte[] buffer = new byte[1024];
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) {
                        throw new IOException("선택한 이미지를 열 수 없습니다.");
                    }
                    int length;
                    while ((length = input.read(buffer)) != -1) {
                        imageSize += length;
                    }
                }

                conn = (HttpURLConnection) new URL(site_url + "/api_root/Post/")
                        .openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setFixedLengthStreamingMode(start.length + imageSize + end.length);
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type",
                        "multipart/form-data; boundary=" + boundary);

                try (InputStream input = getContentResolver().openInputStream(uri);
                     OutputStream output = conn.getOutputStream()) {
                    if (input == null) {
                        throw new IOException("선택한 이미지를 열 수 없습니다.");
                    }
                    output.write(start);

                    // 이미지를 조금씩 읽어서 보내기
                    int length;
                    while ((length = input.read(buffer)) != -1) {
                        output.write(buffer, 0, length);
                    }
                    output.write(end);
                }

                int status = conn.getResponseCode();
                boolean success = status >= 200 && status < 300;
                String response = readResponse(success
                        ? conn.getInputStream() : conn.getErrorStream());
                if (!success) {
                    errorMessage = "HTTP " + status + "\n" + response;
                }
                return success;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                return false;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            findViewById(R.id.btn_save).setEnabled(true);
            if (success) {
                Toast.makeText(MainActivity.this, "업로드 완료!", Toast.LENGTH_SHORT).show();
                onClickDownload(null);
            } else {
                textView.setText("업로드 실패: " + errorMessage);
            }
        }
    }
}
