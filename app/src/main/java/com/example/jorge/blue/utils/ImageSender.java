package com.example.jorge.blue.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.util.Log;


public class ImageSender extends AsyncTask<Void, Integer, Integer> {

    private String boundary = "";
    private HttpURLConnection httpConn;
    private static final String LINE_FEED = "\r\n";
    private String charset = "UTF-8";
    private final int maxBufferSize = 4096;
    private OutputStream outputStream;
    private PrintWriter writer;

    private static final String url = Identifiers.URL_SERVER+"api/imgcapture";

    private File file;

    private Context context;


    String responseStr;


    public ImageSender(Context context, File file) {
        this.context = context;
        this.file = file;
    }

    public void initialization() throws IOException {
        boundary = "----WebKitFormBoundaryG6EHbypQYOn4kq4i";
        URL url = new URL(ImageSender.url);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Connection", "Keep-Alive");
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("User-Agent", "Android Multipart HTTP Client 1.0");

        outputStream = new BufferedOutputStream(httpConn.getOutputStream());
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    @Override
    protected void onPreExecute(){
        super.onPreExecute();
    }

    @Override
    protected void onCancelled() {
        try {
            if (httpConn != null)
                httpConn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: "
                        + URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();
        FileInputStream inputStream = new FileInputStream(uploadFile);
        int bufferSize = Math.min(inputStream.available(), maxBufferSize);
        byte[] buffer = new byte[bufferSize];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer, 0, bufferSize)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        writer.append(LINE_FEED);
        writer.flush();
        inputStream.close();

    }

    public Integer send() throws IOException {
        Integer resultRspns = 0;
        StringBuffer response = new StringBuffer();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.flush();
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            writer.flush();
            reader.close();
            httpConn.disconnect();
            resultRspns = HttpURLConnection.HTTP_OK;
            responseStr = response.toString();
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader( httpConn.getErrorStream() ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            System.out.println(response);
            Log.e("ERROR ON UPLOAD IMAGE", response.toString());
            reader.close();
            httpConn.disconnect();
            resultRspns = status;
            responseStr = response.toString();
        }

        return resultRspns;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        try {
            if (file == null){
                Log.e("ERROR ON UPLOAD IMAGE", "FILE NULL");
                return 0;
            }

            Log.d("IMAGE SENDER", "SENDING... PHOTO");

            ContextWrapper cw = new ContextWrapper(context);
            initialization();
            addFormField("CaptureDate", "1547996460");
            addFormField("StationId", "14");
            addFormField("ApiKey", "23ff15852c63b066");
            addFilePart("ImageFile", file);

            Log.d("IMAGE SENDER", "SENDING... PHOTO: "+writer.toString());

            return send();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    @Override
    protected void onPostExecute(Integer result) {
        try {
            if (result == HttpURLConnection.HTTP_OK) {
                success();
                Log.d("IMAGE SENDER", "SUCCESS SENDING PHOTO: "+result);
            } else {
                Log.d("IMAGE SENDER", "FAILED SENDING PHOTO: "+result);
                failure();
            }
        } catch (Exception e) {
            e.printStackTrace();
            failure();
        }
    }

    private void success() {
        //TODO: Add the code to delete de image in the phone.
        Log.d("IMAGE SENDER", "SUCCESS SENDING PHOTO");
    }

    private void failure() {
        //TODO: Add the code to delete de image in the phone.
        Log.d("IMAGE SENDER", "FAILED SENDING PHOTO");
    }

}