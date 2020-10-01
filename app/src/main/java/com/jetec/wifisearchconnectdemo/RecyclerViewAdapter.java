package com.jetec.wifisearchconnectdemo;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    List<ScanResult> list = new ArrayList<>();
    OnItemClick onItemClick;

    public void addItem(List<ScanResult> list){
        this.list = list;
        notifyDataSetChanged();
    }

    public OnItemClick getOnItemClick() {
        return onItemClick;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView tvSSID,tvBSSID,tvCap,tvFren,tvLevel;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvSSID = itemView.findViewById(R.id.textView_SSID);
            tvBSSID = itemView.findViewById(R.id.textView_BSSID);
            tvCap = itemView.findViewById(R.id.textView_Cap);
            tvFren = itemView.findViewById(R.id.textView_Frequency);
            tvLevel = itemView.findViewById(R.id.textView_Level);

        }
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.result_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult scanResult = list.get(position);
        holder.tvSSID.setText(scanResult.SSID);
        holder.tvBSSID.setText("位址： "+scanResult.BSSID);
        holder.tvCap.setText("加密方式： "+scanResult.capabilities);
        holder.tvFren.setText("訊號頻率： "+scanResult.frequency);
        holder.tvLevel.setText("訊號強度： "+scanResult.level);
        holder.itemView.setOnClickListener(v -> {onItemClick.onItemClick(scanResult);});
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    interface OnItemClick{
        void onItemClick(ScanResult scanResult);
    }


}
