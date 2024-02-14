package com.example.exptrace.ui.notifications;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private Button b2;
    private Button b3;
    private TextView tv2,tv4,tv6;

    private TableLayout tl1;

    private ImageView iv1;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        b2 = root.findViewById(R.id.b2);
        b3 = root.findViewById(R.id.b3);
        tl1 = root.findViewById(R.id.tl1);
        tv2 = root.findViewById(R.id.tv2);
        tv4 = root.findViewById(R.id.tv4);
        tv6 = root.findViewById(R.id.tv6);


        tl1.setVisibility(View.INVISIBLE);
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


    private ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {

            // Make Table Visible
            tl1.setVisibility(View.VISIBLE);

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

                        // ID of Product
                        int productID = Integer.parseInt(jsonResponse.optString("barcode"));

                        // Alias Name of Product
                        String productName = jsonResponse.optString("alias");

                        // Date Added
                        LocalDate localDate = LocalDate.now();
                        DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        String dateNow = localDate.format(formatDate);

                        // Image


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

    private void scanNFC(){
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());

        if (nfcAdapter == null) {
            // NFC is not supported on this device.
            Toast.makeText(requireContext(), "NFC is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add FLAG_IMMUTABLE flag to PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0,
                new Intent(requireContext(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .putExtra("fromNFC", true), PendingIntent.FLAG_IMMUTABLE);

        IntentFilter[] intentFiltersArray = new IntentFilter[]{
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        };

        String[][] techListsArray = new String[][]{};

        nfcAdapter.enableForegroundDispatch(getActivity(), pendingIntent, intentFiltersArray, techListsArray);
    }


    @Override
    public void onResume() {
        super.onResume();
        // Enable NFC foreground dispatch when the activity is in the foreground
        scanNFC();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Disable NFC foreground dispatch when the activity is paused
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext());
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(requireActivity());
        }
    }

    public void handleNfcIntent(Intent intent) {
        // Handle the NFC tag here
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            Toast.makeText(requireContext(), "There is a tag"+ tag, Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
