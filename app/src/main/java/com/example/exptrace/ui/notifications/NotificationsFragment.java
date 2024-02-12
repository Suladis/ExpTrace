package com.example.exptrace.ui.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.exptrace.CaptureAct;
import com.example.exptrace.R;
import com.example.exptrace.databinding.FragmentNotificationsBinding;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private Button b2;
    private Button b3;


    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    Tag myTag;
    Context context;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        b2 = root.findViewById(R.id.b2);
        b3 = root.findViewById(R.id.b3);

        b2.setOnClickListener(v -> {
            scanCode();
        });

        b3.setOnClickListener(v -> {
            scanNFC();
        });


        return root;
    }

    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Volume up to flash on");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    private void scanNFC(){
//        Placeholder text
    }

    private ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            // Extract UPC or EAN number from the barcode result
            String barcode = result.getContents();

            // Make HTTP request to UPC Database API
            OkHttpClient client = new OkHttpClient();
            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://api.upcdatabase.org/product/" + barcode).newBuilder();
            urlBuilder.addQueryParameter("apikey", "806B6CBDB7002EED9D65ACBC355D74EA");
            String url = urlBuilder.build().toString();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    try {
                        // Parse the JSON response
                        JSONObject jsonResponse = new JSONObject(response.body().string());

                        // Extract product information
                        String productName = jsonResponse.optString("title");
                        // You can extract more fields as needed

                        // Display product information in AlertDialog
                        requireActivity().runOnUiThread(() -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                            builder.setTitle("Product Information");
                            builder.setMessage("Product Name: " + productName);
                            builder.setPositiveButton("Ok", null);
                            builder.show();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }
    });

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
