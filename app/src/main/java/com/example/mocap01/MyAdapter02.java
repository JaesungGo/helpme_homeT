package com.example.mocap01;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mocap01.NewsItem;
import com.example.mocap01.R;

import java.util.List;

public class MyAdapter02 extends RecyclerView.Adapter<MyAdapter02.ViewHolder> {

    private List<NewsItem> items;
    private Context context;
    private OnItemClickListener clickListener;

    public MyAdapter02(List<NewsItem> newsItemList, Context context) {
        this.items = newsItemList;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewsItem item = items.get(position);

        holder.titleTextView.setText(item.getTitle());
        holder.descTextView.setText(item.getDesc());
        holder.linkTextView.setText(item.getLink());
        holder.dateTextView.setText(item.getDate());

        Glide.with(context)
                .load(item.getImgUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView linkTextView;
        TextView titleTextView;
        TextView descTextView;
        TextView dateTextView;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_title);
            descTextView = itemView.findViewById(R.id.tv_desc);
            linkTextView = itemView.findViewById(R.id.tv_link);
            dateTextView = itemView.findViewById(R.id.tv_date);
            imageView = itemView.findViewById(R.id.iv);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }
}
