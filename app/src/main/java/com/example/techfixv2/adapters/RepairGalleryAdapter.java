package com.example.techfixv2.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.example.techfixv2.BookRepairActivity;
import com.example.techfixv2.R;
import com.example.techfixv2.models.RepairedDevice;

import java.util.List;

// Custom PagerAdapter implementation for recent completed repairs gallery (ViewPager)
public class RepairGalleryAdapter extends PagerAdapter {

    private final Context context;
    private List<RepairedDevice> deviceList;

    public RepairGalleryAdapter(Context context, List<RepairedDevice> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
    }

    public void updateData(List<RepairedDevice> newList) {
        this.deviceList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return deviceList != null ? deviceList.size() : 0;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    // Inflate custom card layout item_repaired_device and bind RepairedDevice model data
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_repaired_device, container, false);
        RepairedDevice device = deviceList.get(position);

        ImageView ivRepairedPhoto = view.findViewById(R.id.ivRepairedPhoto);
        ImageView ivRepairedPlaceholder = view.findViewById(R.id.ivRepairedPlaceholder);
        TextView tvRepairedBranch = view.findViewById(R.id.tvRepairedBranch);
        TextView tvRepairedDeviceName = view.findViewById(R.id.tvRepairedDeviceName);
        TextView tvRepairedDescription = view.findViewById(R.id.tvRepairedDescription);
        TextView tvRepairedCost = view.findViewById(R.id.tvRepairedCost);
        View btnRequestSimilar = view.findViewById(R.id.btnRequestSimilar);

        // set text
        tvRepairedDeviceName.setText(device.getName());
        tvRepairedDescription.setText(device.getDescription());
        tvRepairedBranch.setText("📍 " + device.getLocation() + " Branch");
        tvRepairedCost.setText(device.getFormattedPrice());

        // set image or placeholder
        String imgUriStr = device.getImageUrl();
        if (imgUriStr != null && !imgUriStr.trim().isEmpty()) {
            try {
                ivRepairedPhoto.setImageURI(Uri.parse(imgUriStr));
                ivRepairedPlaceholder.setVisibility(View.GONE);
                ivRepairedPhoto.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                showCategoryPlaceholder(ivRepairedPlaceholder, device.getCategory());
            }
        } else {
            showCategoryPlaceholder(ivRepairedPlaceholder, device.getCategory());
        }

        // book similar
        btnRequestSimilar.setOnClickListener(v -> {
            Intent intent = new Intent(context, BookRepairActivity.class);
            intent.putExtra("preselected_service", device.getName());
            intent.putExtra("preselected_category", device.getCategory());
            intent.putExtra("preselected_cost", device.getPrice());
            intent.putExtra("preselected_branch", device.getLocation());
            context.startActivity(intent);
        });

        container.addView(view);
        return view;
    }

    private void showCategoryPlaceholder(ImageView placeholder, String category) {
        if (placeholder == null) return;
        placeholder.setVisibility(View.VISIBLE);
        if (category != null && (category.toLowerCase().contains("laptop") || category.toLowerCase().contains("macbook"))) {
            placeholder.setImageResource(R.drawable.ic_customer_laptop);
        } else {
            placeholder.setImageResource(R.drawable.ic_customer_phone);
        }
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
