package com.thesis.adminthesis.module;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<String> items;
    private int selectedItemPosition = -1;

    public ItemAdapter(List<String> items) {
        this.items = items;
    }

    public String getSelectedItemString() {
        return selectedItemPosition != -1 ? items.get(selectedItemPosition) : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_activated_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(items.get(position));

        // Highlight the selected item
        holder.itemView.setActivated(position == selectedItemPosition);

        holder.itemView.setOnClickListener(view -> {
            selectedItemPosition = holder.getAdapterPosition();
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(String item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public void removeSelectedItem() {
        if (selectedItemPosition != -1) {
            items.remove(selectedItemPosition);
            notifyItemRemoved(selectedItemPosition);
            selectedItemPosition = -1;
        }
    }

    public void removeAllItem() {
        items.clear();
        notifyDataSetChanged();
        selectedItemPosition = -1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}
