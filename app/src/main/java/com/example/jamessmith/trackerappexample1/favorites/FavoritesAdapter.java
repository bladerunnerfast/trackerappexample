package com.example.jamessmith.trackerappexample1.favorites;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.jamessmith.trackerappexample1.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by James Smith on 13/02/2018.
 */

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.CustomViewHolder> {

    private Context context;
    private List<FavoritesModel> favoritesModel;

    public FavoritesAdapter(Context context, List<FavoritesModel> favoritesModel) {
        this.context = context;
        this.favoritesModel = favoritesModel;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.favorites_list_layout, parent, false);
        return new CustomViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position) {

        holder._origin.setText("Origin:" + favoritesModel.get(position).getOrigin());
        holder._destination.setText("Destination: " + favoritesModel.get(position).getDestination());
        holder._distance.setText("Distance: " + favoritesModel.get(position).getDistance());
        holder._duration.setText("Duration: " + favoritesModel.get(position).getDuration());
    }

    @Override
    public int getItemCount() {
        return favoritesModel == null ? 0 : favoritesModel.size();
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.tv_origin) TextView _origin;
        @BindView(R.id.tv_destination) TextView _destination;
        @BindView(R.id.tv_distance) TextView _distance;
        @BindView(R.id.tv_duration) TextView _duration;
        @BindView(R.id.btn_go) Button _goBtn;

        private Intent intent;

        public CustomViewHolder(View view, final Context context) {
            super(view);
            ButterKnife.bind(this, view);

            intent = new Intent("updateMapFragment");

            _goBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    intent.putExtra("instruction", "updateFromList");
                    intent.putExtra("origin", _origin.getText());
                    intent.putExtra("destination", _destination.getText());
                    context.sendBroadcast(intent);
                }
            });
        }
    }
}